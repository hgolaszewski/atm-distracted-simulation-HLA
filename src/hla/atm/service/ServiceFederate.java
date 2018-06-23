package hla.atm.service;

import static hla.atm.commons.event.EventType.CASH_REFILL_REQUEST;
import static hla.atm.commons.event.EventType.NO_CASH;
import static hla.atm.utils.Utils.*;

import hla.atm.commons.AbstractFederate;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

class ServiceFederate extends AbstractFederate<ServiceAmbassador> {

    ServiceFederate() throws RTIinternalError {
        super(SERVICE_FEDERATE_NAME);
        this.setRTIambassador(RtiFactoryFactory.getRtiFactory().createRtiAmbassador());
        this.setFederateAmbassador(new ServiceAmbassador());
    }

    void runFederate() throws RTIexception{
        prepareFederate();
        declarePublishAndSubscribePolicy();

        while (getFederateAmbassador().isRunning()) {
            advanceTime();
            processEvents();
            getRTIambassador().tick();
        }

    }

    private void processEvents() {
        getFederateAmbassador().getExternalEvents().forEach(externalEvent ->  {
            switch (externalEvent.getEventType()) {

                case NO_CASH:
                    int cashToRefill = generateRandomInt(20000, 25000);
                    sendCashRefillRequestInteraction(cashToRefill);
                    break;
            }
        });
        getFederateAmbassador().getExternalEvents().clear();
    }

    private void sendCashRefillRequestInteraction(int amount) {
        try {
            SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            int interactionHandle = getRTIambassador().getInteractionClassHandle("InteractionRoot." + CASH_REFILL_REQUEST.name());
            int amountHandle = getRTIambassador().getParameterHandle("amount", interactionHandle);
            parameters.add(amountHandle, EncodingHelpers.encodeInt(amount));
            LogicalTime time = convertTime(getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateLookahead());
            log(getFederateName() + ": sending " + CASH_REFILL_REQUEST.name() + " for amount: " + amount);
            getRTIambassador().sendInteraction(interactionHandle, parameters, INT_TAG.getBytes(), time);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void declarePublishAndSubscribePolicy() throws RTIexception {
        publishInteraction(CASH_REFILL_REQUEST.name());
        getFederateAmbassador().setNoCashInteractionHandler(subscribeInteraction(NO_CASH.name()));
    }

}
