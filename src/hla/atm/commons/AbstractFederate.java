package hla.atm.commons;

import static hla.atm.utils.Utils.*;
import static hla.atm.config.Config.FEDERATION_FOM_FILE;
import static hla.atm.config.Config.FEDERATION_NAME;
import static hla.atm.config.Config.SYNC_POINT;

import java.io.File;
import java.net.MalformedURLException;

import hla.rti.*;

public abstract class AbstractFederate<T extends AbstractAmbassador> {

	private RTIambassador RTIambassador;
	private T federateAmbassador;
	private String federateName;

	public AbstractFederate(String federateName) {
		this.federateName = federateName;
	}

	protected void publishInteraction(String interactionName) throws RTIexception {
		int handle = RTIambassador.getInteractionClassHandle("InteractionRoot." + interactionName);
		RTIambassador.publishInteractionClass(handle);
		log(federateName + ": published interaction " + interactionName);
	}

	protected int subscribeInteraction(String interactionName) throws RTIexception {
		int handle = RTIambassador.getInteractionClassHandle("InteractionRoot." + interactionName);
		RTIambassador.subscribeInteractionClass(handle);
		log(federateName + ": subscribed to interaction " + interactionName);
		return handle;
	}

	protected void prepareFederate() throws RTIexception {
		tryCreateFederation();
		tryJoinFederationAndRegisterSyncPoint();
		waitForSynchronization(getFederateName());
		getRTIambassador().synchronizationPointAchieved(SYNC_POINT);
		log(getFederateName() + ": achieved sync point: " + SYNC_POINT + ", waiting for federation...");
		while(!getFederateAmbassador().isReadyToRun()) {
			getRTIambassador().tick();
		}
		enableTimePolicy();
	}

	protected void advanceTime() throws RTIexception {
		getFederateAmbassador().setAdvancing(true);
		LogicalTime newTime = convertTime(getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateStep());
		getRTIambassador().timeAdvanceRequest(newTime);
		while (getFederateAmbassador().isAdvancing()) {
			getRTIambassador().tick();
		}
	}

	private void tryJoinFederationAndRegisterSyncPoint() throws FederateAlreadyExecutionMember, FederationExecutionDoesNotExist, SaveInProgress, RestoreInProgress, RTIinternalError, ConcurrentAccessAttempted, FederateNotExecutionMember {
		getRTIambassador().joinFederationExecution(getFederateName(), FEDERATION_NAME, getFederateAmbassador());
		log(getFederateName() + " joined Federation!");
		getRTIambassador().registerFederationSynchronizationPoint(SYNC_POINT, null);
		while(!getFederateAmbassador().isAnnounced()) {
			getRTIambassador().tick();
		}
	}

	private void tryCreateFederation() throws CouldNotOpenFED, ErrorReadingFED,
			RTIinternalError, ConcurrentAccessAttempted {
		try {
			File fom = new File(FEDERATION_FOM_FILE);
			getRTIambassador().createFederationExecution(FEDERATION_NAME, fom.toURI().toURL());
			log(getFederateName() + ": created Federation");
		} catch(FederationExecutionAlreadyExists exists) {
			log(getFederateName() + ": didn't create federation, it already existed");
		} catch(MalformedURLException url) {
			log(getFederateName() + ": exception processing fom: " + url.getMessage());
			url.printStackTrace();
		}
	}

	private void enableTimePolicy() throws RTIexception {
		LogicalTime currentTime = convertTime(getFederateAmbassador().getFederateTime());
		LogicalTimeInterval lookahead = convertInterval(getFederateAmbassador().getFederateLookahead());
		getRTIambassador().enableTimeRegulation(currentTime, lookahead);
		while(!getFederateAmbassador().isRegulating()) {
			getRTIambassador().tick();
		}
		getRTIambassador().enableTimeConstrained();
		while(!getFederateAmbassador().isConstrained()) {
			getRTIambassador().tick();
		}
	}

	protected T getFederateAmbassador() {
		return federateAmbassador;
	}

	protected void setFederateAmbassador(T federateAmbassador) {
		this.federateAmbassador = federateAmbassador;
	}

	protected RTIambassador getRTIambassador() {
		return RTIambassador;
	}

	protected void setRTIambassador(RTIambassador RTIambassador) {
		this.RTIambassador = RTIambassador;
	}

	protected String getFederateName() {
		return federateName;
	}

	protected void setFederateName(String federateName) {
		this.federateName = federateName;
	}
}
