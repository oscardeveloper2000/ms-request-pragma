//package co.com.bancolombia.sqs.listener.helper;
//
//import co.com.bancolombia.sqs.listener.SQSProcessor;
//import co.com.bancolombia.sqs.listener.config.SQSProperties;
//import co.com.bancolombia.usecase.processcapacityvalidation.ProcessCapacityValidationUseCase;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.mock;
//
//class SQSListenerTest {
//
//    @Mock
//    private ProcessCapacityValidationUseCase processCapacityValidationUseCase;
//
//    private SQSProcessor sqsProcessor;
//    private SQSProperties sqsProperties;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//
//        sqsProperties = new SQSProperties(
//            "test-queue-url",
//            "us-east-1",
//            Map.of("key1", "value1", "key2", "value2"),
//            "test-access-key",
//            10, 20, 30, 40
//        );
//
//        sqsProcessor = new SQSProcessor(processCapacityValidationUseCase);
//    }
//
//    @Test
//    void shouldCreateSQSListenerSuccessfully() {
//        ProcessCapacityValidationUseCase mockUseCase = mock(ProcessCapacityValidationUseCase.class);
//
//        var sqsListener = SQSListener.builder()
//                .properties(sqsProperties)
//                .processor(new SQSProcessor(mockUseCase))
//                .build();
//
//        assertNotNull(sqsListener);
//        assertNotNull(sqsListener.getProperties());
//        assertNotNull(sqsListener.getProcessor());
//    }
//
//    @Test
//    void shouldCreateSQSPropertiesWithCorrectValues() {
//        var properties = new SQSProperties(
//            "test-queue-url",
//            "us-east-1",
//            Map.of("key1", "value1", "key2", "value2"),
//            "test-access-key",
//            10, 20, 30, 40
//        );
//
//        assertNotNull(properties);
//        assertEquals("test-queue-url", properties.queueUrl());
//        assertEquals("us-east-1", properties.region());
//        assertEquals("test-access-key", properties.accessKey());
//        assertEquals(10, properties.maxConcurrentMessages());
//        assertEquals(20, properties.messageRetentionPeriod());
//        assertEquals(30, properties.visibilityTimeout());
//        assertEquals(40, properties.waitTimeSeconds());
//    }
//
//    @Test
//    void shouldCreateSQSPropertiesWithEmptyMap() {
//        var properties = new SQSProperties(
//            "queue-url",
//            "region",
//            Map.of(),
//            "access-key",
//            5, 10, 15, 20
//        );
//
//        assertNotNull(properties);
//        assertNotNull(properties.headers());
//        assertEquals(0, properties.headers().size());
//    }
//
//    @Test
//    void shouldCreateSQSProcessorWithUseCase() {
//        ProcessCapacityValidationUseCase mockUseCase = mock(ProcessCapacityValidationUseCase.class);
//        var processor = new SQSProcessor(mockUseCase);
//
//        assertNotNull(processor);
//    }
//
//    @Test
//    void shouldCreateSQSProcessorWithMockedUseCase() {
//        var processor = new SQSProcessor(processCapacityValidationUseCase);
//
//        assertNotNull(processor);
//    }
//
//    @Test
//    void shouldBuildSQSListenerWithAllComponents() {
//        var listener = SQSListener.builder()
//                .properties(sqsProperties)
//                .processor(sqsProcessor)
//                .build();
//
//        assertNotNull(listener);
//        assertEquals(sqsProperties, listener.getProperties());
//        assertEquals(sqsProcessor, listener.getProcessor());
//    }
//}