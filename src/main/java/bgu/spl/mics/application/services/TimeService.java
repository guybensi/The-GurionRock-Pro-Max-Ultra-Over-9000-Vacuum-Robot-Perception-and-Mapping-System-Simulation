package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {

    private final int tickTime;  // Duration of each tick in milliseconds
    private final int duration;  // Total number of ticks



    /**
     * Constructor for TimeService.
     *
     * @param tickTime  The duration of each tick in milliseconds.
     * @param duration  The total number of ticks before the service terminates.
     */
    public TimeService(int tickTime, int duration) {
        super("TimeService");
        this.tickTime = tickTime;
        this.duration = duration;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() {
        System.out.println("TimeService initialized.");
        
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
            System.out.println(getName() + ": got crashed");
            terminate();
        });
        
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast broadcast) -> {
            int currentTick = broadcast.getTime();
            
            if (currentTick < duration && !FusionSlam.getInstance().isTerminated() && !isterminated()) {
                try {
                    // חכה את משך הזמן המתאים לכל Tick
                    Thread.sleep(tickTime * 1000L);
                    
                    // שדר את ה-Tick הבא
                    sendBroadcast(new TickBroadcast(currentTick + 1, duration));
                    int sentTick = currentTick +1;
                    System.out.println("TimeService broadcasted Tick: "  + sentTick);
        
                    // עדכן את זמן הריצה במערכת הסטטיסטית
                    StatisticalFolder.getInstance().updateSystemRuntime(1);
        
                } catch (InterruptedException e) {
                    System.out.println("TimeService interrupted during Tick: " + currentTick);
                    Thread.currentThread().interrupt(); // שמור על סטטוס ה-interrupt
                    terminate();
                    sendBroadcast(new TerminatedBroadcast(getName()));
                    System.out.println("TimeService broadcasted TerminatedBroadcast.");
                }
            } else {
                // סיים את השירות כאשר התנאים מסתיימים
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));
                System.out.println("TimeService broadcasted TerminatedBroadcast.");
            }
        });
        
        sendBroadcast(new TickBroadcast(1, duration));

    }
}