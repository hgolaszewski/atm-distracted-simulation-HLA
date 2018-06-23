package hla.atm.service;

import static hla.atm.utils.Utils.SYNC_POINT;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;
import static hla.atm.utils.event.EventType.*;

import java.util.ArrayList;

import hla.atm.utils.event.ExternalEvent;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.NullFederateAmbassador;

public class ServiceAmbassador extends NullFederateAmbassador {

    double federateTime  = 0.0;
    double federateLookahead = 1.0;
    double federateStep = 2.0;

    boolean isRegulating = false;
    boolean isConstrained = false;
    boolean isAdvancing = false;

    boolean isAnnounced = false;
    boolean isReadyToRun = false;

    boolean running = true;

    // Subscribed interaction handlers
    int noCashInteractionHandler = 0;

    // Received events
    private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

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

    @Override
    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
                                   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {

        StringBuilder messageToLog = new StringBuilder("ServiceFederateAmbassador: Interaction Received: ");

        // NO_CASH INTERACTION
        if(interactionClass == noCashInteractionHandler) {
            double time = convertTime(theTime);
            ExternalEvent externalEvent = new ExternalEvent(NO_CASH, time);
            externalEvents.add(externalEvent);
            messageToLog.append("EntryRequest, time = ")
                    .append(time)
                    .append("\n");
        }
        log(messageToLog.toString());
    }

    public ArrayList<ExternalEvent> getExternalEvents() {
        return externalEvents;
    }

    public void setNoCashInteractionHandler(int noCashInteractionHandler) {
        this.noCashInteractionHandler = noCashInteractionHandler;
    }

}
