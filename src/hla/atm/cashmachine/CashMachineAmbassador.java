package hla.atm.cashmachine;

import static hla.atm.utils.Utils.CASH_MACHINE_AMBASSADOR_NAME;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;
import static hla.atm.commons.event.EventType.CASH_REFILL_REQUEST;
import static hla.atm.commons.event.EventType.WITHDRAWAL_REQUEST;

import hla.atm.commons.AbstractAmbassador;
import hla.atm.commons.event.ExternalEvent;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;

class CashMachineAmbassador extends AbstractAmbassador {

    private int cashRefillRequestInteractionHandle = 0;
    private int withdrawalRequestInteractionHandle = 0;

    CashMachineAmbassador() {
        super(CASH_MACHINE_AMBASSADOR_NAME);
    }

    @Override
    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
                                   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {

        StringBuilder messageToLog = new StringBuilder(getAmbassadorName() + ": Interaction Received: ");

        try {
            // CASH_REFILL_REQUEST INTERACTION
            if (interactionClass == cashRefillRequestInteractionHandle) {
                int refilledCashAmount = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time = convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(CASH_REFILL_REQUEST, time)
                        .withAmount(refilledCashAmount);
                getExternalEvents().add(externalEvent);
                messageToLog.append(CASH_REFILL_REQUEST.name())
                        .append(", time = ")
                        .append(time)
                        .append(" amount = ")
                        .append(refilledCashAmount)
                        .append("\n");
                // WITHDRAWAL_REQUEST INTERACTION
            } else if (interactionClass == withdrawalRequestInteractionHandle) {
                int cashToWithdraw = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(1));
                double time = convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(WITHDRAWAL_REQUEST, time)
                        .withAmount(cashToWithdraw)
                        .withClientId(clientId);
                getExternalEvents().add(externalEvent);
                messageToLog.append(WITHDRAWAL_REQUEST.name())
                        .append(", time = ")
                        .append(time)
                        .append(" amount = ")
                        .append(cashToWithdraw)
                        .append(" clientId = ")
                        .append(clientId)
                        .append("\n");
            }
        } catch (ArrayIndexOutOfBounds e) {
            e.printStackTrace();
        }
        log(messageToLog.toString());
    }

    void setCashRefillRequestInteractionHandle(int cashRefillRequestInteractionHandle) {
        this.cashRefillRequestInteractionHandle = cashRefillRequestInteractionHandle;
    }

    void setWithdrawalRequestInteractionHandle(int withdrawalRequestInteractionHandle) {
        this.withdrawalRequestInteractionHandle = withdrawalRequestInteractionHandle;
    }

}
