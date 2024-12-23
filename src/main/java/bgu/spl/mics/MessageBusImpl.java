package bgu.spl.mics;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
public class MessageBusImpl implements MessageBus {
    private final Lock EventLock = new ReentrantLock();
    private final Lock BroadcastLock = new ReentrantLock();
    private final Map<Class<? extends Event<?>>, Queue<MicroService>> eventSubscribers = new ConcurrentHashMap<>();
    private final Map<Class<? extends Broadcast>, List<MicroService>> broadcastSubscribers = new ConcurrentHashMap<>();
    private final Map<Event<?>, Future<?>> eventFutures = new ConcurrentHashMap<>();
    private final Map<MicroService, BlockingQueue<Message>> microServiceQueues = new ConcurrentHashMap<>();
    private static volatile MessageBusImpl instance = null;
//פונקציות חדשות


	/**
     * מחזיר את המופע היחיד של MessageBusImpl (אם לא קיים ייווצר אחד).
     */
    
    public static MessageBusImpl getInstance() {
        // שימוש במנגנון סינכרון לוודא שמופע MessageBusImpl ייווצר רק פעם אחת
        if (instance == null) {
            synchronized (MessageBusImpl.class) {
                if (instance == null) {
                    instance = new MessageBusImpl();
                }
            }
        }
        return instance;
    }
    
//פונקציות רשומות
	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
        // אם אין מנויים על סוג האירוע, ניצור רשימה חדשה
        eventSubscribers.putIfAbsent(type, new LinkedList<>());
        Queue <MicroService> subscribers = eventSubscribers.get(type);
        
        // אם המיקרו-שירות לא נרשם כבר, נרשום אותו
        if (!subscribers.contains(m)) {
            subscribers.add(m);
        }
    }

	/**
     * רושם מיקרו-שירות למנוי על ברודקאסט מסוג type.
     */
    @Override
    public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
        // אם אין מנויים על סוג הברודקאסט, ניצור רשימה חדשה
        broadcastSubscribers.putIfAbsent(type, new ArrayList<>());
        List<MicroService> subscribers = broadcastSubscribers.get(type);
        
        // אם המיקרו-שירות לא נרשם כבר, נרשום אותו
        if (!subscribers.contains(m)) {
            subscribers.add(m);
        }
    }

	/**
     * מעדכן את ה-Future של האירוע, כשהוא מקבל את תוצאת הביצוע.
     * משתמש במידע שנשלח כדי להחזיר תוצאה ל-Future המתאימה.
     */
	@Override
	public <T> void complete(Event<T> e, T result) {
		// הוצא את ה-Future בצורה בטוחה מבלי לבצע cast לא בטוח
		Future<?> future = eventFutures.get(e); // מבצע cast רק אחרי בדיקה אם זה אותו סוג של Future   
        if (future != null) {
            // עדכן את התוצאה והגדר את ה-Future כ-"הושלם"
            future.setResult((T)result);  // עדכון התוצאה
            future.setIsResolved(true); // עדכון המצב ל-"נפתר"
        }	
	}

	/**
     * שולח ברודקאסט לכל המיקרו-שירותים שנרשמו לאותו ברודקאסט.
     */
    @Override
    public void sendBroadcast(Broadcast b) {
        // בודק אם יש מנויים לברודקאסט מסוג זה
        List<MicroService> subscribers = broadcastSubscribers.get(b.getClass());
        
        // אם יש מנויים, שולח להם את ההודעה
        if (subscribers != null) {
            for (MicroService m : subscribers) {
                try {
                    // שמים את הברודקאסט בתור של המיקרו-שירות
                    microServiceQueues.get(m).put(b);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
    }

	
	/**
     * שולח אירוע למיקרו-שירות שנרשם אליו (אם יש מנוי).
     * אם יש מנויים, האירוע נשלח לפי עקרון round-robin (ברירת מחדל: המיקרו-שירות הראשון).
     */
    @Override
    public <T> Future<T> sendEvent(Event<T> e) {
        // בודק אם יש מנויים לאירוע מסוג זה
        Queue <MicroService> subscribers = eventSubscribers.get(e.getClass());
        
        // אם אין מנויים, מחזיר null
        if (subscribers == null || subscribers.isEmpty()) {
            return null;
        }

        // בוחר מיקרו-שירות לשלוח אליו את האירוע (בחרנו כאן את הראשון ברשימה)
        MicroService selectedService = subscribers.poll(); // במימוש זה, בחרנו את המיקרו-שירות הראשון
        Future<T> future = new Future<>();
        
        // שומרים את ה-Future של האירוע כך שנוכל להחזיר את התוצאה בהמשך
        eventFutures.put(e, future);

        try {
            // שולחים את האירוע למיקרו-שירות הנבחר
            microServiceQueues.get(selectedService).put(e);
            subscribers.add(selectedService);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // מחזירים את ה-Future של האירוע
        return future;
    }

    @Override
    public void register(MicroService m) {
        synchronized(microServiceQueues){
            microServiceQueues.put(m, new LinkedBlockingQueue<>());
        }
    }
    

	@Override
    public void unregister(MicroService m) {
        // מסירים את המיקרו-שירות מהמפות השונות
        microServiceQueues.remove(m);
        
        // מסירים את המיקרו-שירות מהמנויים לאירועים
        for (Queue <MicroService> subscribers : eventSubscribers.values()) {
            subscribers.remove(m);
        }
        
        // מסירים את המיקרו-שירות מהמנויים לברודקאסטים
        for (List<MicroService> subscribers : broadcastSubscribers.values()) {
            subscribers.remove(m);
        }
    }

	 /**
     * מחפש את ההודעה הבאה בתור של המיקרו-שירות וממתין לה אם אין.
     * במקרה שאין הודעה, המיקרו-שירות יחכה עד שתהיה אחת.
     */
    @Override
    public Message awaitMessage(MicroService m) throws InterruptedException {
        // בודק אם המיקרו-שירות נרשם
        BlockingQueue<Message> queue = microServiceQueues.get(m);

        // אם לא נרשם, פשוט מחכה שהמיקרו-שירות יירשם או שההודעה תיכנס לתור
        while (queue == null) {
            // מחכה עד שהמיקרו-שירות יירשם לתור
            Thread.sleep(50);  // עיכוב קטן (חצי שנייה) כדי לא לבצע חיפוש אינסופי
            queue = microServiceQueues.get(m);  // בודק שוב אם המיקרו-שירות נרשם
        }

        // מחזיר את ההודעה הבאה בתור, ומחכה אם אין הודעה זמינה
        return queue.take();  // תחסום עד שיתקבלו הודעות
    }


	

}
