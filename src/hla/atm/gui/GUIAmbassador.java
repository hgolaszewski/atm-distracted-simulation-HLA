package hla.atm.gui;

import static hla.atm.utils.Utils.GUI_AMBASSADOR_NAME;
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

class GUIAmbassador extends AbstractAmbassador {

	private int clientEnteredInteractionHandler = 0;
	private int clientLeftInteractionHandler = 0;
	private int noCashInteractionHandler = 0;
	private int cashRefillRequestInteractionHandler = 0;

	GUIAmbassador() {
		super(GUI_AMBASSADOR_NAME);
	}

	@Override
	public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
		receiveInteraction(interactionClass, theInteraction, tag, null, null);
	}

	public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
								   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {

		StringBuilder messageToLog = new StringBuilder(getAmbassadorName() + ": Interaction Received: ");

		try {
			// CLIENT_ENTERED INTERACTION
			if(interactionClass == clientEnteredInteractionHandler) {
				int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
				double time = convertTime(theTime);
				ExternalEvent externalEvent = new ExternalEvent(CLIENT_ENTERED, time)
						.withClientId(clientId);
				getExternalEvents().add(externalEvent);
				messageToLog.append(CLIENT_ENTERED.name())
						.append(", time = ")
						.append(time)
						.append(" clientId = ")
						.append(clientId)
						.append("\n");
				// CLIENT_LEFT INTERACTION
			} else if (interactionClass == clientLeftInteractionHandler) {
				int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
				double time =  convertTime(theTime);
				ExternalEvent externalEvent = new ExternalEvent(CLIENT_LEFT, time)
						.withClientId(clientId);
				getExternalEvents().add(externalEvent);
				messageToLog.append(CLIENT_LEFT.name())
						.append("ClientLeft, time = ")
						.append(time)
						.append(" clientId = ")
						.append(clientId)
						.append("\n");
				// NO_CASH INTERACTION
			} else if (interactionClass == noCashInteractionHandler) {
				double time =  convertTime(theTime);
				ExternalEvent externalEvent = new ExternalEvent(NO_CASH, time);
				getExternalEvents().add(externalEvent);
				messageToLog.append(NO_CASH.name())
						.append(", time = ")
						.append(time)
						.append("\n");
				// CASH_REFILL_REQUEST INTERACTION
			} else if (interactionClass == cashRefillRequestInteractionHandler) {
				double time =  convertTime(theTime);
				ExternalEvent externalEvent = new ExternalEvent(CASH_REFILL_REQUEST, time);
				getExternalEvents().add(externalEvent);
				messageToLog.append(CASH_REFILL_REQUEST.name())
						.append(", time = ")
						.append(time)
						.append("\n");
			}
		} catch (ArrayIndexOutOfBounds e) {
			e.printStackTrace();
		}
		log(messageToLog.toString());
	}

	void setClientEnteredInteractionHandler(int clientEnteredInteractionHandler) {
		this.clientEnteredInteractionHandler = clientEnteredInteractionHandler;
	}

	void setClientLeftInteractionHandler(int clientLeftInteractionHandler) {
		this.clientLeftInteractionHandler = clientLeftInteractionHandler;
	}

	void setNoCashInteractionHandler(int noCashInteractionHandler) {
		this.noCashInteractionHandler = noCashInteractionHandler;
	}

	void setCashRefillRequestInteractionHandler(int cashRefillRequestInteractionHandler) {
		this.cashRefillRequestInteractionHandler = cashRefillRequestInteractionHandler;
	}

}
