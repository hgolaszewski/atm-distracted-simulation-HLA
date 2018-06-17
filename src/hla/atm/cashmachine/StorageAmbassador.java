package hla.atm.cashmachine;

import static hla.atm.utils.Utils.SYNC_POINT;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;

import java.util.ArrayList;

public class StorageAmbassador extends NullFederateAmbassador {

    boolean running = true;

    double grantedTime = 0.0;
    double federateTime = 0.0;
    double federateLookahead = 1.0;

    boolean isAnnounced = false;
    boolean isReadyToRun = false;

    boolean isAdvancing = false;
    boolean isRegulating = false;
    boolean isConstrained = false;

    int cashRefilledInteractionHandle = 0;
    int withdrawalRequestInteractionHandle = 0;

    ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

    public void synchronizationPointRegistrationFailed(String label) {
        log("CashMachineFederateAmbassador: Failed to register sync point: " + label);
    }

    public void synchronizationPointRegistrationSucceeded(String label) {
        log("CashMachineFederateAmbassador: Successfully registered sync point: " + label);
    }

    public void announceSynchronizationPoint(String label, byte[] tag) {
        log("CashMachineFederateAmbassador: synchronization point announced: " + label);
        if(label.equals(SYNC_POINT)) {
            this.isAnnounced = true;
        }
    }

    public void federationSynchronized(String label) {
        log("CashMachineFederateAmbassador: federation Synchronized: " + label);
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
        this.grantedTime = convertTime(theTime);
        this.isAdvancing = false;
    }

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
                                   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {
        StringBuilder messageToLog = new StringBuilder("CashMachineAmbassador: Interaction Received: ");
        if(interactionClass == cashRefilledInteractionHandle) {
            try {
                int refilledCashAmount = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time =  convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(refilledCashAmount, ExternalEvent.EventType.REFILL, time);
                externalEvents.add(externalEvent);
                messageToLog.append("CashRefilled, time = ")
                            .append(time)
                            .append(" amount = ")
                            .append(refilledCashAmount)
                            .append("\n");
            } catch (ArrayIndexOutOfBounds ignored) {}
        } else if (interactionClass == withdrawalRequestInteractionHandle) {
            try {
                int cashToWithdraw = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time =  convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(cashToWithdraw, ExternalEvent.EventType.WITHDRAW, time);
                externalEvents.add(externalEvent);
                messageToLog.append("WithdrawalRequest, time = ")
                            .append(time)
                            .append(" amount = ")
                            .append(cashToWithdraw)
                            .append("\n");
            } catch (ArrayIndexOutOfBounds ignored) {}
        }
        log(messageToLog.toString());
    }

}
