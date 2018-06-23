package hla.atm.service;

import static hla.atm.utils.Utils.SERVICE_AMBASSADOR_NAME;
import static hla.atm.utils.Utils.convertTime;
import static hla.atm.utils.Utils.log;
import static hla.atm.commons.event.EventType.*;

import hla.atm.commons.AbstractAmbassador;
import hla.atm.commons.event.ExternalEvent;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;

class ServiceAmbassador extends AbstractAmbassador {

    private int noCashInteractionHandler = 0;

    ServiceAmbassador() {
        super(SERVICE_AMBASSADOR_NAME);
    }

    @Override
    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag) {
        receiveInteraction(interactionClass, theInteraction, tag, null, null);
    }

    public void receiveInteraction(int interactionClass, ReceivedInteraction theInteraction, byte[] tag,
                                   LogicalTime theTime, EventRetractionHandle eventRetractionHandle) {

        StringBuilder messageToLog = new StringBuilder(getAmbassadorName() + ": Interaction Received: ");

        // NO_CASH INTERACTION
        if(interactionClass == noCashInteractionHandler) {
            double time = convertTime(theTime);
            ExternalEvent externalEvent = new ExternalEvent(NO_CASH, time);
            getExternalEvents().add(externalEvent);
            messageToLog.append(ENTRY_REQUEST.name())
                    .append(", time = ")
                    .append(time)
                    .append("\n");
        }
        log(messageToLog.toString());
    }

    void setNoCashInteractionHandler(int noCashInteractionHandler) {
        this.noCashInteractionHandler = noCashInteractionHandler;
    }

}
