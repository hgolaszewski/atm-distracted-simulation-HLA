package hla.atm.client;

import static hla.atm.utils.Utils.CLIENT_AMBASSADOR_NAME;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;
import static hla.atm.commons.event.EventType.*;
import hla.atm.commons.AbstractAmbassador;
import hla.atm.commons.event.ExternalEvent;
import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.EncodingHelpers;

class ClientAmbassador extends AbstractAmbassador {

    private int queueSizeStateResponseInteractionHandler = 0;
    private int transactionStatusInteractionHandler = 0;
    private int noCashInteractionHandler = 0;

    ClientAmbassador() {
        super(CLIENT_AMBASSADOR_NAME);
    }

    @Override
    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
                                   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {

        StringBuilder messageToLog = new StringBuilder(getAmbassadorName() + ": Interaction Received: ");

        try {

            // QUEUE_SIZE_STATE_RESPONSE INTERACTION
            if (interactionClass == queueSizeStateResponseInteractionHandler) {
                boolean queueIsFull = EncodingHelpers.decodeBoolean(theInteraction.getValue(0));
                int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(1));
                double time = convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(QUEUE_SIZE_STATE_RESPONSE, time)
                        .withQueueIsFull(queueIsFull)
                        .withClientId(clientId);
                getExternalEvents().add(externalEvent);
                messageToLog.append(QUEUE_SIZE_STATE_RESPONSE.name())
                        .append(", time = ")
                        .append(time)
                        .append(" queueIsFull = ")
                        .append(queueIsFull)
                        .append(" clientId = ")
                        .append(clientId)
                        .append("\n");
                // NO_CASH INTERACTION
            } else if (interactionClass == noCashInteractionHandler) {
                double time = convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(NO_CASH, time);
                getExternalEvents().add(externalEvent);
                messageToLog.append(NO_CASH.name())
                        .append(", time = ")
                        .append(time)
                        .append("\n");
                // TRANSACTION_STATUS INTERACTION
            } else if (interactionClass == transactionStatusInteractionHandler) {
                int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time = convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(TRANSACTION_STATUS, time)
                        .withClientId(clientId);
                getExternalEvents().add(externalEvent);
                messageToLog.append(TRANSACTION_STATUS.name())
                        .append(", time = ")
                        .append(time)
                        .append(" clientId = ")
                        .append(clientId)
                        .append("\n");
            }
        } catch (ArrayIndexOutOfBounds e) {
            e.printStackTrace();
        }
        log(messageToLog.toString());
    }

    void setQueueSizeStateResponseInteractionHandler(int queueSizeStateResponseInteractionHandler) {
        this.queueSizeStateResponseInteractionHandler = queueSizeStateResponseInteractionHandler;
    }

    void setNoCashInteractionHandler(int noCashInteractionHandler) {
        this.noCashInteractionHandler = noCashInteractionHandler;
    }

    void setTransactionStatusInteractionHandler(int transactionStatusInteractionHandler) {
        this.transactionStatusInteractionHandler = transactionStatusInteractionHandler;
    }

}
