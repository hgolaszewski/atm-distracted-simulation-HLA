package hla.atm.queue;

import static hla.atm.utils.Utils.SYNC_POINT;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;
import static hla.atm.utils.event.EventType.ENTRY_REQUEST;
import static hla.atm.utils.event.EventType.LEAVE_REQUEST;

import java.util.ArrayList;

import hla.atm.utils.event.ExternalEvent;
import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;

public class QueueAmbassador extends NullFederateAmbassador {

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
	private int entryRequestInteractionHandler = 0;
	private int leaveRequestInteractionHandler = 0;

	// Received events
	private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

	public void synchronizationPointRegistrationSucceeded(String label){
		log("QueueFederateAmbassador: successfully registered sync point: " + label);
	}

	public void announceSynchronizationPoint(String label, byte[] tag) {
		log("QueueFederateAmbassador: synchronization point announced: " + label);
		if(label.equals(SYNC_POINT)) {
			this.isAnnounced = true;
		}
	}

	public void federationSynchronized(String label) {
		log("QueueFederateAmbassador: federation synchronized: " + label);
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
		log("QueueFederateAmbassador: failed to register sync point: " + label);
	}

	@Override
	public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
		receiveInteraction(interactionClass, theInteraction, tag, null, null);
	}

	public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
								   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {

		StringBuilder messageToLog = new StringBuilder("QueueFederateAmbassador: Interaction Received: ");

		// ENTRY_REQUEST INTERACTION
		if(interactionClass == entryRequestInteractionHandler) {
			try {
				int clientId = EncodingHelpers.decodeInt(theInteraction.getValue(0));
				double time =  convertTime(theTime);
				ExternalEvent externalEvent = new ExternalEvent(ENTRY_REQUEST, time).withClientId(clientId);
				externalEvents.add(externalEvent);
				messageToLog.append("EntryRequest, time = ")
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
				ExternalEvent externalEvent = new ExternalEvent(LEAVE_REQUEST, time).withClientId(clientId);
				externalEvents.add(externalEvent);
				messageToLog.append("LeaveRequest, time = ")
						.append(time)
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

	public void setEntryRequestInteractionHandler(int entryRequestInteractionHandler) {
		this.entryRequestInteractionHandler = entryRequestInteractionHandler;
	}

	public void setLeaveRequestInteractionHandler(int leaveRequestInteractionHandler) {
		this.leaveRequestInteractionHandler = leaveRequestInteractionHandler;
	}

}
