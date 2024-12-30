package bgu.spl.mics.application.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bgu.spl.mics.*;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.messages.*;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 */
public class FusionSlamService extends MicroService {
    private final FusionSlam fusionSlam;

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
            fusionSlam.processTrackedObjects(event.getTrackedObjects());
            complete(event, true);
        });

        // Register for PoseEvent
        subscribeEvent(PoseEvent.class, event -> {
            fusionSlam.addPose(event.getPose());
            complete(event, true);
        });

        // Register for TickBroadcast
        subscribeBroadcast(TickBroadcast.class, broadcast -> {
           
        });

        // Register for TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> {
            fusionSlam.decreaseServiceCounter();
            if (fusionSlam.getserviceCounter() == 0) {
                // Generate output file
                terminate();
                Map<String, Object> lastFrames = new HashMap<>(); // Populate if isError = true
                List<Pose> poses = new ArrayList<>(); // Populate if isError = true
                fusionSlam.generateOutputFile("output_file.json", false, null, null, lastFrames, poses);// איפה הקובץ?
            }
        });

        // Register for CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> {
            terminate();
            // Generate output file with errors
            boolean isError = true; // Set to true if an error occurred
            String errorDescription = broadcast.getErrorMessage(); // Populate if isError = true
            String faultySensor = broadcast.getSenderId(); // Populate if isError = true
            Map<String, Object> lastFrames = new HashMap<>(); // Populate if isError = true
            List<Pose> poses = new ArrayList<>(); // Populate if isError = true
            fusionSlam.generateOutputFile("output_file.json", isError, errorDescription, faultySensor, lastFrames, poses);// איפה הקובץ?
        });
    }
}