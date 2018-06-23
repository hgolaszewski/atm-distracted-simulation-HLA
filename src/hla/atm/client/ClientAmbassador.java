package hla.atm.client;

import static hla.atm.utils.Utils.SYNC_POINT;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;
import static hla.atm.utils.event.EventType.*;

import java.util.ArrayList;

import hla.atm.utils.event.ExternalEvent;
import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;

public class ClientAmbassador extends NullFederateAmbassador {

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
    int queueSizeStateResponseInteractionHandler = 0;
    int noCashInteractionHandler = 0;
    int transactionStatusInteractionHandler = 0;

    // Received events
    private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

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

    @Override
    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
                                   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {

        StringBuilder messageToLog = new StringBuilder("ClientFederateAmbassador: Interaction Received: ");

        // QUEUE_SIZE_STATE_RESPONSE INTERACTION
        if(interactionClass == queueSizeStateResponseInteractionHandler) {
            try {
                boolean queueIsFull = EncodingHelpers.decodeBoolean(theInteraction.getValue(0));
                int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(1));
                double time =  convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(QUEUE_SIZE_STATE_RESPONSE, time)
                        .withQueueIsFull(queueIsFull)
                        .withClientId(clientId);
                externalEvents.add(externalEvent);
                messageToLog.append("QueueSizeStateResponse, time = ")
                        .append(time)
                        .append(" queueIsFull = ")
                        .append(queueIsFull)
                        .append(" clientId = ")
                        .append(clientId)
                        .append("\n");
            } catch (ArrayIndexOutOfBounds ignored) {}
            // NO_CASH INTERACTION
        } else if (interactionClass == noCashInteractionHandler) {
                double time =  convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(NO_CASH, time);
                externalEvents.add(externalEvent);
                messageToLog.append("NoCash, time = ")
                        .append(time)
                        .append("\n");
            // TRANSACTION_STATUS INTERACTION
        } else if (interactionClass == transactionStatusInteractionHandler) {
            try {
                boolean transactionSucceed = EncodingHelpers.decodeBoolean(theInteraction.getValue(0));
                int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(1));
                double time =  convertTime(theTime);
                ExternalEvent externalEvent = new ExternalEvent(TRANSACTION_STATUS, time)
                        .withTransactionSucced(transactionSucceed)
                        .withClientId(clientId);
                externalEvents.add(externalEvent);
                messageToLog.append("TransactionStatus, time = ")
                        .append(time)
                        .append(" transactionSucceed = ")
                        .append(transactionSucceed)
                        .append(" clientId = ")
                        .append(clientId)
                        .append("\n");
            } catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
                arrayIndexOutOfBounds.printStackTrace();
            }
        }
        log(messageToLog.toString());
    }

    public void setQueueSizeStateResponseInteractionHandler(int queueSizeStateResponseInteractionHandler) {
        this.queueSizeStateResponseInteractionHandler = queueSizeStateResponseInteractionHandler;
    }

    public void setNoCashInteractionHandler(int noCashInteractionHandler) {
        this.noCashInteractionHandler = noCashInteractionHandler;
    }

    public void setTransactionStatusInteractionHandler(int transactionStatusInteractionHandler) {
        this.transactionStatusInteractionHandler = transactionStatusInteractionHandler;
    }

    public ArrayList<ExternalEvent> getExternalEvents() {
        return externalEvents;
    }
}
