package hla.atm.gui;

import static hla.atm.commons.event.EventType.*;
import static hla.atm.utils.Utils.*;

import hla.atm.commons.AbstractFederate;
import hla.atm.gui.visualization.GUI;
import hla.rti.*;
import hla.rti.jlc.RtiFactoryFactory;

class GUIFederate extends AbstractFederate<GUIAmbassador> {

	private GUI gui;

	GUIFederate() throws RTIinternalError {
		super(GUI_FEDERATE_NAME);
		gui = new GUI();
		this.setRTIambassador(RtiFactoryFactory.getRtiFactory().createRtiAmbassador());
		this.setFederateAmbassador(new GUIAmbassador());
	}

	void runFederate() throws RTIexception {
		Thread guiThread = new Thread(gui);
		guiThread.start();
		prepareFederate();
		definePublishAndSubscribePolicy();

		while(getFederateAmbassador().isRunning()) {
			advanceTime();
			processEvents();
			getRTIambassador().tick();
		}
	}

	private void processEvents() {
		getFederateAmbassador().getExternalEvents().forEach( externalEvent -> {
			switch (externalEvent.getEventType()) {

				case CLIENT_ENTERED:
					gui.getClientIds().add(externalEvent.getClientId());
					gui.repaint();
					slowAnimation(500);
					break;

				case CLIENT_LEFT:
					gui.getClientIds().remove(Integer.valueOf(externalEvent.getClientId()));
					gui.repaint();
					slowAnimation(500);
					break;

				case NO_CASH:
					gui.setATMActive(false);
					gui.setServiceActive(true);
					gui.repaint();
					slowAnimation(800);
					break;

				case CASH_REFILL_REQUEST:
					gui.setATMActive(true);
					gui.setServiceActive(false);
					gui.repaint();
					break;
			}
		});
		getFederateAmbassador().getExternalEvents().clear();
	}

	private void definePublishAndSubscribePolicy() throws RTIexception {
		getFederateAmbassador().setClientEnteredInteractionHandler(subscribeInteraction(CLIENT_ENTERED.name()));
		getFederateAmbassador().setClientLeftInteractionHandler(subscribeInteraction(CLIENT_LEFT.name()));
		getFederateAmbassador().setNoCashInteractionHandler(subscribeInteraction(NO_CASH.name()));
		getFederateAmbassador().setCashRefillRequestInteractionHandler(subscribeInteraction(CASH_REFILL_REQUEST.name()));
	}

	private void slowAnimation(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
