// infrastructure/driven-adapters/sqs-sender/src/main/java/co/com/bancolombia/sqs/sender/enums/QueueType.java
package co.com.bancolombia.sqs.sender.enums;

public enum QueueType {
    LOAN_CALCULATE_CAPACITY("loan-calculate-capacity-queue-url"),
    LOAN_NOTIFICATION_EMAIL("loan-notification-email-queue-url");

    private final String queueKey;

    QueueType(String queueKey) {
        this.queueKey = queueKey;
    }

    public String getQueueKey() {
        return queueKey;
    }
}