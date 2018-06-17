package hla.atm.service;

import static hla.atm.utils.Utils.SYNC_POINT;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;

import hla.rti.LogicalTime;
import hla.rti.jlc.NullFederateAmbassador;

public class ProducerAmbassador extends NullFederateAmbassador {

    double federateTime = 0.0;
    double federateLookahead = 1.0;

    boolean isAnnounced = false;
    boolean isReadyToRun = false;

    boolean isAdvancing = false;
    boolean isRegulating = false;
    boolean isConstrained = false;

    boolean running = true;

    public void synchronizationPointRegistrationSucceeded(String label){
        log("ServiceFederateAmbassador: successfully registered sync point: " + label);
    }

    public void announceSynchronizationPoint(String label, byte[] tag) {
        log("ServiceFederateAmbassador: synchronization point announced: " + label);
        if(label.equals(SYNC_POINT)) {
            this.isAnnounced = true;
        }
    }

    public void federationSynchronized(String label) {
        log("ServiceFederateAmbassador: federation synchronized: " + label);
        if(label.equals(SYNC_POINT)) {
            this.isReadyToRun = true;
        }
    }

    public void timeRegulationEnabled(LogicalTime theFederateTime) {
        this.federateTime = convertTime(theFederateTime);
        this.isRegulating = true;
    }

    public void timeConstrainedEnabled(LogicalTime theFederateTime) {
        this.federateTime = convertTime(theFederateTime);
        this.isConstrained = true;
    }

    public void timeAdvanceGrant(LogicalTime theTime) {
        this.federateTime = convertTime(theTime);
        this.isAdvancing = false;
    }

    public void synchronizationPointRegistrationFailed(String label) {
        log("ServiceFederateAmbassador: failed to register sync point: " + label);
    }

}
