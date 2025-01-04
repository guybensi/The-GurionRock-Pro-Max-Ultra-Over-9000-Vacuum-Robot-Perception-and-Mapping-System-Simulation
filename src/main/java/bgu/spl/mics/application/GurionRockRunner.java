package bgu.spl.mics.application;

import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 */
public class GurionRockRunner {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java GurionRockRunner <config-file-path>");
            return;
        }

        String configFilePath = args[0];
        File configFile = new File(configFilePath);

        if (!configFile.exists()) {
            System.err.println("Configuration file not found at: " + configFilePath);
            return;
        }

        String basePath = configFile.getParent();
        List<Camera> cameras = new ArrayList<>();
        List<LiDarWorkerTracker> lidars = new ArrayList<>();
        GPSIMU gpsimu = null;
        FusionSlam fusionSlam = null;
        int tickTime = 0;
        int duration = 0;

        try {
            // Read the configuration file
            Gson gson = new Gson();
            JsonObject config = gson.fromJson(new FileReader(configFile), JsonObject.class);

            // Parse simulation time parameters
            tickTime = config.get("TickTime").getAsInt();
            duration = config.get("Duration").getAsInt();

            // Parse Cameras
            parseCameras(config, basePath, cameras);

            // Parse LiDARs
            parseLidars(config, basePath, lidars);

            // Parse GPSIMU
            String poseJsonFile = config.get("poseJsonFile").getAsString();
            gpsimu = new GPSIMU(new File(basePath, poseJsonFile).getAbsolutePath());

            // Initialize FusionSlam
            int numOfSensors = cameras.size() + lidars.size() + 1; // +1 for GPS
            fusionSlam = FusionSlam.getInstance();
            fusionSlam.setserviceCounter(numOfSensors);

            // Display initialization summary
            System.out.println("System Initialized:");
            System.out.println("Cameras: " + cameras.size());
            System.out.println("LiDar Workers: " + lidars.size());
            System.out.println("GPSIMU: Initialized");
            System.out.println("FusionSlam: Initialized with " + numOfSensors + " sensors");

        } catch (IOException e) {
            System.err.println("Error reading configuration file: " + e.getMessage());
            return;
        }

        // Initialize Threads
        List<Thread> threads = initializeThreads(cameras, lidars, gpsimu, fusionSlam, tickTime, duration);

        // Start Threads
        threads.forEach(Thread::start);

        // Wait for FusionSlam thread to complete
        try {
            threads.get(threads.size() - 1).join(); // Assuming the last thread is FusionSlam
        } catch (InterruptedException e) {
            System.err.println("FusionSlam thread interrupted: " + e.getMessage());
        }

        System.out.println("Simulation completed.");
    }

    private static void parseCameras(JsonObject config, String basePath, List<Camera> cameras) {
        if (!config.has("Cameras")) {
            System.err.println("No Cameras section found in configuration.");
            return;
        }

        JsonElement camerasElement = config.get("Cameras");

        if (camerasElement.isJsonArray()) {
            JsonArray camerasArray = camerasElement.getAsJsonArray();
            for (int i = 0; i < camerasArray.size(); i++) {
                JsonObject cameraJson = camerasArray.get(i).getAsJsonObject();
                addCamera(cameras, cameraJson, basePath, "camera" + (i + 1));
            }
        } else if (camerasElement.isJsonObject()) {
            JsonObject camerasObject = camerasElement.getAsJsonObject();
            String cameraDataPath = camerasObject.get("camera_datas_path").getAsString();
            JsonArray cameraConfigs = camerasObject.getAsJsonArray("CamerasConfigurations");
            for (int i = 0; i < cameraConfigs.size(); i++) {
                JsonObject cameraJson = cameraConfigs.get(i).getAsJsonObject();
                addCamera(cameras, cameraJson, new File(basePath, cameraDataPath).getAbsolutePath(), "camera" + (i + 1));
            }
        } else {
            System.err.println("Unexpected format for Cameras in configuration.");
        }
    }

    private static void addCamera(List<Camera> cameras, JsonObject cameraJson, String dataPath, String cameraKey) {
        try {
            int id = cameraJson.get("id").getAsInt();
            int frequency = cameraJson.get("frequency").getAsInt();
            cameras.add(new Camera(id, frequency, dataPath, cameraKey));
        } catch (Exception e) {
            System.err.println("Error adding camera: " + e.getMessage());
        }
    }

    private static void parseLidars(JsonObject config, String basePath, List<LiDarWorkerTracker> lidars) {
        if (!config.has("LiDarWorkers")) {
            System.err.println("No LiDARs section found in configuration.");
            return;
        }

        JsonObject lidarConfig = config.getAsJsonObject("LiDarWorkers");
        String lidarDataPath = lidarConfig.get("lidars_data_path").getAsString();
        JsonArray lidarConfigs = lidarConfig.getAsJsonArray("LidarConfigurations");

        for (JsonElement lidarElement : lidarConfigs) {
            JsonObject lidarJson = lidarElement.getAsJsonObject();
            int id = lidarJson.get("id").getAsInt();
            int frequency = lidarJson.get("frequency").getAsInt();
            lidars.add(new LiDarWorkerTracker(id, frequency, new File(basePath, lidarDataPath).getAbsolutePath()));
        }
    }

    private static List<Thread> initializeThreads(List<Camera> cameras, List<LiDarWorkerTracker> lidars, GPSIMU gpsimu, FusionSlam fusionSlam, int tickTime, int duration) {
        List<Thread> threads = new ArrayList<>();

        // Initialize Camera threads
        cameras.forEach(camera -> threads.add(new Thread(new CameraService(camera))));

        // Initialize LiDAR threads
        int i = 1;
        for (LiDarWorkerTracker lidar : lidars) {
            threads.add(new Thread(new LiDarService("LiDarService" + i++, lidar)));
        }

        // Initialize PoseService thread
        threads.add(new Thread(new PoseService(gpsimu)));

        // Initialize FusionSlamService thread
        threads.add(new Thread(new FusionSlamService(fusionSlam)));

        // Initialize TimeService thread
        threads.add(new Thread(new TimeService(tickTime, duration)));

        return threads;
    }
}
