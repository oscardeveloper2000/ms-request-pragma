// infrastructure/driven-adapters/sqs-sender/src/main/java/co/com/bancolombia/sqs/sender/enums/QueueType.java
package co.com.bancolombia.sqs.listener.enums;

public enum QueueTypeListener {
    LOAN_VALIDATION_RESULTS("loan-validation-results-queue-url");

    private final String queueKey;

    QueueTypeListener(String queueKey) {
        this.queueKey = queueKey;
    }

    public String getQueueKey() {
        return queueKey;
    }
}