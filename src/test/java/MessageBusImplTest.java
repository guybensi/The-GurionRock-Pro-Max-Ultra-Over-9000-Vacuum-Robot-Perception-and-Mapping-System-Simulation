import bgu.spl.mics.*;
import bgu.spl.mics.example.messages.ExampleBroadcast;
import bgu.spl.mics.example.messages.ExampleEvent;
import bgu.spl.mics.example.services.ExampleEventHandlerService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class MessageBusImplThreadSafeTest {

    @Test
    void testConcurrentEventHandling() throws InterruptedException, ExecutionException {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<MicroService> handlers = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            MicroService handler = new ExampleEventHandlerService("Handler" + i, new String[]{"5"});
            handlers.add(handler);
            messageBus.register(handler);
            messageBus.subscribeEvent(ExampleEvent.class, handler);
        }

        // Action: Send events concurrently
        Callable<Boolean> sendEventTask = () -> {
            Event<String> event = new ExampleEvent("TestEvent");
            Future<String> future = messageBus.sendEvent(event);
            return future != null;
        };

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(sendEventTask));
        }

        // Wait for all threads to complete
        for (Future<Boolean> future : futures) {
            assertTrue(future.get());
        }

        // Verify that all handlers processed events
        for (MicroService handler : handlers) {
            Message receivedMessage = messageBus.awaitMessage(handler);
            assertNotNull(receivedMessage);
            assertTrue(receivedMessage instanceof ExampleEvent);
        }

        // Cleanup
        for (MicroService handler : handlers) {
            messageBus.unregister(handler);
        }
        executor.shutdown();
    }

    @Test
    void testConcurrentBroadcastHandling() throws InterruptedException {
        // Setup
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<MicroService> listeners = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            MicroService listener = new ExampleEventHandlerService("Listener" + i, new String[]{"5"});
            listeners.add(listener);
            messageBus.register(listener);
            messageBus.subscribeBroadcast(ExampleBroadcast.class, listener);
        }

        // Action: Send broadcast concurrently
        Runnable sendBroadcastTask = () -> messageBus.sendBroadcast(new ExampleBroadcast("TestBroadcast"));

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(sendBroadcastTask));
        }

        // Wait for all threads to complete
        for (Future<?> future : futures) {
            future.get();
        }

        // Verify that all listeners received the broadcast
        for (MicroService listener : listeners) {
            Message receivedMessage = messageBus.awaitMessage(listener);
            assertNotNull(receivedMessage);
            assertTrue(receivedMessage instanceof ExampleBroadcast);
        }

        // Cleanup
        for (MicroService listener : listeners) {
            messageBus.unregister(listener);
        }
        executor.shutdown();
    }
}
