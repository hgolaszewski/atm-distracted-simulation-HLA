package hla.atm.client;

import static hla.atm.utils.Utils.*;
import static hla.atm.utils.event.EventType.ENTRY_REQUEST;

import hla.atm.client.data.Client;
import hla.atm.utils.event.ExternalEvent;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ClientFederate {

    private RTIambassador rtiamb;
    private ClientAmbassador fedamb;
    private List<Client> clientsToAdd = new ArrayList<>();
    private List<Client> clientsAdded = new ArrayList<>();

    void runFederate() throws RTIexception {
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
        fedamb = new ClientAmbassador();
        tryCreateFederation();
        tryJoinFederationAndRegisterSyncPoint();
        waitForSynchronization("ClientFederate");

        rtiamb.synchronizationPointAchieved(SYNC_POINT);
        log("ClientFederate: achieved sync point: " + SYNC_POINT + ", waiting for federation...");

        while(!fedamb.isReadyToRun) {
            rtiamb.tick();
        }

        enableTimePolicy();
        declarePublishAndSubscribePolicy();
        ExternalEvent addFirstClientEvent = new ExternalEvent(ENTRY_REQUEST, fedamb.federateTime);
        fedamb.getExternalEvents().add(addFirstClientEvent);

        while(fedamb.running) {
            advanceTime();

            for(ExternalEvent externalEvent: fedamb.getExternalEvents()) {
                switch (externalEvent.getEventType()) {

                    // Event generowany przez samego siebie, w celu stworzenia klienta
                    case ENTRY_REQUEST:
                        // Czas niecierpliwienia
                        Random r = new Random();
                        double randomValue = 1.0 + (2.0 - 1.0) * r.nextDouble();

                        Client clientToAdd = new Client();
                        clientToAdd.setLeaveTime(fedamb.federateTime + randomValue);

                        clientsToAdd.add(clientToAdd);
                        sendEntryRequestInteraction(clientToAdd.getId());
                        break;

                    case QUEUE_SIZE_STATE_RESPONSE:
                        int clientQueuesSizeStateResponseId = externalEvent.getClientId();
                        boolean queueIsFull = externalEvent.isQueueIsFull();
                        Client returnedClient = clientsToAdd.stream()
                                .filter(client -> client.getId() == clientQueuesSizeStateResponseId)
                                .findFirst()
                                .get();
                        if (queueIsFull) {
                            clientsToAdd.remove(returnedClient);
                        } else {
                            clientsAdded.add(returnedClient);
                            int index = clientsAdded.indexOf(returnedClient);
                            if (index == 0) {
                                sendWithdrawalRequestInteraction(clientQueuesSizeStateResponseId, 2000);
                            }
                        }
                        clientsToAdd.remove(returnedClient);
                        break;

                    case TRANSACTION_STATUS:
                        int clientIdTransactionStatus = externalEvent.getClientId();
                        sendLeaveRequestInteraction(clientIdTransactionStatus);
                        Client clientToDeleteSuccessfully = clientsAdded.stream()
                                .filter(client -> client.getId() == clientIdTransactionStatus)
                                .findFirst()
                                .get();
                        clientsAdded.remove(clientToDeleteSuccessfully);
                        if(clientsAdded.size() > 0) {
                            Random random = new Random();
                            int cash = random.nextInt(3000) + 200;
                            sendWithdrawalRequestInteraction(clientsAdded.get(0).getId(), cash);
                        }
                        break;
                }

            }

            fedamb.getExternalEvents().clear();

            Random r = new Random();
            int random = r.nextInt(90);
            if(random % 2 == 1) {
                ExternalEvent newEntryRequest = new ExternalEvent(ENTRY_REQUEST, fedamb.federateTime);
                fedamb.getExternalEvents().add(newEntryRequest);
            }

            rtiamb.tick();
        }
    }

    private void tryJoinFederationAndRegisterSyncPoint() throws FederateAlreadyExecutionMember, FederationExecutionDoesNotExist, SaveInProgress, RestoreInProgress, RTIinternalError, ConcurrentAccessAttempted, FederateNotExecutionMember {
        rtiamb.joinFederationExecution("ClientFederate" , "ATMFederation", fedamb);
        log("ClientFederate: joined Federation as ClientFederate");

        rtiamb.registerFederationSynchronizationPoint(SYNC_POINT, null);

        while(!fedamb.isAnnounced) {
            rtiamb.tick();
        }
    }

    private void tryCreateFederation() throws CouldNotOpenFED, ErrorReadingFED, RTIinternalError, ConcurrentAccessAttempted {
        try {
            File fom = new File("atmfederation.fed");
            rtiamb.createFederationExecution("ATMFederation", fom.toURI().toURL());
            log("ClientFederate: created Federation");
        } catch(FederationExecutionAlreadyExists exists) {
            log("ClientFederate: didn't create federation, it already existed");
        } catch(MalformedURLException url) {
            log("ClientFederate: exception processing fom: " + url.getMessage());
            url.printStackTrace();
        }
    }

    private void enableTimePolicy() throws RTIexception {
        LogicalTime currentTime = convertTime(fedamb.federateTime);
        LogicalTimeInterval lookahead = convertInterval(fedamb.federateLookahead);
        this.rtiamb.enableTimeRegulation(currentTime, lookahead);
        while(!fedamb.isRegulating) {
            rtiamb.tick();
        }
        this.rtiamb.enableTimeConstrained();
        while(!fedamb.isConstrained) {
            rtiamb.tick();
        }
    }

    private void declarePublishAndSubscribePolicy() throws RTIexception {
        publishInteraction("EntryRequest");
        publishInteraction("LeaveRequest");
        publishInteraction("WithdrawalRequest");
        fedamb.setNoCashInteractionHandler(subscribeInteraction("NoCash"));
        fedamb.setTransactionStatusInteractionHandler(subscribeInteraction("TransactionStatus"));
        fedamb.setQueueSizeStateResponseInteractionHandler(subscribeInteraction("QueueSizeStateResponse"));
    }

    private void sendWithdrawalRequestInteraction(int clientId, int amount) throws RTIinternalError, NameNotFound, FederateNotExecutionMember, InteractionClassNotDefined, RestoreInProgress, InteractionClassNotPublished, SaveInProgress, InvalidFederationTime, ConcurrentAccessAttempted, InteractionParameterNotDefined {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.WithdrawalRequest");
        int clientIdHandle = rtiamb.getParameterHandle("clientId", interactionHandle);
        int amountHandle = rtiamb.getParameterHandle("amount", interactionHandle);

        parameters.add(amountHandle, EncodingHelpers.encodeInt(amount));
        parameters.add(clientIdHandle, EncodingHelpers.encodeInt(clientId));

        LogicalTime time = convertTime(fedamb.federateTime + fedamb.federateLookahead);
        log("ClientFederate: sending WithdrawalRequest for amount: " + amount + ", client = " + clientId);
        rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
    }

    private void sendLeaveRequestInteraction(int clientId) throws RTIinternalError, NameNotFound, FederateNotExecutionMember, InteractionClassNotDefined, RestoreInProgress, InteractionClassNotPublished, SaveInProgress, InvalidFederationTime, ConcurrentAccessAttempted, InteractionParameterNotDefined {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        byte[] clientIdBytes = EncodingHelpers.encodeInt(clientId);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.LeaveRequest");
        int clientIdHandle = rtiamb.getParameterHandle("clientId", interactionHandle);

        parameters.add(clientIdHandle, clientIdBytes);

        LogicalTime time = convertTime(fedamb.federateTime + fedamb.federateLookahead);
        log("ClientFederate: sending LeaveRequest for clientId: " + clientId);
        rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
    }

    private void sendEntryRequestInteraction(int clientId) throws RTIinternalError, NameNotFound, FederateNotExecutionMember, InteractionClassNotDefined, RestoreInProgress, InteractionClassNotPublished, SaveInProgress, InvalidFederationTime, ConcurrentAccessAttempted, InteractionParameterNotDefined {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        byte[] clientIdBytes = EncodingHelpers.encodeInt(clientId);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.EntryRequest");
        int clientIdHandle = rtiamb.getParameterHandle("clientId", interactionHandle);

        parameters.add(clientIdHandle, clientIdBytes);

        LogicalTime time = convertTime(fedamb.federateTime + fedamb.federateLookahead);
        log("ClientFederate: sending EntryRequest for clientId: " + clientId);
        rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
    }

    protected void publishInteraction(String interactionName) throws RTIexception {
        int handle = rtiamb.getInteractionClassHandle("InteractionRoot." + interactionName);
        rtiamb.publishInteractionClass(handle);
        log("ClientFederate: published interaction " + interactionName + " (" + handle + ")");
    }

    protected int subscribeInteraction(String interactionName) throws RTIexception {
        int handle = rtiamb.getInteractionClassHandle("InteractionRoot." + interactionName);
        rtiamb.subscribeInteractionClass(handle);
        log("ClientFederate: subscribed to interaction " + interactionName);
        return handle;
    }

    private void advanceTime() throws RTIexception {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime(fedamb.federateTime + fedamb.federateStep);
        rtiamb.timeAdvanceRequest(newTime);
        while (fedamb.isAdvancing) {
            rtiamb.tick();
        }
    }

}
