package hla.atm.cashmachine;


import static hla.atm.utils.Utils.*;

import hla.rti.*;
import hla.rti.jlc.RtiFactoryFactory;

import java.io.File;
import java.net.MalformedURLException;

public class StorageFederate {

    private int cash = 10000;
    private RTIambassador rtiamb;
    private StorageAmbassador fedamb;

    private void runFederate() throws Exception {
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();

        try {
            File fom = new File("atmfederation.fed");
            rtiamb.createFederationExecution("ATMFederation", fom.toURI().toURL());
            log("CashMachineFederate: created Federation");
        } catch(FederationExecutionAlreadyExists exists) {
            log("CashMachineFederate: didn't create federation, it already existed");
        } catch(MalformedURLException url) {
            log("CashMachineFederate: exception processing fom: " + url.getMessage());
            url.printStackTrace();
            return;
        }

        fedamb = new StorageAmbassador();
        rtiamb.joinFederationExecution("CashMachineFederate", "ATMFederation", fedamb);
        log("CashMachineFederate: joined Federation as CashMachineFederate");

        rtiamb.registerFederationSynchronizationPoint(SYNC_POINT, null);

        while(!fedamb.isAnnounced) {
            rtiamb.tick();
        }

		waitForSynchronization("CashMachineFederate");

        rtiamb.synchronizationPointAchieved(SYNC_POINT);
        log("CashMachineFederate: achieved sync point: " + SYNC_POINT + ", waiting for federation...");
        while(!fedamb.isReadyToRun) {
            rtiamb.tick();
        }

        enableTimePolicy();
        publishAndSubscribe();

        while (fedamb.running) {
        	double timeStep = 10.0;
            double timeToAdvance = fedamb.federateTime + timeStep;
            advanceTime(timeToAdvance);

            if(fedamb.externalEvents.size() > 0) {
                fedamb.externalEvents.sort(new ExternalEvent.ExternalEventComparator());
                for(ExternalEvent externalEvent : fedamb.externalEvents) {
                    fedamb.federateTime = externalEvent.getTime();
                    switch (externalEvent.getEventType()) {
						case REFILL:
                            this.addToStock(externalEvent.getAmount());
                            break;
						case WITHDRAW:
                            this.getFromStock(externalEvent.getAmount());
                            break;
                    }
                }
                fedamb.externalEvents.clear();
            }
            rtiamb.tick();
        }

    }

    private void addToStock(int amountToAdd) {
        this.cash += amountToAdd;
        log("CashMachineFederate: refilled " + amountToAdd + " at time: " + fedamb.federateTime + ". Current cash: " + this.cash);
    }

    private void getFromStock(int amountToGet) {
        if(this.cash - amountToGet < 0) {
            log("CashMachineFederate: : not enough cash at cash machine!");
        } else {
            this.cash -= amountToGet;
            log("CashMachineFederate: removed " + amountToGet + " at time: " + fedamb.federateTime + ". Current cash:" + " " + this.cash);
        }
    }

    private void advanceTime(double timeToAdvance) throws RTIexception {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime(timeToAdvance);
        rtiamb.timeAdvanceRequest(newTime);
        while(fedamb.isAdvancing) {
            rtiamb.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        int cashRefilledHandle = rtiamb.getInteractionClassHandle("InteractionRoot.CashRefilled");
        fedamb.cashRefilledInteractionHandle = cashRefilledHandle;
        rtiamb.subscribeInteractionClass(cashRefilledHandle);

        int withdrawalRequestHandle = rtiamb.getInteractionClassHandle("InteractionRoot.WithdrawalRequest");
        fedamb.withdrawalRequestInteractionHandle = withdrawalRequestHandle;
        rtiamb.subscribeInteractionClass(withdrawalRequestHandle);
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

    public static void main(String[] args) {
        try {
            new StorageFederate().runFederate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
