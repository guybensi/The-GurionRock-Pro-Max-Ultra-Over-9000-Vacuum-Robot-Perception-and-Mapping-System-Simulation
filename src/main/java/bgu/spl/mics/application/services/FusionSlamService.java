package bgu.spl.mics.application.services;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import bgu.spl.mics.*;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.messages.*;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 */
public class FusionSlamService extends MicroService {
    private final FusionSlam fusionSlam;
private PriorityQueue<TrackedObjectsEvent> waitingTrackedObjects = 
    new PriorityQueue<>(Comparator.comparingInt(e -> e.getTrackedObjects().get(0).getTime()));


    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlamService");
        this.fusionSlam = FusionSlam.getInstance();    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle events and broadcasts.
     */
    @Override
    protected void initialize() {
        // Register for TrackedObjectsEvent
        subscribeEvent(TrackedObjectsEvent.class, event -> {
            if (fusionSlam.getPoseAtTime(event.getTrackedObjects().get(0).getTime())  != null){
                fusionSlam.processTrackedObjects(event.getTrackedObjects());
                System.out.println("the event has been processed in: " + getName());
                complete(event, true);
            }
            else{
                System.out.println("this event had no pose");
                waitingTrackedObjects.add(event);
            }
            
        });

        // Register for PoseEvent
        subscribeEvent(PoseEvent.class, event -> {
            fusionSlam.addPose(event.getPose());
            complete(event, true);
            while (!waitingTrackedObjects.isEmpty() && fusionSlam.getPoseAtTime(waitingTrackedObjects.peek().getTime()) != null){
                TrackedObjectsEvent e = waitingTrackedObjects.poll(); 
                fusionSlam.processTrackedObjects(e.getTrackedObjects());
                System.out.println("the event has been processed in: " + getName());
                complete(e, true);
            }
        });

        // Register for TickBroadcast
        subscribeBroadcast(TickBroadcast.class, broadcast -> {
            System.out.println(getName() + ": got a tick and the tick is, " + broadcast.getTime());
           fusionSlam.setTick(broadcast.getTime());
        });

        // Register for TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> {
            
            fusionSlam.decreaseServiceCounter();
            if (fusionSlam.getserviceCounter() == 0) {
                // Generate output file
                System.out.println(getName() + ": counter is 0 to terminate");

                terminate();
                System.out.println(getName() + ": is terminated");
                Map<String, Object> lastFrames = new HashMap<>(); // Populate if isError = true
                List<Pose> poses = fusionSlam.getPosesUpToTick(fusionSlam.getTick()); // Populate if isError = true
                System.out.println(getName() + ": is printing an output file");
                fusionSlam.generateOutputFile("output_file.json", false, null, null, lastFrames, poses);// איפה הקובץ?
            }/// כאן במקרה שאין שגיאה לא צריך לייצא את הפוזות האחרונות ואת הפריימים האחרונים
        });

        // Register for CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> {
            System.out.println(getName() + ": got crashed");
            terminate();
            // Generate output file with errors
            boolean isError = true; // Set to true if an error occurred
            String errorDescription = broadcast.getErrorMessage(); // Populate if isError = true
            String faultySensor = broadcast.getSenderId(); // Populate if isError = true
            Map<String, Object> lastFrames = new HashMap<>(); /// כאן צריך להבין איך בונים את האובייקט הזה אין מה לשלוח אותו ריק
            List<Pose> poses = fusionSlam.getPosesUpToTick(fusionSlam.getTick()); // אולי אין מה למשוך את המידע ולשלוח אותו חזרה לפיוזן פשוט לבדוק אותו שם 
            System.out.println(getName() + ": is printing an output file");
            fusionSlam.generateOutputFile("output_file.json", isError, errorDescription, faultySensor, lastFrames, poses);// איפה הקובץ?
        });//// אולי כדאי פשוט שהפונקציה תקבל רק את 4 הארגומנטים הראשונים, פוזות יש לו בשדה שלו ואת הפריימס צריך לחשוב איך עושים 
    }
}
//// אולי להוסיף לסטטיסטיק פולדר מפה של מצלמות והדידקטד אובג'ט שלהן
///  ולידר והלאסט טרקאקטד אובג' שלו וכל פעם ששולחים מידע לסטטיסטיק מעדכנים שם ובסוף פשוט עוברים על הרשימות האלו