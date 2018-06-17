package hla.atm.cashmachine;


import java.util.Comparator;

class ExternalEvent {

    public enum EventType {REFILL, WITHDRAW}

    private int amount;
    private Double time;
    private EventType eventType;

    ExternalEvent(int amount, EventType eventType, Double time) {
        this.amount = amount;
        this.time = time;
        this.eventType = eventType;
    }

    EventType getEventType() {
        return eventType;
    }

    int getAmount() {
        return amount;
    }

    Double getTime() {
        return time;
    }

    static class ExternalEventComparator implements Comparator<ExternalEvent> {
        @Override
        public int compare(ExternalEvent o1, ExternalEvent o2) {
            return o1.getTime().compareTo(o2.getTime());
        }
    }

}
