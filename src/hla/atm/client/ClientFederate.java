package hla.atm.client;

import static hla.atm.utils.Utils.*;
import static hla.atm.commons.event.EventType.*;

import hla.atm.client.data.Client;
import hla.atm.commons.AbstractFederate;
import hla.atm.commons.event.ExternalEvent;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import java.util.ArrayList;
import java.util.List;

class ClientFederate extends AbstractFederate<ClientAmbassador> {

    private List<Client> clientsWaitingToJoinQueue = new ArrayList<>();
    private List<Client> clientsInQueue = new ArrayList<>();

    ClientFederate() throws RTIinternalError {
        super(CLIENT_FEDERATE_NAME);
        this.setRTIambassador(RtiFactoryFactory.getRtiFactory().createRtiAmbassador());
        this.setFederateAmbassador(new ClientAmbassador());
    }

    void runFederate() throws RTIexception {
        prepareFederate();
        declarePublishAndSubscribePolicy();
        generateNewClient();
        while(getFederateAmbassador().isRunning()) {
            advanceTime();
            processEvents();
            if(generateRandomInt(3, 0) % 2 == 1) {
                generateNewClient();
            }
            getRTIambassador().tick();
        }
    }

    private void processEvents() {
        getFederateAmbassador().getExternalEvents().forEach(externalEvent ->  {
            switch (externalEvent.getEventType()) {

                case ENTRY_REQUEST:
                    Client clientToAdd = new Client();
                    clientsWaitingToJoinQueue.add(clientToAdd);
                    sendEntryRequestInteraction(clientToAdd.getId());
                    break;

                case QUEUE_SIZE_STATE_RESPONSE:
                    clientsWaitingToJoinQueue.stream()
                            .filter(client -> client.getId() == externalEvent.getClientId())
                            .findFirst()
                            .ifPresent(client -> {
                                if (externalEvent.isQueueIsFull()) {
                                    clientsWaitingToJoinQueue.remove(client);
                                } else {
                                    clientsInQueue.add(client);
                                    if (clientsInQueue.indexOf(client) == 0) {
                                        sendWithdrawalRequestInteraction(client.getId(), generateRandomInt(2000, 500));
                                    }
                                }
                                clientsWaitingToJoinQueue.remove(client);
                            });
                    break;

                case TRANSACTION_STATUS:
                    int clientIdTransactionStatus = externalEvent.getClientId();
                    sendLeaveRequestInteraction(clientIdTransactionStatus);
                    clientsInQueue.stream()
                            .filter(client -> client.getId() == clientIdTransactionStatus)
                            .findFirst()
                            .ifPresent(client -> clientsInQueue.remove(client));
                    if(clientsInQueue.size() > 0) {
                        int cash = generateRandomInt(3000, 500);
                        sendWithdrawalRequestInteraction(clientsInQueue.get(0).getId(), cash);
                    }
                    break;
            }
        });
        getFederateAmbassador().getExternalEvents().clear();
    }

    private void generateNewClient() {
        ExternalEvent addFirstClientEvent = new ExternalEvent(ENTRY_REQUEST, getFederateAmbassador().getFederateTime());
        getFederateAmbassador().getExternalEvents().add(addFirstClientEvent);
    }

    private void declarePublishAndSubscribePolicy() throws RTIexception {
        publishInteraction(ENTRY_REQUEST.name());
        publishInteraction(LEAVE_REQUEST.name());
        publishInteraction(WITHDRAWAL_REQUEST.name());
        getFederateAmbassador().setNoCashInteractionHandler(subscribeInteraction(NO_CASH.name()));
        getFederateAmbassador().setTransactionStatusInteractionHandler(subscribeInteraction(TRANSACTION_STATUS.name()));
        getFederateAmbassador().setQueueSizeStateResponseInteractionHandler(subscribeInteraction(QUEUE_SIZE_STATE_RESPONSE.name()));
    }

    private void sendWithdrawalRequestInteraction(int clientId, int amount) {
        try {
            SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            int interactionHandle = getRTIambassador().getInteractionClassHandle("InteractionRoot." + WITHDRAWAL_REQUEST.name());
            int clientIdHandle = getRTIambassador().getParameterHandle("clientId", interactionHandle);
            int amountHandle = getRTIambassador().getParameterHandle("amount", interactionHandle);
            parameters.add(amountHandle, EncodingHelpers.encodeInt(amount));
            parameters.add(clientIdHandle, EncodingHelpers.encodeInt(clientId));
            LogicalTime time = convertTime(getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateLookahead());
            log(getFederateName() + ": sending" + WITHDRAWAL_REQUEST.name() + " for amount: " + amount + ", clientId:" + clientId);
            getRTIambassador().sendInteraction(interactionHandle, parameters, INT_TAG.getBytes(), time);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendLeaveRequestInteraction(int clientId) {
        try {
            SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            byte[] clientIdBytes = EncodingHelpers.encodeInt(clientId);
            int interactionHandle = getRTIambassador().getInteractionClassHandle("InteractionRoot." + LEAVE_REQUEST.name());
            int clientIdHandle = getRTIambassador().getParameterHandle("clientId", interactionHandle);
            parameters.add(clientIdHandle, clientIdBytes);
            LogicalTime time = convertTime(getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateLookahead());
            log(getFederateName() + ": sending " + LEAVE_REQUEST.name() + " for clientId: " + clientId);
            getRTIambassador().sendInteraction(interactionHandle, parameters, INT_TAG.getBytes(), time);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendEntryRequestInteraction(int clientId) {
        try {
            SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            byte[] clientIdBytes = EncodingHelpers.encodeInt(clientId);
            int interactionHandle = getRTIambassador().getInteractionClassHandle("InteractionRoot." + ENTRY_REQUEST.name());
            int clientIdHandle = getRTIambassador().getParameterHandle("clientId", interactionHandle);
            parameters.add(clientIdHandle, clientIdBytes);
            LogicalTime time = convertTime(getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateLookahead());
            log(getFederateName() + ": sending " + ENTRY_REQUEST.name() + " for clientId: " + clientId);
            getRTIambassador().sendInteraction(interactionHandle, parameters, INT_TAG.getBytes(), time);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
