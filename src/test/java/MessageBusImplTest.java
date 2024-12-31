import bgu.spl.mics.*;
import bgu.spl.mics.example.messages.ExampleBroadcast;
import bgu.spl.mics.example.messages.ExampleEvent;
import bgu.spl.mics.example.services.ExampleBroadcastListenerService;
import bgu.spl.mics.example.services.ExampleEventHandlerService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MessageBusImplTest {

    @Test
    void testRegisterBroadcastSubscription() {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        MicroService listener = new ExampleBroadcastListenerService("Listener", new String[]{"5"});
        
        // Registration
        messageBus.register(listener);
        assertTrue(messageBus.isRegistered(listener), 
            "Expected: Listener is registered. Actual: Listener is not registered.");
    
        // Duplicate Registration 
        messageBus.register(listener);
        assertEquals(1, messageBus.getNumberOfRegisters(), 
            "Expected: Listener should only be registered once. Actual: Listener is registered more than once.");
    
        // Subscribe to Broadcast
        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener);
        assertTrue(messageBus.isSubscribedToBroad(ExampleBroadcast.class, listener), 
            "Expected: Listener is subscribed to ExampleBroadcast. Actual: Listener is not subscribed.");
    
        // Duplicate subscription and Verify that the listener is still subscribed only once to the Broadcast
        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener);
        assertEquals(1, messageBus.getNumberOfSubscribersToBroad(ExampleBroadcast.class), 
            "Expected: Listener should be subscribed only once to ExampleBroadcast. Actual: Listener is subscribed more than once.");
    
        // Cleanup
        messageBus.unregister(listener);
        assertFalse(messageBus.isRegistered(listener), 
            "Expected: Listener is no longer registered after unregister. Actual: Listener is still registered.");
        assertFalse(messageBus.isSubscribedToBroad(ExampleBroadcast.class, listener), 
            "Expected: Listener is no longer subscribed to ExampleBroadcast after unregister. Actual: Listener is still subscribed.");
    }
    
    @Test
    void testRegisterEventSubscription() {        
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        MicroService Handler = new ExampleEventHandlerService("Handler", new String[]{"5"});

        // Registration
        messageBus.register(Handler);
        assertTrue(messageBus.isRegistered(Handler), 
            "Expected: Handler is registered. Actual: Handler is not registered.");

        // Duplicate Registration 
        messageBus.register(Handler);
        assertEquals(1, messageBus.getNumberOfRegisters(), 
             "Expected: Handler should only be registered once. Actual: Handler is registered more than once.");

        // Subscribe to Event
        messageBus.subscribeEvent(ExampleEvent.class, Handler);
        assertTrue(messageBus.isSubscribedToEvent(ExampleEvent.class, Handler), 
            "Expected: Handler is subscribed to ExampleEvent. Actual: Handler is not subscribed.");

        // Duplicate subscription and Verify that the Handler is still subscribed only once to the event
        messageBus.subscribeEvent(ExampleEvent.class, Handler);
        assertEquals(1, messageBus.getNumberOfSubscribersToEvent(ExampleEvent.class), 
            "Expected: Handler should be subscribed only once to ExampleEvent. Actual: Handler is subscribed more than once.");

        // Cleanup
        messageBus.unregister(Handler);
        assertFalse(messageBus.isRegistered(Handler), 
            "Expected: Handler is no longer registered after unregister. Actual: Handler is still registered.");
        assertFalse(messageBus.isSubscribedToEvent(ExampleEvent.class, Handler), 
            "Expected: Handler is no longer subscribed to ExampleEvent after unregister. Actual: Handler is still subscribed.");
    }

    @Test
    void testSendBroadcast() {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        MicroService listener1 = new ExampleBroadcastListenerService("Listener1", new String[]{"5"});
        MicroService listener2 = new ExampleBroadcastListenerService("Listener2", new String[]{"5"});
    
        //Registration and Verify registration
        messageBus.register(listener1);
        messageBus.register(listener2);
        assertTrue(messageBus.isRegistered(listener1), 
            "Expected: Listener1 is registered with the message bus. Actual: Listener1 is not registered.");
        assertTrue(messageBus.isRegistered(listener2), 
            "Expected: Listener2 is registered with the message bus. Actual: Listener2 is not registered.");
    
        // Action
        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener1);
        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener2);
        Broadcast broadcast = new ExampleBroadcast("TestBroadcast");
        messageBus.sendBroadcast(broadcast);
    
        // Assertion
        assertDoesNotThrow(() -> {
            assertEquals(broadcast, messageBus.awaitMessage(listener1), 
                "Expected: Listener1 receives the broadcast message. Actual: Listener1 did not receive the message.");
            assertEquals(broadcast, messageBus.awaitMessage(listener2), 
                "Expected: Listener2 receives the broadcast message. Actual: Listener2 did not receive the message.");
        });
    
        // Cleanup
        messageBus.unregister(listener1);
        messageBus.unregister(listener2);
        assertFalse(messageBus.isRegistered(listener1), 
            "Expected: Listener1 is no longer registered with the message bus after unregister. Actual: Listener1 is still registered.");
        assertFalse(messageBus.isRegistered(listener2), 
            "Expected: Listener2 is no longer registered with the message bus after unregister. Actual: Listener2 is still registered.");
    }
    

    @Test
    void testSendEvent() throws InterruptedException {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        MicroService handler = new ExampleEventHandlerService("Handler", new String[]{"5"});
    
        // Registration and Verify registration
        messageBus.register(handler);
        assertTrue(messageBus.isRegistered(handler), 
            "Expected: Handler is registered with the message bus. Actual: Handler is not registered.");
    
        // Sending an event with no subscribers
        Event<String> event1 = new ExampleEvent("TestEvent");
        Future<String> future1 = messageBus.sendEvent(event1);
        assertNull(future1, 
            "Expected: Future should be null when no subscribers are available. Actual: Future is not null.");
        
        // Action   
        messageBus.subscribeEvent(ExampleEvent.class, handler);
        Event<String> event2 = new ExampleEvent("TestEvent");
        Future<String> future2 = messageBus.sendEvent(event2);
    
        // Assertion
        assertNotNull(future2, 
            "Expected: Future should not be null after sending the event. Actual: Future is null.");
        assertEquals(event2, messageBus.awaitMessage(handler), 
            "Expected: Handler should receive the event message. Actual: Handler did not receive the event.");

        // Cleanup
        messageBus.unregister(handler);
        assertFalse(messageBus.isRegistered(handler), 
            "Expected: Handler is no longer registered with the message bus after unregister. Actual: Handler is still registered.");
    }
    /*רעיונות לעוד בדיקות שאולי אפשר להוסיף:
     * שני הנדלרים שרשומים ולוודא שהאירוע נכנס רק לתור הודעות של אחד מהם 
     */
    

   /* @Test
    void testAwaitMessageThrowsExceptionIfNotRegistered() {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        MicroService unregisteredService = new ExampleBroadcastListenerService("Unregistered", new String[]{"5"});

        // Assertion
        assertThrows(IllegalStateException.class, () -> {
            messageBus.awaitMessage(unregisteredService);
        }, "Expected IllegalStateException when service is not registered");
        
    }
        /* */

    @Test
    void testAwaitMessageReceivesMessagesCorrectly() throws InterruptedException {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();

        //Pulling a message for a service that is not registered and verifying that an exception is thrown.
        MicroService unregisteredService = new ExampleBroadcastListenerService("Unregistered", new String[]{"5"});
        assertThrows(IllegalStateException.class, () -> {
            messageBus.awaitMessage(unregisteredService);
        }, "Expected IllegalStateException when service is not registered");

        //Creating a new microservice, registering it, and verifying that its message queue is empty
        MicroService service = new ExampleBroadcastListenerService("service", new String[]{"5"});
        messageBus.register(service); 
        messageBus.subscribeBroadcast(ExampleBroadcast.class, service); 
        assertEquals(0, messageBus.getQueueSize(service), 
            "Queue size should be 0 before sending messages for service: " + service.getName());
    
        //Creating two messages and sending them
        ExampleBroadcast msg1 = new ExampleBroadcast("Message 1");
        ExampleBroadcast msg2 = new ExampleBroadcast("Message 2");
        messageBus.sendBroadcast(msg1);
        messageBus.sendBroadcast(msg2);
    
        //Performing awaitMessage for the first time and verifying the results
        Message receivedMessage1 = messageBus.awaitMessage(service);
        assertTrue(receivedMessage1 instanceof ExampleBroadcast, 
            "Expected message type: ExampleBroadcast, but received: " + receivedMessage1.getClass().getName());
        assertEquals("Message 1", ((ExampleBroadcast) receivedMessage1).getSenderId(), 
            "Expected senderId: Message 1, but received: " + ((ExampleBroadcast) receivedMessage1).getSenderId());
        assertEquals(1, messageBus.getQueueSize(service), 
            "Queue size should be 1 after receiving the first message for service: " + service.getName());
    
        //Performing awaitMessage for the second time and verifying the results.
        Message receivedMessage2 = messageBus.awaitMessage(service);
        assertTrue(receivedMessage2 instanceof ExampleBroadcast, 
            "Expected message type: ExampleBroadcast, but received: " + receivedMessage2.getClass().getName());
        assertEquals("Message 2", ((ExampleBroadcast) receivedMessage2).getSenderId(), 
            "Expected senderId: Message 2, but received: " + ((ExampleBroadcast) receivedMessage2).getSenderId());
        assertEquals(0, messageBus.getQueueSize(service), 
            "Queue size should be 0 after receiving all messages for service: " + service.getName());
    }
    

}
//--------------------להוסיף לcompletmjnn
