package hla.atm.queue;

import static hla.atm.commons.event.EventType.*;
import static hla.atm.utils.Utils.*;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;

import java.util.ArrayList;
import java.util.List;
import hla.atm.commons.AbstractFederate;
import hla.atm.config.Config;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

class QueueFederate extends AbstractFederate<QueueAmbassador> {

	private List<Integer> waitingClientIds = new ArrayList<>();

	QueueFederate() throws RTIinternalError {
		super(QUEUE_FEDERATE_NAME);
		this.setRTIambassador(RtiFactoryFactory.getRtiFactory().createRtiAmbassador());
		this.setFederateAmbassador(new QueueAmbassador());
	}

	void runFederate() throws RTIexception {
		prepareFederate();
		definePublishAndSubscribePolicy();

		while(getFederateAmbassador().isRunning()) {
			advanceTime();
			processEvents();
			getRTIambassador().tick();

		}
	}

	private void processEvents() {
		getFederateAmbassador().getExternalEvents().forEach(externalEvent -> {
			switch (externalEvent.getEventType()) {

				case ENTRY_REQUEST:
					Integer clientIdEntryRequest = externalEvent.getClientId();
					if (waitingClientIds.size() < Config.QUEUE_SIZE_LIMIT) {
						waitingClientIds.add(clientIdEntryRequest);
						sendClientEnteredInteraction(clientIdEntryRequest);
						sendQueueSizeStateResponseInteraction(false, clientIdEntryRequest);
					} else {
						sendQueueSizeStateResponseInteraction(true, clientIdEntryRequest);
					}
					break;

				case LEAVE_REQUEST:
					int clientIdLeaveRequest = externalEvent.getClientId();
					waitingClientIds.remove(new Integer(clientIdLeaveRequest));
					sendClientLeftInteraction(clientIdLeaveRequest);
					break;
			}
		});

		getFederateAmbassador().getExternalEvents().clear();
	}

	private void sendClientEnteredInteraction(int clientId) {
		try {
			SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
			int interactionHandle = getRTIambassador().getInteractionClassHandle("InteractionRoot." + CLIENT_ENTERED.name());
			int clientIdHandle = getRTIambassador().getParameterHandle("clientId", interactionHandle);
			parameters.add(clientIdHandle, EncodingHelpers.encodeInt(clientId));
			LogicalTime time = convertTime(getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateLookahead());
			log(getFederateName() + ": sending " + CLIENT_ENTERED.name() + " for clientId: " + clientId);
			getRTIambassador().sendInteraction(interactionHandle, parameters, INT_TAG.getBytes(), time);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendClientLeftInteraction(int clientId) {
		try {
			SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
			int interactionHandle = getRTIambassador().getInteractionClassHandle("InteractionRoot." + CLIENT_LEFT.name());
			int clientIdHandle = getRTIambassador().getParameterHandle("clientId", interactionHandle);
			parameters.add(clientIdHandle, EncodingHelpers.encodeInt(clientId));
			LogicalTime time = convertTime(getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateLookahead());
			log(getFederateName() + ": sending " + CLIENT_LEFT.name() + " for clientId: " + clientId);
			getRTIambassador().sendInteraction(interactionHandle, parameters, INT_TAG.getBytes(), time);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void sendQueueSizeStateResponseInteraction(boolean queueIsFull, int clientId) {
		try {
			SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
			int interactionHandle = getRTIambassador().getInteractionClassHandle("InteractionRoot." + QUEUE_SIZE_STATE_RESPONSE.name());
			int queueIsFullHandle = getRTIambassador().getParameterHandle("queueIsFull", interactionHandle);
			int clientIdHandle = getRTIambassador().getParameterHandle("clientId", interactionHandle);
			parameters.add(queueIsFullHandle, EncodingHelpers.encodeBoolean(queueIsFull));
			parameters.add(clientIdHandle, EncodingHelpers.encodeInt(clientId));
			LogicalTime time = convertTime(getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateLookahead());
			log(getFederateName() + ": sending " + QUEUE_SIZE_STATE_RESPONSE.name() + " for queueIsFull " + queueIsFull + ", clientId: " + clientId);
			getRTIambassador().sendInteraction(interactionHandle, parameters, INT_TAG.getBytes(), time);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void definePublishAndSubscribePolicy() throws RTIexception {
		publishInteraction(QUEUE_SIZE_STATE_RESPONSE.name());
		publishInteraction(CLIENT_ENTERED.name());
		publishInteraction(CLIENT_LEFT.name());
		getFederateAmbassador().setEntryRequestInteractionHandler(subscribeInteraction(ENTRY_REQUEST.name()));
		getFederateAmbassador().setLeaveRequestInteractionHandler(subscribeInteraction(LEAVE_REQUEST.name()));
	}


}
