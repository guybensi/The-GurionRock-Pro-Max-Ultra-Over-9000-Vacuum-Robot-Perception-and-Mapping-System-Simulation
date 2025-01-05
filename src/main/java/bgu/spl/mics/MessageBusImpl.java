package bgu.spl.mics;

import java.util.*;
import java.util.concurrent.*;



/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
    public class MessageBusImpl implements MessageBus {
        private final Map<Class<? extends Event<?>>, Queue<MicroService>> eventSubscribers = new ConcurrentHashMap<>();
        private final Map<Class<? extends Broadcast>, List<MicroService>> broadcastSubscribers = new ConcurrentHashMap<>();
        private final Map<Event<?>, Future<?>> eventFutures = new ConcurrentHashMap<>();
        private final Map<MicroService, BlockingQueue<Message>> microServiceQueues = new ConcurrentHashMap<>();

        private static class SingletonHolderMessageBusImpl { 
            private static final MessageBusImpl INSTANCE = new MessageBusImpl();
        }
    
        public static MessageBusImpl getInstance() {
            return SingletonHolderMessageBusImpl.INSTANCE;
        }

        @Override
        public void register(MicroService m) {
            microServiceQueues.putIfAbsent(m, new LinkedBlockingQueue<>());
            System.out.println("Registered MicroService: " + m.getName());
        }

        @Override
        public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
            eventSubscribers.putIfAbsent(type, new LinkedList<>());
            Queue <MicroService> subscribers = eventSubscribers.get(type);
            synchronized(subscribers){
                if (!subscribers.contains(m)) {  
                    subscribers.add(m);
                }
            }
        }

        @Override
        public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
            broadcastSubscribers.putIfAbsent(type, new ArrayList<>());
            List<MicroService> subscribers = broadcastSubscribers.get(type);
            synchronized(subscribers){
                if (!subscribers.contains(m)) { 
                    subscribers.add(m);
                    System.out.println(m.getName() + " subscribed to Broadcast: " + type.getSimpleName());
                }
            }
            
        }

        
        @Override
        public <T> void complete(Event<T> e, T result) {
            @SuppressWarnings("unchecked")
            Future<T> future = (Future<T>) eventFutures.get(e);
            if (future != null) {
                future.resolve(result); 
                eventFutures.remove(e);
            }
        }
        

    @Override
    public void sendBroadcast(Broadcast b) {
        List<MicroService> subscribers = broadcastSubscribers.get(b.getClass());
        synchronized(subscribers){
            if (subscribers == null || subscribers.isEmpty()) {
                System.out.println("No subscribers found for broadcast: " + b.getClass().getSimpleName());
            } else {
                for (MicroService m : subscribers) {
                    if (!microServiceQueues.containsKey(m)) {
                        System.out.println("Error: MicroService " + m.getName() + " is not registered in microServiceQueues.");
                    }
                    try {
                        microServiceQueues.get(m).put(b);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

        
        @Override
        public <T> Future<T> sendEvent(Event<T> e) {
            Queue <MicroService> subscribers = eventSubscribers.get(e.getClass());
            if (subscribers == null || subscribers.isEmpty()) {
                return null;
            }
            MicroService selectedService;
            synchronized(subscribers){
                selectedService = subscribers.poll(); 
                if (selectedService != null) {
                    subscribers.add(selectedService); 
                }
            }
            if (selectedService == null || !microServiceQueues.containsKey(selectedService)) {
                return null; // No valid service to handle the event
            }
            Future<T> future = new Future<>();
            eventFutures.putIfAbsent(e, future);
            try {
                microServiceQueues.get(selectedService).put(e);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            return future;
        }

        @Override
        public void unregister(MicroService m) {
            if (microServiceQueues.containsKey(m)){
                microServiceQueues.remove(m);
                for (Queue <MicroService> subscribers : eventSubscribers.values()) {
                    synchronized(subscribers){ 
                        subscribers.remove(m);
                    }
                }
                for (List<MicroService> subscribers : broadcastSubscribers.values()) {
                    synchronized(subscribers){ 
                        subscribers.remove(m);
                    }
                }
                System.out.println("Unregistered MicroService: " + m.getName());
            }
        }

        @Override
        public Message awaitMessage(MicroService m) throws InterruptedException {
            BlockingQueue<Message> queue = microServiceQueues.get(m);
            if (queue == null){
                throw new IllegalStateException("MicroService " + m.getName() + " is not registered.");
            }
            return queue.take(); 
        }

        
        //-------------------------------פונקציות עזר לטסטים----------------------------------------
        // פונקציה 1: בודקת אם המיקרו-שירות רשום
        public boolean isRegistered(MicroService micro) {
            return microServiceQueues.containsKey(micro);
         }

         // פונקציה 2: מחזירה את מספר המיקרו-שירותים הרשומים
         public int getNumberOfRegisters() {
            return microServiceQueues.size();
        }

        // פונקציה 3: בודקת אם המיקרו-שירות מנוי לאירוע מסוג Broadcast
         public boolean isSubscribedToBroad(Class<? extends Broadcast> type, MicroService listener) {
            List<MicroService> subscribers = broadcastSubscribers.get(type);
            return subscribers != null && subscribers.contains(listener);
        }

        // פונקציה 4: מחזירה את מספר המנויים לאירוע מסוג Broadcast
        public int getNumberOfSubscribersToBroad(Class<? extends Broadcast> type) {
            List<MicroService> subscribers = broadcastSubscribers.get(type);
            if (subscribers == null) {
                return 0;
            } else {
                return subscribers.size();
            }
        }

        // פונקציה 5: בודקת אם המיקרו-שירות מנוי לאירוע מסוג Event
        public boolean isSubscribedToEvent(Class<? extends Event<?>> type, MicroService listener) {
            Queue<MicroService> subscribers = eventSubscribers.get(type);
            return subscribers != null && subscribers.contains(listener);
        }
        
        
        // פונקציה 6: מחזירה את מספר המנויים לאירוע מסוג Event
        public int getNumberOfSubscribersToEvent(Class<? extends Event<?>> type) {
            Queue<MicroService> subscribers = eventSubscribers.get(type); // Corrected to eventSubscribers
            if (subscribers == null) {
                return 0;
            } else {
                return subscribers.size();
            }
        }
        //פונקציה 7
        public int getQueueSize(MicroService m) {
            BlockingQueue<Message> queue = microServiceQueues.get(m);
            if (queue == null) {
                return 0;
            } else {
                return queue.size();
            }
        }

        public Queue<MicroService> getEventSubscribers(Class<? extends Event<?>> type) {
            return eventSubscribers.get(type);
        }
        

}