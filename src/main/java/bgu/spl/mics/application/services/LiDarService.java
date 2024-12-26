package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObject;
import bgu.spl.mics.application.objects.TrackedObject;
import bgu.spl.mics.application.objects.StatisticalFolder;

import java.util.ArrayList;
import java.util.List;

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

    public LiDarService(String name, LiDarWorkerTracker lidarWorkerTracker) {
        super(name);
        this.lidarWorkerTracker = lidarWorkerTracker;
    }

    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast broadcast) -> {
        //----------------------fill
        });
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
        //----------------------fill
        });
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
        //----------------------fill
        });
    
        // ---------------ראשוני, צריך להבין
        subscribeEvent(DetectObjectsEvent.class, (DetectObjectsEvent event) -> {
            List<TrackedObject> trackedObjects = new ArrayList<>();
        
            // Retrieve the detected objects from the event
            StampedDetectedObject stampedDetectedObjects = event.getStampedDetectedObjects();
            int detectionTime = stampedDetectedObjects.getTime();
            int processingTime = detectionTime + lidarWorkerTracker.getFrequency();
        
            // Ensure the current time aligns with the LiDAR worker's processing time
            if (currentTick >= processingTime) {
                List<DetectedObject> detectedObjects = stampedDetectedObjects.getDetectedObjects();
        
                // Process each detected object
                for (DetectedObject detectedObject : detectedObjects) {
                    // Create a TrackedObject with the coordinates from LiDAR
                    TrackedObject trackedObject = new TrackedObject(
                        detectedObject.getId(),
                        detectionTime,
                        detectedObject.getDescription(),
                        lidarWorkerTracker.getCoordinates(
                            detectedObject.getId(),
                            processingTime
                        )
                    );
        
                    // Add to the list of tracked objects
                    trackedObjects.add(trackedObject);
                }
        
                // Send a new TrackedObjectsEvent with the tracked objects
                sendEvent(new TrackedObjectsEvent(processingTime, trackedObjects, getName()));
        
                // Update statistics
                StatisticalFolder.getInstance().updateNumTrackedObjects(trackedObjects.size());
            }
        });

    }
        
}