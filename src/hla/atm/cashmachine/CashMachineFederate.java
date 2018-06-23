package hla.atm.cashmachine;

import static hla.atm.utils.Utils.*;

import hla.atm.utils.event.ExternalEvent;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class CashMachineFederate {

    private int cash = 30000;
    private RTIambassador rtiamb;
    private CashMachineAmbassador fedamb;
    private boolean noCashSent = false;

    void runFederate() throws Exception {
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
        fedamb = new CashMachineAmbassador();
        tryCreateFederation();
        tryJoinFederationAndRegisterSyncPoint();
		waitForSynchronization("CashMachineFederate");

        rtiamb.synchronizationPointAchieved(SYNC_POINT);
        log("CashMachineFederate: achieved sync point: " + SYNC_POINT + ", waiting for federation...");
        while(!fedamb.isReadyToRun) {
            rtiamb.tick();
        }

        enableTimePolicy();
        declarePublishAndSubscribePolicy();

        while (fedamb.running) {
            advanceTime();
            List<ExternalEvent> eventsToDelete = new ArrayList<>();
                for(ExternalEvent externalEvent : fedamb.externalEvents) {
                    switch (externalEvent.getEventType()) {
                        case CASH_REFILL_REQUEST:
                            this.addCash(externalEvent.getAmount());
                            eventsToDelete.add(externalEvent);
                            noCashSent = false;
                            break;
                        case WITHDRAWAL_REQUEST:
                            if (externalEvent.getAmount() > this.cash) {
                                if (!noCashSent) {
                                    sendNoCashInteraction();
                                }
                                noCashSent = true;
                            } else {
                                this.getCash(externalEvent.getAmount());
                                sendTransactionStatusInteraction(true, externalEvent.getClientId());
                                eventsToDelete.add(externalEvent);
                            }
                            break;
                    }
                }
            fedamb.getExternalEvents().removeAll(eventsToDelete);
            rtiamb.tick();
        }
    }

    private void tryJoinFederationAndRegisterSyncPoint() throws FederateAlreadyExecutionMember, FederationExecutionDoesNotExist, SaveInProgress, RestoreInProgress, RTIinternalError, ConcurrentAccessAttempted, FederateNotExecutionMember {
        rtiamb.joinFederationExecution("CashMachineFederate", "ATMFederation", fedamb);
        log("CashMachineFederate: joined Federation as CashMachineFederate");

        rtiamb.registerFederationSynchronizationPoint(SYNC_POINT, null);

        while(!fedamb.isAnnounced) {
            rtiamb.tick();
        }
    }

    private void tryCreateFederation() throws CouldNotOpenFED, ErrorReadingFED, RTIinternalError,
            ConcurrentAccessAttempted {
        try {
            File fom = new File("atmfederation.fed");
            rtiamb.createFederationExecution("ATMFederation", fom.toURI().toURL());
            log("CashMachineFederate: created Federation");
        } catch(FederationExecutionAlreadyExists exists) {
            log("CashMachineFederate: didn't create federation, it already existed");
        } catch(MalformedURLException url) {
            log("CashMachineFederate: exception processing fom: " + url.getMessage());
            url.printStackTrace();
        }
    }

    private void addCash(int amountToAdd) {
        this.cash += amountToAdd;
        log("CashMachineFederate: refilled " + amountToAdd + " at time: " + fedamb.federateTime + ". Current cash: " + this.cash);
    }

    private void getCash(int amountToGet) {
        this.cash -= amountToGet;
        log("CashMachineFederate: removed " + amountToGet + " at time: " + fedamb.federateTime + ". Current cash:" + " " + this.cash);
    }

    private void advanceTime() throws RTIexception {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime(fedamb.federateTime + fedamb.federateStep);
        rtiamb.timeAdvanceRequest(newTime);
        while (fedamb.isAdvancing) {
            rtiamb.tick();
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
        publishInteraction("TransactionStatus");
        publishInteraction("NoCash");
		fedamb.setWithdrawalRequestInteractionHandle(subscribeInteraction("WithdrawalRequest"));
		fedamb.setCashRefillRequestInteractionHandle(subscribeInteraction("CashRefillRequest"));

    }

    private void publishInteraction(String interactionName) throws RTIexception {
        int handle = rtiamb.getInteractionClassHandle("InteractionRoot." + interactionName);
        rtiamb.publishInteractionClass(handle);
        log("CashMachineFederate: published interaction " + interactionName + " (" + handle + ")");
    }

    private int subscribeInteraction(String interactionName) throws RTIexception {
        int handle = rtiamb.getInteractionClassHandle("InteractionRoot." + interactionName);
        rtiamb.subscribeInteractionClass(handle);
        log("CashMachineFederate: subscribed to interaction " + interactionName);
        return handle;
    }

    private void sendNoCashInteraction() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, InteractionClassNotDefined, RestoreInProgress, InteractionClassNotPublished, SaveInProgress, InvalidFederationTime, ConcurrentAccessAttempted, InteractionParameterNotDefined {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.NoCash");

        LogicalTime time = convertTime(fedamb.federateTime + fedamb.federateLookahead);
        log("CashMachineFederate: sending NoCash");
        rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
    }

    private void sendTransactionStatusInteraction(boolean transactionSucceed, int clientId) throws NameNotFound, FederateNotExecutionMember, RTIinternalError, InteractionClassNotDefined, RestoreInProgress, InteractionClassNotPublished, SaveInProgress, InvalidFederationTime, ConcurrentAccessAttempted, InteractionParameterNotDefined {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.TransactionStatus");
        int transactionSucceedHandle = rtiamb.getParameterHandle("transactionSucceed", interactionHandle);
        int clientIdHandle = rtiamb.getParameterHandle("clientId", interactionHandle);

        parameters.add(clientIdHandle, EncodingHelpers.encodeInt(clientId));
        parameters.add(transactionSucceedHandle, EncodingHelpers.encodeBoolean(transactionSucceed));

        LogicalTime time = convertTime(fedamb.federateTime + fedamb.federateLookahead);
        log("CashMachineFederate: sending TransactionStatus: " + transactionSucceed + ", clientId = " + clientId);
        rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
    }

}
