package hla.atm.service;


import static hla.atm.utils.Utils.*;

import hla.atm.utils.event.ExternalEvent;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Random;

public class ServiceFederate {

    private RTIambassador rtiamb;
    private ServiceAmbassador fedamb;

    void runFederate() throws RTIexception{
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
        fedamb = new ServiceAmbassador();

        tryCreateFederation();
        tryJoinFederationAndRegisterSyncPoint();

        waitForSynchronization("ServiceFederate");

        rtiamb.synchronizationPointAchieved(SYNC_POINT);
        log("ServiceFederate: achieved sync point: " + SYNC_POINT + ", waiting for federation...");
        while(!fedamb.isReadyToRun) {
            rtiamb.tick();
        }

        enableTimePolicy();
        declarePublishAndSubscribePolicy();

        while (fedamb.running) {
            advanceTime();

            for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
                switch (externalEvent.getEventType()) {
                    case NO_CASH:
                        Random random = new Random();
                        int cashToRefill = random.nextInt(20000) + 20000;
                        sendCashRefillRequestInteraction(cashToRefill);
                        break;
                }
            }

            fedamb.getExternalEvents().clear();
            rtiamb.tick();
        }

    }

    private void sendCashRefillRequestInteraction(int amount) throws NameNotFound, FederateNotExecutionMember, RTIinternalError, InteractionClassNotDefined, RestoreInProgress, InteractionClassNotPublished, SaveInProgress, InvalidFederationTime, ConcurrentAccessAttempted, InteractionParameterNotDefined {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.CashRefillRequest");
        int amountHandle = rtiamb.getParameterHandle("amount", interactionHandle);

        parameters.add(amountHandle, EncodingHelpers.encodeInt(amount));

        LogicalTime time = convertTime(fedamb.federateTime + fedamb.federateStep);
        log("ServiceFederate: sending CashRefillRequest: " + amount);
        rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
    }

    private void declarePublishAndSubscribePolicy() throws RTIexception {
        publishInteraction("CashRefillRequest");
        fedamb.setNoCashInteractionHandler(subscribeInteraction("NoCash"));
    }

    private void tryJoinFederationAndRegisterSyncPoint() throws FederateAlreadyExecutionMember, FederationExecutionDoesNotExist, SaveInProgress, RestoreInProgress, RTIinternalError, ConcurrentAccessAttempted, FederateNotExecutionMember {
        rtiamb.joinFederationExecution("ServiceFederate", "ATMFederation", fedamb);
        log("ServiceFederate: joined Federation as ServiceFederate");

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
            log("ServiceFederate: created Federation");
        } catch(FederationExecutionAlreadyExists exists) {
            log("ServiceFederate: didn't create federation, it already existed");
        } catch(MalformedURLException url) {
            log("ServiceFederate: exception processing fom: " + url.getMessage());
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

    private void advanceTime() throws RTIexception {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime(fedamb.federateTime + fedamb.federateStep);
        rtiamb.timeAdvanceRequest(newTime);
        while (fedamb.isAdvancing) {
            rtiamb.tick();
        }
    }

    private void publishInteraction(String interactionName) throws RTIexception {
        int handle = rtiamb.getInteractionClassHandle("InteractionRoot." + interactionName);
        rtiamb.publishInteractionClass(handle);
        log("ServiceFederate: published interaction " + interactionName + " (" + handle + ")");
    }

    private int subscribeInteraction(String interactionName) throws RTIexception {
        int handle = rtiamb.getInteractionClassHandle("InteractionRoot." + interactionName);
        rtiamb.subscribeInteractionClass(handle);
        log("ServiceFederate: subscribed to interaction " + interactionName);
        return handle;
    }

}
