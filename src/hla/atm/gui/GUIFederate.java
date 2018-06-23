package hla.atm.gui;

import static hla.atm.utils.Utils.*;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;

import java.io.File;
import java.net.MalformedURLException;

import hla.atm.gui.swing.GUI;
import hla.atm.utils.event.ExternalEvent;
import hla.rti.*;
import hla.rti.jlc.RtiFactoryFactory;

public class GUIFederate {

	private RTIambassador rtiamb;
	private GUIAmbassador fedamb;
	private GUI gui;

	void runFederate() throws RTIexception {
		rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
		fedamb = new GUIAmbassador();
		gui = new GUI();
		Thread guiThread = new Thread(gui);
		guiThread.start();
		tryCreateFederation();
		tryJoinFederationAndRegisterSyncPoint();
		waitForSynchronization("GUIFederate");

		rtiamb.synchronizationPointAchieved(SYNC_POINT);
		log("GUIFederate: achieved sync point: " + SYNC_POINT + ", waiting for federation...");

		while(!fedamb.isReadyToRun) {
			rtiamb.tick();
		}

		enableTimePolicy();
		definePublishAndSubscribePolicy();

		while(fedamb.running) {
			advanceTime();

			for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
				switch (externalEvent.getEventType()) {

					case CLIENT_ENTERED:
						gui.getClientIds().add(externalEvent.getClientId());
						gui.repaint();
						try {
							Thread.sleep(400);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						break;

					case CLIENT_LEFT:
						gui.getClientIds().remove(new Integer(externalEvent.getClientId()));
						gui.repaint();
						try {
							Thread.sleep(400);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						break;

					case NO_CASH:
						gui.setATMActive(false);
						gui.setServiceActive(true);
						gui.repaint();
						try {
							Thread.sleep(700);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						break;

					case CASH_REFILL_REQUEST:
						gui.setATMActive(true);
						gui.setServiceActive(false);
						gui.repaint();
						break;
				}
			}

			fedamb.getExternalEvents().clear();
			rtiamb.tick();

		}
	}

	private void tryJoinFederationAndRegisterSyncPoint() throws FederateAlreadyExecutionMember, FederationExecutionDoesNotExist, SaveInProgress, RestoreInProgress, RTIinternalError, ConcurrentAccessAttempted, FederateNotExecutionMember {
		rtiamb.joinFederationExecution("GUIFederate" , "ATMFederation", fedamb);
		log("GUIFederate: joined Federation as GUIFederate");

		rtiamb.registerFederationSynchronizationPoint(SYNC_POINT, null);

		while(!fedamb.isAnnounced) {
			rtiamb.tick();
		}
	}

	private void tryCreateFederation() throws CouldNotOpenFED, ErrorReadingFED, RTIinternalError, ConcurrentAccessAttempted {
		try {
			File fom = new File("atmfederation.fed");
			rtiamb.createFederationExecution("ATMFederation", fom.toURI().toURL());
			log("GUIFederate: created Federation");
		} catch(FederationExecutionAlreadyExists exists) {
			log("GUIFederate: didn't create federation, it already existed");
		} catch(MalformedURLException url) {
			log("GUIFederate: exception processing fom: " + url.getMessage());
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
		fedamb.setClientEnteredInteractionHandler(subscribeInteraction("ClientEntered"));
		fedamb.setClientLeftInteractionHandler(subscribeInteraction("ClientLeft"));
		fedamb.setNoCashInteractionHandler(subscribeInteraction("NoCash"));
		fedamb.setCashRefillRequestInteractionHandler(subscribeInteraction("CashRefillRequest"));
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
		log("GUIFederate: published interaction " + interactionName + " (" + handle + ")");
	}

	private int subscribeInteraction(String interactionName) throws RTIexception {
		int handle = rtiamb.getInteractionClassHandle("InteractionRoot." + interactionName);
		rtiamb.subscribeInteractionClass(handle);
		log("GUIFederate: subscribed to interaction " + interactionName);
		return handle;
	}

}
