package hla.atm.client;

import static hla.atm.utils.Utils.*;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Random;

public class ConsumerFederate {

    private RTIambassador rtiamb;
    private ConsumerAmbassador fedamb;

    private void runFederate() throws RTIexception {
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();

        try {
            File fom = new File("atmfederation.fed");
            rtiamb.createFederationExecution("ATMFederation", fom.toURI().toURL());
            log("ClientFederate: created Federation");
        } catch(FederationExecutionAlreadyExists exists) {
            log("ClientFederate: didn't create federation, it already existed");
        } catch(MalformedURLException url) {
            log("ClientFederate: exception processing fom: " + url.getMessage());
            url.printStackTrace();
            return;
        }

        fedamb = new ConsumerAmbassador();
        rtiamb.joinFederationExecution("ClientFederate", "ATMFederation", fedamb);
        log("ClientFederate: joined Federation as ClientFederate");

        rtiamb.registerFederationSynchronizationPoint(SYNC_POINT, null);

        while(!fedamb.isAnnounced) {
            rtiamb.tick();
        }

        waitForSynchronization("ClientFederate");

        rtiamb.synchronizationPointAchieved(SYNC_POINT);
        log("ClientFederate: achieved sync point: " + SYNC_POINT + ", waiting for federation...");
        while(!fedamb.isReadyToRun) {
            rtiamb.tick();
        }

        enableTimePolicy();
        publishAndSubscribe();

        while(fedamb.running) {
            advanceTime(randomTime());
            sendInteraction(fedamb.federateTime + fedamb.federateLookahead);
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

    private void sendInteraction(double timeStep) throws RTIexception {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        Random random = new Random();
        int amountInt = random.nextInt(100000) + 1;
        byte[] amount = EncodingHelpers.encodeInt(amountInt);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.WithdrawalRequest");
        int amountHandle = rtiamb.getParameterHandle("amount", interactionHandle);

        parameters.add(amountHandle, amount);

        LogicalTime time = convertTime(timeStep);
        log("ClientFederate: sending WithdrawalRequest: " + amountInt);
        rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
    }

    private void publishAndSubscribe() throws RTIexception {
        int withdrawalRequestHandle = rtiamb.getInteractionClassHandle("InteractionRoot.WithdrawalRequest");
        rtiamb.publishInteractionClass(withdrawalRequestHandle);
    }

    private void advanceTime(double timeStep) throws RTIexception {
        log("ClientFederate: requesting time advance for: " + timeStep);
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime(fedamb.federateTime + timeStep);
        rtiamb.timeAdvanceRequest(newTime);
        while(fedamb.isAdvancing) {
            rtiamb.tick();
        }
    }

    public static void main(String[] args) {
        try {
            new ConsumerFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }


}
