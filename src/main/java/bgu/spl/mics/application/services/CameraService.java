package bgu.spl.mics.application.services;

import java.util.ArrayDeque;
import java.util.Queue;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.StampedDetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    private final Camera camera;
    private Queue<DetectObjectsEvent> eventQueue;


    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera) {
        super("CameraService" + camera.getId());
        this.camera = camera;
        this.eventQueue = new ArrayDeque<>();

    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        // Subscribe to TickBroadcast
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast broadcast) -> {
            int currentTime = broadcast.getTime();
            System.out.println(getName() + ": got a tick, " + currentTime + " and my status is: " + camera.getStatus());
            // Check if the camera is active and it's time to send an event
            if (camera.getStatus() == STATUS.UP) {
                StampedDetectedObject detectedObject = camera.getDetectedObjectsAtTime(currentTime);
                if (camera.getStatus() == STATUS.ERROR){
                    System.out.println(getName() + ": has an error");
                    terminate();
                    sendBroadcast(new CrashedBroadcast(camera.getErrMString(), "camera" + camera.getId()));
                }
                else{
                    if (detectedObject != null) {
                        int sendTime = currentTime + camera.getFrequency();
                        DetectObjectsEvent event = new DetectObjectsEvent(detectedObject, getName(), sendTime);
                        eventQueue.add(event);
                    }
                    // Process events that are ready to be sent
                    while (!eventQueue.isEmpty()) {
                        DetectObjectsEvent event = eventQueue.peek();
                        if (event.getSendTime() > currentTime) {
                            break; // If the first event is not ready, stop processing
                        }
                        DetectObjectsEvent readyEvent = eventQueue.poll(); // Remove the first event (FIFO)
                        sendEvent(readyEvent);
                        System.out.println(getName() + ": has sent DetectObjectsEvent from time" + event.getStampedDetectedObjects().getTime());
                        StatisticalFolder.getInstance().updateNumDetectedObjects(
                                readyEvent.getStampedDetectedObjects().getDetectedObjects().size()
                        );
                        StatisticalFolder.getInstance().updateLastFrame(getName(), readyEvent);
                    }  
                }
                if (camera.getStatus() == STATUS.DOWN){
                    System.out.println(getName() + ": down1 and terminate");
                    terminate();
                    sendBroadcast(new TerminatedBroadcast(getName()));  
                }
            }
            else {//camers is down 
                System.out.println(getName() + ": down2 and terminate");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));     
            }
        });
        // Subscribe to TerminatedBroadcast
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
        
    }
}