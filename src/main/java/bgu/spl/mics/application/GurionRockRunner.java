package bgu.spl.mics.application;

import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GurionRockRunner {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java GurionRockRunner <config-file-path>");
            return;
        }

        String configFilePath = args[0];
        File configFile = new File(configFilePath);
        File configDirectory = configFile.getParentFile();

        if (!configFile.exists()) {
            System.err.println("Configuration file not found at: " + configFilePath);
            return;
        }

        try (FileReader reader = new FileReader(configFilePath)) {
            Gson gson = new Gson();
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();

            // Parse Cameras
            List<Camera> cameras = parseCameras(config, configDirectory);

            // Parse LiDARs
            List<LiDarWorkerTracker> lidars = parseLidars(config, configDirectory);

            // Parse PoseService
            GPSIMU gpsimu = parsePoseService(config, configDirectory);

            // Initialize FusionSlam
            FusionSlam fusionSlam = FusionSlam.getInstance();
            fusionSlam.setserviceCounter(cameras.size() + lidars.size() + 1);

            // Initialize TimeService
            int tickTime = config.get("TickTime").getAsInt();
            int duration = config.get("Duration").getAsInt();
            TimeService timeService = new TimeService(tickTime, duration);
            Thread timeThread = new Thread(timeService, "TimeService");

            // Start Threads
            List<Thread> threads = new ArrayList<>();
            for (Camera camera : cameras) {
                CameraService cameraService = new CameraService(camera);
                threads.add(new Thread(cameraService, "CameraService" + camera.getId()));
            }
            for (LiDarWorkerTracker lidar : lidars) {
                LiDarService lidarService = new LiDarService("LiDarService" + lidar.getId(), lidar);
                threads.add(new Thread(lidarService, "LiDarService" + lidar.getId()));
            }

            PoseService poseService = new PoseService(gpsimu);
            threads.add(new Thread(poseService, "PoseService"));

            FusionSlamService fusionSlamService = new FusionSlamService(fusionSlam);
            threads.add(new Thread(fusionSlamService, "FusionSlamService"));

            threads.forEach(Thread::start);

            // Allow services to initialize
            Thread.sleep(100);
            timeThread.start();

            // Wait for TimeService
            timeThread.join();

            System.out.println("Simulation completed successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Camera> parseCameras(JsonObject config, File configDirectory) throws IOException {
        List<Camera> cameras = new ArrayList<>();
        JsonElement camerasElement = config.get("Cameras");

        if (camerasElement.isJsonArray()) {
            JsonArray camerasArray = camerasElement.getAsJsonArray();
            for (JsonElement cameraElement : camerasArray) {
                JsonObject cameraJson = cameraElement.getAsJsonObject();
                int id = cameraJson.get("id").getAsInt();
                int frequency = cameraJson.get("frequency").getAsInt();
                String cameraDataPath = cameraJson.get("camera_datas_path").getAsString();
                String cameraKey = cameraJson.get("camera_key").getAsString();

                File cameraDataFile = new File(configDirectory, cameraDataPath);
                Map<String, List<StampedDetectedObject>> cameraData = readCameraData(cameraDataFile);
                List<StampedDetectedObject> detectedObjects = cameraData.getOrDefault(cameraKey, new ArrayList<>());

                cameras.add(new Camera(id, frequency, detectedObjects));
            }
        } else if (camerasElement.isJsonObject()) {
            JsonObject camerasConfig = camerasElement.getAsJsonObject();
            String cameraDataPath = camerasConfig.get("camera_datas_path").getAsString();
            JsonArray cameraConfigs = camerasConfig.getAsJsonArray("CamerasConfigurations");

            File cameraDataFile = new File(configDirectory, cameraDataPath);
            Map<String, List<StampedDetectedObject>> cameraData = readCameraData(cameraDataFile);

            for (JsonElement cameraElement : cameraConfigs) {
                JsonObject cameraJson = cameraElement.getAsJsonObject();
                int id = cameraJson.get("id").getAsInt();
                int frequency = cameraJson.get("frequency").getAsInt();
                String cameraKey = cameraJson.get("camera_key").getAsString();

                List<StampedDetectedObject> detectedObjects = cameraData.getOrDefault(cameraKey, new ArrayList<>());
                cameras.add(new Camera(id, frequency, detectedObjects));
            }
        }

        return cameras;
    }

    private static Map<String, List<StampedDetectedObject>> readCameraData(File cameraDataFile) throws IOException {
        try (FileReader reader = new FileReader(cameraDataFile)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, new com.google.gson.reflect.TypeToken<Map<String, List<StampedDetectedObject>>>() {}.getType());
        }
    }

    private static List<LiDarWorkerTracker> parseLidars(JsonObject config, File configDirectory) throws IOException {
        List<LiDarWorkerTracker> lidars = new ArrayList<>();
        JsonObject lidarsConfig = null;
    
        if (config.has("LiDarWorkers")) {
            lidarsConfig = config.getAsJsonObject("LiDarWorkers");
        } else if (config.has("Lidars")) {
            lidarsConfig = config.getAsJsonObject("Lidars");
        }
    
        if (lidarsConfig == null) {
            System.err.println("LiDAR section is missing in configuration.");
            return lidars;
        }
    
        String lidarDataPath = lidarsConfig.get("lidars_data_path").getAsString();
        File lidarDataFile = new File(configDirectory, lidarDataPath);
    
        if (!lidarDataFile.exists()) {
            throw new IOException("LiDAR data file not found: " + lidarDataFile.getAbsolutePath());
        }
    
        JsonArray lidarConfigs = lidarsConfig.getAsJsonArray("LidarConfigurations");
        for (JsonElement lidarElement : lidarConfigs) {
            JsonObject lidarJson = lidarElement.getAsJsonObject();
            int id = lidarJson.get("id").getAsInt();
            int frequency = lidarJson.get("frequency").getAsInt();
            lidars.add(new LiDarWorkerTracker(id, frequency, lidarDataFile.getAbsolutePath()));
        }
    
        return lidars;
    }
    

    private static GPSIMU parsePoseService(JsonObject config, File configDirectory) {
        String poseDataPath = config.get("poseJsonFile").getAsString();
        File poseDataFile = new File(configDirectory, poseDataPath);
        return new GPSIMU(poseDataFile.getAbsolutePath());
    }
}
