package hla.atm.utils.event;


import java.util.Comparator;

public class ExternalEvent {

    private Double time;
    private EventType eventType;

    // WithdrawalRequest
    private int amount;

    // TransactionStatus
    private boolean transactionSucceed;

    // QueueSizeStateResponse
    private boolean queueIsFull;

    // Other
    private int clientId;

    public ExternalEvent(EventType eventType, Double time) {
        this.time = time;
        this.eventType = eventType;
    }

    public ExternalEvent withQueueIsFull(boolean queueIsFull) {
        this.queueIsFull = queueIsFull;
        return this;
    }

    public ExternalEvent withAmount(int amount) {
        this.amount = amount;
        return this;
    }

    public ExternalEvent withClientId(int clientId) {
        this.clientId = clientId;
        return this;
    }

    public ExternalEvent withTransactionSucced(boolean transactionSucceed) {
        this.transactionSucceed = transactionSucceed;
        return this;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Double getTime() {
        return time;
    }

    public int getAmount() {
        return amount;
    }

    public int getClientId() {
        return clientId;
    }

    public boolean isQueueIsFull() {
        return queueIsFull;
    }

    public static class ExternalEventComparator implements Comparator<ExternalEvent> {
        @Override
        public int compare(ExternalEvent o1, ExternalEvent o2) {
            return o1.getTime().compareTo(o2.getTime());
        }
    }

}
