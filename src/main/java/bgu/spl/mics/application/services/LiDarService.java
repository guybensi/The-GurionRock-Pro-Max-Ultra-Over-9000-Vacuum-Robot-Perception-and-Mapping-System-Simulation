package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.TrackedObject;
import bgu.spl.mics.application.objects.StatisticalFolder;

//import static org.junit.Assert.assertSame;

//import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {

    private final LiDarWorkerTracker lidarWorkerTracker;
    private  PriorityQueue<TrackedObjectsEvent> eventQueue;


    public LiDarService(String name, LiDarWorkerTracker lidarWorkerTracker) {
        super(name);
        this.lidarWorkerTracker = lidarWorkerTracker;
        this.eventQueue = new PriorityQueue<>(Comparator.comparingInt(event -> event.getTime()));

    }

    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, tick -> {
            int currentTime = tick.getTime();
            System.out.println(getName() + ": got a tick and the tick is, " + tick.getTime());
            lidarWorkerTracker.updateTick(currentTime);
            while (!eventQueue.isEmpty()) {
                TrackedObjectsEvent event = eventQueue.peek();
                if (event.getdesignatedTime() > currentTime) {
                    break;
                }
                TrackedObjectsEvent readyEvent = eventQueue.poll();
                complete(readyEvent.getHandeledEvent(), true);
                lidarWorkerTracker.setLastTrackedObjects(readyEvent.getTrackedObjects());
                System.out.println(getName() + ": sent TrackedObjectsEvent at time" + currentTime+ "for object from time" + event.getTime() );
                sendEvent(readyEvent);
                StatisticalFolder.getInstance().updateNumDetectedObjects(
                    readyEvent.getTrackedObjects().size());
                StatisticalFolder.getInstance().updateLastFrame(getName(), readyEvent);

            } 
            if (eventQueue.isEmpty() && (lidarWorkerTracker.getStatus()==STATUS.DOWN)){
                System.out.println(getName() + ": is down, finished and terminated");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));    
            } 
        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
            if (broadcast.getSenderId() == "TimeService"){
                System.out.println(getName() + ": got TerminatedBroadcast from TimeService");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));  
            }
        });
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
            System.out.println(getName() + ": got crashed");
            terminate();
        });
    
        subscribeEvent(DetectObjectsEvent.class, event -> {
            System.out.println(getName() + ": got DetectObjectsEvent from" +event.getSenderName() +"for time:" + event.getStampedDetectedObjects().getTime());
            if(lidarWorkerTracker.getStatus()==STATUS.UP){   
                List<TrackedObject> TrackedObjects = lidarWorkerTracker.prosseingEvent(event.getStampedDetectedObjects());
               if (lidarWorkerTracker.getStatus()==STATUS.ERROR){
                    System.out.println(getName() + ": has an eror");
                    terminate();
                    sendBroadcast(new CrashedBroadcast("LidarWorker" + lidarWorkerTracker.getId() + "disconnected", this.getName()+ ""+"LidarWorker" + lidarWorkerTracker.getId() ));
               }
               else{
                    int designatedTime = event.getStampedDetectedObjects().getTime() + lidarWorkerTracker.getFrequency();
                    int currTime = lidarWorkerTracker.getCurrentTick();
                    TrackedObjectsEvent toSendEvent = (new TrackedObjectsEvent(event, event.getStampedDetectedObjects().getTime(), TrackedObjects, getName(), designatedTime));
                    if (designatedTime <= currTime){ 
                        complete(event, true);
                        System.out.println(getName() + ": sent TrackedObjectsEvent at time" + currTime + "for object from time" + event.getStampedDetectedObjects().getTime() );
                        sendEvent(toSendEvent);
                        lidarWorkerTracker.setLastTrackedObjects(TrackedObjects);
                        StatisticalFolder.getInstance().updateNumTrackedObjects(TrackedObjects.size());
                        StatisticalFolder.getInstance().updateLastFrame(getName(), toSendEvent);
                    }
                    else{
                        eventQueue.add(toSendEvent);
                    }
               }
               if (eventQueue.isEmpty() && lidarWorkerTracker.getStatus() == STATUS.DOWN){
                System.out.println(getName() + ": is down, finished and terminated");
                    terminate();
                    sendBroadcast(new TerminatedBroadcast(getName()));  
                }
            }
            else if (eventQueue.isEmpty()){
                System.out.println(getName() + ": is down2, finished and terminated");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));    
            }          
        });


    }
        
}