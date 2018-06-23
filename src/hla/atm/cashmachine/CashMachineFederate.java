package hla.atm.cashmachine;

import static hla.atm.utils.Utils.*;
import static hla.atm.commons.event.EventType.*;

import hla.atm.commons.AbstractFederate;
import hla.atm.commons.event.ExternalEvent;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import java.util.ArrayList;
import java.util.List;

class CashMachineFederate extends AbstractFederate<CashMachineAmbassador> {

    private int cash = 35000;
    private boolean noCashSent = false;

	CashMachineFederate() throws RTIinternalError {
		super(CASH_MACHINE_FEDERATE_NAME);
		this.setRTIambassador(RtiFactoryFactory.getRtiFactory().createRtiAmbassador());
		this.setFederateAmbassador(new CashMachineAmbassador());
	}

	void runFederate() throws Exception {
        prepareFederate();
        declarePublishAndSubscribePolicy();
        while (getFederateAmbassador().isRunning()) {
            advanceTime();
            processEvents();
            getRTIambassador().tick();
        }
    }

    private void processEvents() {
        List<ExternalEvent> eventsToDelete = new ArrayList<>();
        getFederateAmbassador().getExternalEvents().forEach(externalEvent -> {
            switch (externalEvent.getEventType()) {

                case CASH_REFILL_REQUEST:
                    addCash(externalEvent.getAmount());
                    setNoCashSent(false);
                    eventsToDelete.add(externalEvent);
                    break;

                case WITHDRAWAL_REQUEST:
                    if (externalEvent.getAmount() > getCash()) {
                        if (!isNoCashSent()) {
                            sendNoCashInteraction();
                        }
                        setNoCashSent(true);
                    } else {
                        subtractCash(externalEvent.getAmount());
                        sendTransactionStatusInteraction(externalEvent.getClientId());
                        eventsToDelete.add(externalEvent);
                    }
                    break;
            }
        });
        getFederateAmbassador().getExternalEvents().removeAll(eventsToDelete);
    }

    private void addCash(int amountToAdd) {
        setCash(getCash() + amountToAdd);
        log(getFederateName() + ": refilled " + amountToAdd +
                " at time: " + getFederateAmbassador().getFederateTime() + ". Current cash: " + getCash());
    }

    private void subtractCash(int amountToGet) {
	    setCash(getCash() - amountToGet);
        log(getFederateName() + ": removed " + amountToGet +
                " at time: " + getFederateAmbassador().getFederateTime() + ". Current cash:" + getCash());
    }

    private void declarePublishAndSubscribePolicy() throws RTIexception {
        publishInteraction(TRANSACTION_STATUS.name());
        publishInteraction(NO_CASH.name());
		getFederateAmbassador().setWithdrawalRequestInteractionHandle(subscribeInteraction(WITHDRAWAL_REQUEST.name()));
		getFederateAmbassador().setCashRefillRequestInteractionHandle(subscribeInteraction(CASH_REFILL_REQUEST.name()));
    }

    private void sendNoCashInteraction() {
	    try {
            SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            int interactionHandle = getRTIambassador().getInteractionClassHandle("InteractionRoot." + NO_CASH.name());
            LogicalTime time = convertTime(getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateLookahead());
            log(getFederateName() + ": sending " + NO_CASH.name());
            getRTIambassador().sendInteraction(interactionHandle, parameters, INT_TAG.getBytes(), time);
        } catch (Exception e) {
	        e.printStackTrace();
        }
    }

    private void sendTransactionStatusInteraction(int clientId) {
	    try {
            SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            int interactionHandle = getRTIambassador().getInteractionClassHandle("InteractionRoot." + TRANSACTION_STATUS.name());
            int clientIdHandle = getRTIambassador().getParameterHandle("clientId", interactionHandle);
            parameters.add(clientIdHandle, EncodingHelpers.encodeInt(clientId));
            LogicalTime time = convertTime(getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateLookahead());
            log(getFederateName() + ": sending " + TRANSACTION_STATUS.name() + " for clientId: " + clientId);
            getRTIambassador().sendInteraction(interactionHandle, parameters, INT_TAG.getBytes(), time);
        } catch (Exception e) {
	        e.printStackTrace();
        }
    }

    private int getCash() {
        return cash;
    }

    private void setCash(int cash) {
        this.cash = cash;
    }

    private boolean isNoCashSent() {
        return noCashSent;
    }

    private void setNoCashSent(boolean noCashSent) {
        this.noCashSent = noCashSent;
    }

}
