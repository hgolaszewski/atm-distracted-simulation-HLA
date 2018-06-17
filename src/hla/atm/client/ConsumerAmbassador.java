package hla.atm.client;

import static hla.atm.utils.Utils.SYNC_POINT;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;

import hla.rti.LogicalTime;
import hla.rti.jlc.NullFederateAmbassador;

public class ConsumerAmbassador extends NullFederateAmbassador {

    double federateTime  = 0.0;
    double federateLookahead = 1.0;

    boolean isRegulating = false;
    boolean isConstrained = false;
    boolean isAdvancing = false;

    boolean isAnnounced = false;
    boolean isReadyToRun = false;

    boolean running = true;

    public void synchronizationPointRegistrationFailed(String label) {
        log("ClientFederateAmbassador: failed to register sync point: " + label);
    }

    public void synchronizationPointRegistrationSucceeded(String label) {
        log("ClientFederateAmbassador: successfully registered sync point: " + label);
    }

    public void announceSynchronizationPoint(String label, byte[] tag) {
        log("ClientFederateAmbassador: synchronization point announced: " + label);
        if(label.equals(SYNC_POINT)) {
            this.isAnnounced = true;
        }
    }

    public void federationSynchronized(String label) {
        log("ClientFederateAmbassador: federation synchronized: " + label);
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

}
