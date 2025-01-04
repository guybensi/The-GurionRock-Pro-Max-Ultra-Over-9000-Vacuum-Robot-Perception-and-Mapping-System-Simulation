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

/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
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

        // Get the parent directory of the config file
        File configDirectory = configFile.getParentFile();
        System.out.println("Config directory resolved to: " + configDirectory.getAbsolutePath());

        try (FileReader reader = new FileReader(configFilePath)) {
            Gson gson = new Gson();
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();

            System.out.println("Configuration loaded successfully.");
            System.out.println("Configuration content: " + config);

            // Parse Cameras
            System.out.println("Parsing Cameras...");
            List<Camera> cameras = parseCameras(config, configDirectory);

            // Parse LiDARs
            System.out.println("Parsing LiDARs...");
            List<LiDarWorkerTracker> lidars = parseLidars(config, configDirectory);

            // Parse PoseService
            System.out.println("Parsing PoseService...");
            String poseDataPath = config.get("poseJsonFile").getAsString();
            File poseDataFile = new File(configDirectory, poseDataPath);
            if (!poseDataFile.exists()) {
                System.err.println("Pose data file not found: " + poseDataFile.getAbsolutePath());
                return;
            }
            GPSIMU gpsimu = new GPSIMU(poseDataFile.getAbsolutePath());
            PoseService poseService = new PoseService(gpsimu);
            Thread poseThread = new Thread(poseService, "PoseService");

            // Parse TimeService
            System.out.println("Parsing TimeService...");
            int tickTime = config.get("TickTime").getAsInt();
            int duration = config.get("Duration").getAsInt();
            TimeService timeService = new TimeService(tickTime, duration);
            Thread timeThread = new Thread(timeService, "TimeService");

            // Initialize Fusion-SLAM
            System.out.println("Initializing Fusion-SLAM...");
            FusionSlam fusionSlam = FusionSlam.getInstance();
            FusionSlamService fusionSlamService = new FusionSlamService(fusionSlam);
            Thread fusionSlamThread = new Thread(fusionSlamService, "FusionSlamService");

            // Update FusionSlam with service count
            int totalServices = cameras.size() + lidars.size() + 1; // +1 for PoseService
            fusionSlam.setserviceCounter(totalServices);
            System.out.println("Total services registered in Fusion-SLAM: " + totalServices);

            // Start all threads except TimeService
            System.out.println("Starting threads...");
            List<Thread> threads = new ArrayList<>();
            for (Camera camera : cameras) {
                System.out.println("Starting CameraService for Camera ID: " + camera.getId());
                CameraService cameraService = new CameraService(camera);
                threads.add(new Thread(cameraService, "CameraService" + camera.getId()));
            }

            for (LiDarWorkerTracker lidar : lidars) {
                System.out.println("Starting LiDarService for LiDAR ID: " + lidar.getId());
                LiDarService lidarService = new LiDarService("LiDarService" + lidar.getId(), lidar);
                threads.add(new Thread(lidarService, "LiDarService" + lidar.getId()));
            }

            threads.add(poseThread);
            threads.add(fusionSlamThread);

            threads.forEach(Thread::start);

            // Allow time for all services to register before starting TimeService
            System.out.println("Allowing services to register...");
            Thread.sleep(100);

            // Start TimeService last
            System.out.println("Starting TimeService...");
            timeThread.start();

            // Wait for TimeService to complete
            System.out.println("Waiting for TimeService to finish...");
            timeThread.join();

            System.out.println("Simulation completed successfully!");

        } catch (IOException e) {
            System.err.println("Error reading configuration file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Simulation interrupted.");
        }
    }

    private static List<Camera> parseCameras(JsonObject config, File configDirectory) {
        List<Camera> cameras = new ArrayList<>();
    
        if (config.has("Cameras")) {
            JsonElement camerasElement = config.get("Cameras");
    
            // Check if "Cameras" is a JSON array
            if (camerasElement.isJsonArray()) {
                JsonArray camerasArray = camerasElement.getAsJsonArray();
                for (JsonElement cameraElement : camerasArray) {
                    JsonObject cameraJson = cameraElement.getAsJsonObject();
                    parseSingleCamera(cameraJson, cameras, configDirectory.getAbsolutePath()); // Pass path as String
                }
            }
            // Check if "Cameras" is an object and contains "CamerasConfigurations"
            else if (camerasElement.isJsonObject()) {
                JsonObject camerasObject = camerasElement.getAsJsonObject();
                if (camerasObject.has("CamerasConfigurations")) {
                    String cameraDataPath = camerasObject.has("camera_datas_path")
                            ? camerasObject.get("camera_datas_path").getAsString()
                            : null;
    
                    if (cameraDataPath != null) {
                        JsonArray camerasArray = camerasObject.getAsJsonArray("CamerasConfigurations");
                        File cameraDataFile = new File(configDirectory, cameraDataPath);
                        for (JsonElement cameraElement : camerasArray) {
                            JsonObject cameraJson = cameraElement.getAsJsonObject();
                            parseSingleCamera(cameraJson, cameras, cameraDataFile.getAbsolutePath()); // Pass path as String
                        }
                    } else {
                        System.err.println("Invalid Cameras configuration structure: Missing 'camera_datas_path'.");
                    }
                } else {
                    System.err.println("Invalid Cameras configuration structure: Missing 'CamerasConfigurations'.");
                }
            } else {
                System.err.println("Unexpected Cameras configuration type.");
            }
        } else {
            System.err.println("No Cameras section found in configuration.");
        }
    
        return cameras;
    }
    
    private static void parseSingleCamera(JsonObject cameraJson, List<Camera> cameras, String cameraDataPath) {
        try {
            int id = cameraJson.get("id").getAsInt();
            int frequency = cameraJson.get("frequency").getAsInt();
            String key = cameraJson.get("camera_key").getAsString();
    
            System.out.println("Camera detected: ID=" + id + ", Frequency=" + frequency + ", Key=" + key);
            cameras.add(new Camera(id, frequency, cameraDataPath, key));
        } catch (Exception e) {
            System.err.println("Error parsing camera configuration: " + e.getMessage());
        }
    }
    

    private static List<LiDarWorkerTracker> parseLidars(JsonObject config, File configDirectory) {
        List<LiDarWorkerTracker> lidars = new ArrayList<>();
        if (config.has("LiDarWorkers") || config.has("Lidars")) {
            JsonObject lidarsConfig = config.has("LiDarWorkers")
                    ? config.getAsJsonObject("LiDarWorkers")
                    : config.getAsJsonObject("Lidars");

            String lidarDataPath = lidarsConfig.has("lidars_data_path")
                    ? lidarsConfig.get("lidars_data_path").getAsString()
                    : null;

            if (lidarDataPath != null) {
                File lidarDataFile = new File(configDirectory, lidarDataPath);
                if (!lidarDataFile.exists()) {
                    System.err.println("LiDAR data file not found: " + lidarDataFile.getAbsolutePath());
                    return lidars;
                }

                JsonArray lidarsArray = lidarsConfig.getAsJsonArray("LidarConfigurations");
                for (JsonElement lidarElement : lidarsArray) {
                    JsonObject lidarJson = lidarElement.getAsJsonObject();
                    int id = lidarJson.get("id").getAsInt();
                    int frequency = lidarJson.get("frequency").getAsInt();
                    System.out.println("LiDAR detected: ID=" + id + ", Frequency=" + frequency);
                    lidars.add(new LiDarWorkerTracker(id, frequency, lidarDataFile.getAbsolutePath()));
                }
            } else {
                System.err.println("Missing 'lidars_data_path' in LiDAR configuration.");
            }
        } else {
            System.err.println("No LiDARs section found in configuration.");
        }
        return lidars;
    }
}
