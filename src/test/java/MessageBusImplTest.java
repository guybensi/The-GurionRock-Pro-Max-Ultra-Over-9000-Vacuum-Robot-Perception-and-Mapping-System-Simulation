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
        messageBus.register(listener);

        // Verify registration
        assertTrue(messageBus.isRegistered(listener));

        // Action
        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener);

        // Assertion
        assertTrue(messageBus.isSubscribed(ExampleBroadcast.class, listener));

        // Cleanup
        messageBus.unregister(listener);
    }

    @Test
    void testRegisterEventSubscription() {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        MicroService handler = new ExampleEventHandlerService("Handler", new String[]{"5"});
        messageBus.register(handler);

        // Verify registration
        assertTrue(messageBus.isRegistered(handler));

        // Action
        messageBus.subscribeEvent(ExampleEvent.class, handler);

        // Assertion
        assertTrue(messageBus.isSubscribed(ExampleEvent.class, handler));

        // Cleanup
        messageBus.unregister(handler);
    }

    @Test
    void testSendBroadcast() {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        MicroService listener1 = new ExampleBroadcastListenerService("Listener1", new String[]{"5"});
        MicroService listener2 = new ExampleBroadcastListenerService("Listener2", new String[]{"5"});

        messageBus.register(listener1);
        messageBus.register(listener2);

        // Verify registration
        assertTrue(messageBus.isRegistered(listener1));
        assertTrue(messageBus.isRegistered(listener2));

        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener1);
        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener2);

        // Action
        Broadcast broadcast = new ExampleBroadcast("TestBroadcast");
        messageBus.sendBroadcast(broadcast);

        // Assertion
        assertDoesNotThrow(() -> {
            assertEquals(broadcast, messageBus.awaitMessage(listener1));
            assertEquals(broadcast, messageBus.awaitMessage(listener2));
        });

        // Cleanup
        messageBus.unregister(listener1);
        messageBus.unregister(listener2);
    }

    @Test
    void testSendEvent() throws InterruptedException {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        MicroService handler = new ExampleEventHandlerService("Handler", new String[]{"5"});

        messageBus.register(handler);

        // Verify registration
        assertTrue(messageBus.isRegistered(handler));

        messageBus.subscribeEvent(ExampleEvent.class, handler);

        // Action
        Event<String> event = new ExampleEvent("TestEvent");
        Future<String> future = messageBus.sendEvent(event);

        // Assertion
        assertNotNull(future);
        assertEquals(event, messageBus.awaitMessage(handler));

        // Cleanup
        messageBus.unregister(handler);
    }

    @Test
    void testAwaitMessageThrowsExceptionIfNotRegistered() {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        MicroService unregisteredService = new ExampleBroadcastListenerService("Unregistered", new String[]{"5"});

        // Assertion
        assertThrows(IllegalStateException.class, () -> {
            messageBus.awaitMessage(unregisteredService);
        });
    }
}
