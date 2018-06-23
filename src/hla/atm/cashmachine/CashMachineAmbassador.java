package hla.atm.cashmachine;

import static hla.atm.utils.Utils.SYNC_POINT;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;
import static hla.atm.utils.event.EventType.CASH_REFILL_REQUEST;
import static hla.atm.utils.event.EventType.WITHDRAWAL_REQUEST;

import hla.atm.utils.event.ExternalEvent;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;

import java.util.ArrayList;

public class CashMachineAmbassador extends NullFederateAmbassador {

    double federateTime  = 0.0;
    double federateLookahead = 1.0;
    double federateStep = 2.0;

    boolean isRegulating = false;
    boolean isConstrained = false;
    boolean isAdvancing = false;

    boolean isAnnounced = false;
    boolean isReadyToRun = false;

    boolean running = true;

    int cashRefillRequestInteractionHandle = 0;
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
        this.federateTime = convertTime(theTime);
        this.isAdvancing = false;
    }

    @Override
    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
                                   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {
        StringBuilder messageToLog = new StringBuilder("CashMachineAmbassador: Interaction Received: ");

        // CASH_REFILL_REQUEST INTERACTION
        if(interactionClass == cashRefillRequestInteractionHandle) {
            try {
                int refilledCashAmount = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time =  convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(CASH_REFILL_REQUEST, time).withAmount(refilledCashAmount);
                externalEvents.add(externalEvent);
                messageToLog.append("CashRefilled, time = ")
                            .append(time)
                            .append(" amount = ")
                            .append(refilledCashAmount)
                            .append("\n");
            } catch (ArrayIndexOutOfBounds ignored) {}
            // WITHDRAWAL_REQUEST INTERACTION
        } else if (interactionClass == withdrawalRequestInteractionHandle) {
            try {
                int cashToWithdraw = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(1));
                double time =  convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(WITHDRAWAL_REQUEST, time)
                        .withAmount(cashToWithdraw)
                        .withClientId(clientId);
                externalEvents.add(externalEvent);
                messageToLog.append("WithdrawalRequest, time = ")
                            .append(time)
                            .append(" amount = ")
                            .append(cashToWithdraw)
                            .append(" clientId = ")
                            .append(clientId)
                            .append("\n");
            } catch (ArrayIndexOutOfBounds ignored) {}
        }
        log(messageToLog.toString());
    }

    public ArrayList<ExternalEvent> getExternalEvents() {
        return externalEvents;
    }

    public void setCashRefillRequestInteractionHandle(int cashRefillRequestInteractionHandle) {
        this.cashRefillRequestInteractionHandle = cashRefillRequestInteractionHandle;
    }

    public void setWithdrawalRequestInteractionHandle(int withdrawalRequestInteractionHandle) {
        this.withdrawalRequestInteractionHandle = withdrawalRequestInteractionHandle;
    }
}
