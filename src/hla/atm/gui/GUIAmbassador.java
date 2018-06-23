package hla.atm.gui;

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

public class GUIAmbassador extends NullFederateAmbassador {

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
	private int clientEnteredInteractionHandler = 0;
	private int clientLeftInteractionHandler = 0;
	private int noCashInteractionHandler = 0;
	private int cashRefillRequestInteractionHandler = 0;

	// Received events
	private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

	public void synchronizationPointRegistrationSucceeded(String label){
		log("GUIFederateAmbassador: successfully registered sync point: " + label);
	}

	public void announceSynchronizationPoint(String label, byte[] tag) {
		log("GUIFederateAmbassador: synchronization point announced: " + label);
		if(label.equals(SYNC_POINT)) {
			this.isAnnounced = true;
		}
	}

	public void federationSynchronized(String label) {
		log("GUIFederateAmbassador: federation synchronized: " + label);
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
		log("GUIFederateAmbassador: failed to register sync point: " + label);
	}

	@Override
	public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
		receiveInteraction(interactionClass, theInteraction, tag, null, null);
	}

	public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
								   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {

		StringBuilder messageToLog = new StringBuilder("GUIFederateAmbassador: Interaction Received: ");

		// CLIENT_ENTERED INTERACTION
		if(interactionClass == clientEnteredInteractionHandler) {
			try {
   				int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
 				double time =  convertTime(theTime);
				ExternalEvent externalEvent = new ExternalEvent(CLIENT_ENTERED, time).withClientId(clientId);
				externalEvents.add(externalEvent);
				messageToLog.append("ClientEntered, time = ")
						.append(time)
						.append(" clientId = ")
						.append(clientId)
						.append("\n");
			} catch (ArrayIndexOutOfBounds ignored) {}
			// CLIENT_LEFT INTERACTION
		} else if (interactionClass == clientLeftInteractionHandler) {
			try {
				int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
				double time =  convertTime(theTime);
				ExternalEvent externalEvent = new ExternalEvent(CLIENT_LEFT, time).withClientId(clientId);
				externalEvents.add(externalEvent);
				messageToLog.append("ClientLeft, time = ")
						.append(time)
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
			// CASH_REFILL_REQUEST INTERACTION
		} else if (interactionClass == cashRefillRequestInteractionHandler) {
			double time =  convertTime(theTime);
			ExternalEvent externalEvent = new ExternalEvent(CASH_REFILL_REQUEST, time);
			externalEvents.add(externalEvent);
			messageToLog.append("CashRefillRequest, time = ")
					.append(time)
					.append("\n");
		}
		log(messageToLog.toString());
	}

	public ArrayList<ExternalEvent> getExternalEvents() {
		return externalEvents;
	}

	public void setClientEnteredInteractionHandler(int clientEnteredInteractionHandler) {
		this.clientEnteredInteractionHandler = clientEnteredInteractionHandler;
	}

	public void setClientLeftInteractionHandler(int clientLeftInteractionHandler) {
		this.clientLeftInteractionHandler = clientLeftInteractionHandler;
	}

	public void setNoCashInteractionHandler(int noCashInteractionHandler) {
		this.noCashInteractionHandler = noCashInteractionHandler;
	}

	public void setCashRefillRequestInteractionHandler(int cashRefillRequestInteractionHandler) {
		this.cashRefillRequestInteractionHandler = cashRefillRequestInteractionHandler;
	}
}
