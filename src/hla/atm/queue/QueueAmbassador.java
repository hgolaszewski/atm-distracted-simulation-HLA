package hla.atm.queue;

import static hla.atm.utils.Utils.QUEUE_AMBASSADOR_NAME;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;
import static hla.atm.commons.event.EventType.ENTRY_REQUEST;
import static hla.atm.commons.event.EventType.LEAVE_REQUEST;

import hla.atm.commons.AbstractAmbassador;
import hla.atm.commons.event.ExternalEvent;
import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.EncodingHelpers;

class QueueAmbassador extends AbstractAmbassador {

	private int entryRequestInteractionHandler = 0;
	private int leaveRequestInteractionHandler = 0;

	QueueAmbassador() {
		super(QUEUE_AMBASSADOR_NAME);
	}

	@Override
	public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
		receiveInteraction(interactionClass, theInteraction, tag, null, null);
	}

	public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
								   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {

		StringBuilder messageToLog = new StringBuilder(getAmbassadorName() + ": Interaction Received: ");

		// ENTRY_REQUEST INTERACTION
		if(interactionClass == entryRequestInteractionHandler) {
			try {
				int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
				double time =  convertTime(theTime);
				ExternalEvent externalEvent = new ExternalEvent(ENTRY_REQUEST, time)
						.withClientId(clientId);
				getExternalEvents().add(externalEvent);
				messageToLog.append(ENTRY_REQUEST.name())
						.append(", time = ")
						.append(time)
						.append(" clientId = ")
						.append(clientId)
						.append("\n");
			} catch (ArrayIndexOutOfBounds ignored) {}
		// LEAVE_REQUEST INTERACTION
		} else if (interactionClass == leaveRequestInteractionHandler) {
			try {
				int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
				double time =  convertTime(theTime);
				ExternalEvent externalEvent = new ExternalEvent(LEAVE_REQUEST, time)
						.withClientId(clientId);
				getExternalEvents().add(externalEvent);
				messageToLog.append(LEAVE_REQUEST.name())
						.append(", time = ")
						.append(time)
						.append(" clientId = ")
						.append(clientId)
						.append("\n");
			} catch (ArrayIndexOutOfBounds ignored) {}
		}
		log(messageToLog.toString());
	}

	void setEntryRequestInteractionHandler(int entryRequestInteractionHandler) {
		this.entryRequestInteractionHandler = entryRequestInteractionHandler;
	}

	void setLeaveRequestInteractionHandler(int leaveRequestInteractionHandler) {
		this.leaveRequestInteractionHandler = leaveRequestInteractionHandler;
	}

}
