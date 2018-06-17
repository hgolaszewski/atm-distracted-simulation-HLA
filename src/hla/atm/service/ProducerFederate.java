package hla.atm.service;


import static hla.atm.utils.Utils.*;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Random;

public class ProducerFederate {

    private RTIambassador rtiamb;
    private ProducerAmbassador fedamb;

    private void runFederate() throws RTIexception{
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
        try {
            File fom = new File("atmfederation.fed");
            rtiamb.createFederationExecution("ATMFederation", fom.toURI().toURL());
            log("ServiceFederate: created Federation");
        } catch(FederationExecutionAlreadyExists exists) {
            log("ServiceFederate: didn't create federation, it already existed");
        } catch(MalformedURLException url) {
            log("ServiceFederate: exception processing fom: " + url.getMessage());
            url.printStackTrace();
            return;
        }

        fedamb = new ProducerAmbassador();
        rtiamb.joinFederationExecution("ServiceFederate", "ATMFederation", fedamb);
        log("ServiceFederate: joined Federation as ServiceFederate");

        rtiamb.registerFederationSynchronizationPoint(SYNC_POINT, null);

        while(!fedamb.isAnnounced) {
            rtiamb.tick();
        }

        waitForSynchronization("ServiceFederate");

        rtiamb.synchronizationPointAchieved(SYNC_POINT);
        log("ServiceFederate: achieved sync point: " + SYNC_POINT + ", waiting for federation...");
        while(!fedamb.isReadyToRun) {
            rtiamb.tick();
        }

        enableTimePolicy();
        publishAndSubscribe();

        while (fedamb.running) {
            System.out.println();
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
        int amountInt = random.nextInt(10000) + 1;
        byte[] amount = EncodingHelpers.encodeInt(amountInt);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.CashRefilled");
        int amountHandle = rtiamb.getParameterHandle("amount", interactionHandle);

        parameters.add(amountHandle, amount);

        LogicalTime time = convertTime(timeStep);
        log("ServiceFederate: sending CashRefilled: " + amountInt);
        rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
    }

    private void publishAndSubscribe() throws RTIexception {
        int cashRefilledHandle = rtiamb.getInteractionClassHandle("InteractionRoot.CashRefilled");
        rtiamb.publishInteractionClass(cashRefilledHandle);
    }

    private void advanceTime(double timeStep) throws RTIexception {
        log("ServiceFederate: requesting time advance for: " + timeStep);
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime(fedamb.federateTime + timeStep);
        rtiamb.timeAdvanceRequest(newTime);
        while(fedamb.isAdvancing) {
            rtiamb.tick();
        }
    }

    public static void main(String[] args) {
        try {
            new ProducerFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

}
