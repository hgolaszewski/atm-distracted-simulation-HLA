package hla.atm.queue;

import static hla.atm.utils.Utils.*;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import hla.atm.utils.config.Config;
import hla.atm.utils.event.ExternalEvent;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

public class QueueFederate {

	private List<Integer> clientsWaiting = new ArrayList<>();
	private RTIambassador rtiamb;
	private QueueAmbassador fedamb;

	void runFederate() throws RTIexception {
		rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
		fedamb = new QueueAmbassador();
		tryCreateFederation();
		tryJoinFederationAndRegisterSyncPoint();
		waitForSynchronization("QueueFederate");

		rtiamb.synchronizationPointAchieved(SYNC_POINT);
		log("QueueFederate: achieved sync point: " + SYNC_POINT + ", waiting for federation...");

		while(!fedamb.isReadyToRun) {
			rtiamb.tick();
		}

		enableTimePolicy();
		definePublishAndSubscribePolicy();

		while(fedamb.running) {
			advanceTime();

			for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
				switch (externalEvent.getEventType()) {

					case ENTRY_REQUEST:
						Integer clientIdEntryRequest = externalEvent.getClientId();
						if (clientsWaiting.size() < Config.QUEUE_SIZE_LIMIT) {
							clientsWaiting.add(clientIdEntryRequest);
							sendClientEnteredInteraction(clientIdEntryRequest);
							sendQueueSizeStateResponseInteraction(false, clientIdEntryRequest);
						} else {
							sendQueueSizeStateResponseInteraction(true, clientIdEntryRequest);
						}
						break;

					case LEAVE_REQUEST:
						int clientIdLeaveRequest = externalEvent.getClientId();
						clientsWaiting.remove(new Integer(clientIdLeaveRequest));
						sendClientLeftInteraction(clientIdLeaveRequest);
						break;
				}
			}

			fedamb.getExternalEvents().clear();
			rtiamb.tick();

		}
	}

	// Sending CLIENT_ENTERED interaction
	private void sendClientEnteredInteraction(int clientId) throws RTIinternalError, NameNotFound, FederateNotExecutionMember, InteractionClassNotDefined, RestoreInProgress, InteractionClassNotPublished, SaveInProgress, InvalidFederationTime, ConcurrentAccessAttempted, InteractionParameterNotDefined {
		SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

		int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.ClientEntered");
		int clientIdHandle = rtiamb.getParameterHandle("clientId", interactionHandle);

		parameters.add(clientIdHandle, EncodingHelpers.encodeInt(clientId));

		LogicalTime time = convertTime(fedamb.federateTime + fedamb.federateLookahead);
		log("QueueFederate: sending CLIENT_ENTERED: " + clientId);
		rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
	}

	// Sending CLIENT_LEFT
	private void sendClientLeftInteraction(int clientId) throws RTIinternalError, NameNotFound, FederateNotExecutionMember, InteractionClassNotDefined, RestoreInProgress, InteractionClassNotPublished, SaveInProgress, InvalidFederationTime, ConcurrentAccessAttempted, InteractionParameterNotDefined {
		SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

		int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.ClientLeft");
		int clientIdHandle = rtiamb.getParameterHandle("clientId", interactionHandle);

		parameters.add(clientIdHandle, EncodingHelpers.encodeInt(clientId));

		LogicalTime time = convertTime(fedamb.federateTime + fedamb.federateLookahead);
		log("QueueFederate: sending ClientLeft: " + clientId);
		rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
	}

	// Sending QUEUE_SIZE_STATE_RESPONSE interaction
	private void sendQueueSizeStateResponseInteraction(boolean queueIsFull, int clientId) throws
			RTIinternalError, NameNotFound, FederateNotExecutionMember, InteractionClassNotDefined, RestoreInProgress, InteractionClassNotPublished, SaveInProgress, InvalidFederationTime, ConcurrentAccessAttempted, InteractionParameterNotDefined {
		SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

		int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.QueueSizeStateResponse");
		int queueIsFullHandle = rtiamb.getParameterHandle("queueIsFull", interactionHandle);
		int clientIdHandle = rtiamb.getParameterHandle("clientId", interactionHandle);

		parameters.add(queueIsFullHandle, EncodingHelpers.encodeBoolean(queueIsFull));
		parameters.add(clientIdHandle, EncodingHelpers.encodeInt(clientId));

		LogicalTime time = convertTime(fedamb.federateTime + fedamb.federateLookahead);
		log("QueueFederate: sending QueueSizeStateResponse: " + queueIsFull + " for client: " + clientId);
		rtiamb.sendInteraction(interactionHandle, parameters, "tag".getBytes(), time);
	}
	private void tryJoinFederationAndRegisterSyncPoint() throws FederateAlreadyExecutionMember, FederationExecutionDoesNotExist, SaveInProgress, RestoreInProgress, RTIinternalError, ConcurrentAccessAttempted, FederateNotExecutionMember {
		rtiamb.joinFederationExecution("QueueFederate" , "ATMFederation", fedamb);
		log("QueueFederate: joined Federation as QueueFederate");

		rtiamb.registerFederationSynchronizationPoint(SYNC_POINT, null);

		while(!fedamb.isAnnounced) {
			rtiamb.tick();
		}
	}

	private void tryCreateFederation() throws CouldNotOpenFED, ErrorReadingFED, RTIinternalError, ConcurrentAccessAttempted {
		try {
			File fom = new File("atmfederation.fed");
			rtiamb.createFederationExecution("ATMFederation", fom.toURI().toURL());
			log("QueueFederate: created Federation");
		} catch(FederationExecutionAlreadyExists exists) {
			log("QueueFederate: didn't create federation, it already existed");
		} catch(MalformedURLException url) {
			log("QueueFederate: exception processing fom: " + url.getMessage());
			url.printStackTrace();
		}
	}

	private void enableTimePolicy() throws RTIexception {
		LogicalTime currentTime = convertTime(fedamb.federateTime);
		LogicalTimeInterval lookahead = convertInterval(fedamb.federateLookahead);
		this.rtiamb.enableTimeRegulation(currentTime, lookahead);
		while(!fedamb.isRegulating) {
			rtiamb.tick();
		}
		this.rtiamb.enableTimeConstrained();
		while(!fedamb.isConstrained) {
			rtiamb.tick();
		}
	}

	private void definePublishAndSubscribePolicy() throws RTIexception {
		publishInteraction("QueueSizeStateResponse");
		publishInteraction("ClientEntered");
		publishInteraction("ClientLeft");
		fedamb.setEntryRequestInteractionHandler(subscribeInteraction("EntryRequest"));
		fedamb.setLeaveRequestInteractionHandler(subscribeInteraction("LeaveRequest"));
	}

	private void advanceTime() throws RTIexception {
		fedamb.isAdvancing = true;
		LogicalTime newTime = convertTime(fedamb.federateTime + fedamb.federateStep);
		rtiamb.timeAdvanceRequest(newTime);
		while (fedamb.isAdvancing) {
			rtiamb.tick();
		}
	}

	private void publishInteraction(String interactionName) throws RTIexception {
		int handle = rtiamb.getInteractionClassHandle("InteractionRoot." + interactionName);
		rtiamb.publishInteractionClass(handle);
		log("QueueFederate: published interaction " + interactionName + " (" + handle + ")");
	}

	private int subscribeInteraction(String interactionName) throws RTIexception {
		int handle = rtiamb.getInteractionClassHandle("InteractionRoot." + interactionName);
		rtiamb.subscribeInteractionClass(handle);
		log("QueueFederate: subscribed to interaction " + interactionName);
		return handle;
	}

}
