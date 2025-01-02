package bgu.spl.mics.application;

import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 * <p>
 * This class initializes the system and starts the simulation by setting up
 * services, objects, and configurations.
 * </p>
 */
public class GurionRockRunner {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java GurionRockRunner <config-file-path>");
            return;
        }

        String configFilePath = args[0];
        try (FileReader reader = new FileReader(configFilePath)) {
            File configFile = new File(configFilePath);
            if (!configFile.exists()) {
                System.out.println("Configuration file not found at: " + configFilePath);
                return;
            }

            Gson gson = new Gson();
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();

            // Print loaded configuration for debugging
            System.out.println("Configuration loaded: " + config);

            // Parse Cameras
            JsonObject camerasConfig = config.getAsJsonObject("Cameras");
            String cameraDataPath = camerasConfig.get("camera_datas_path").getAsString();
            List<JsonObject> camerasList = gson.fromJson(
                camerasConfig.getAsJsonArray("CamerasConfigurations"),
                new TypeToken<List<JsonObject>>() {}.getType()
            );
            List<CameraService> cameraServices = new ArrayList<>();
            Set<Integer> cameraIds = new HashSet<>();
            for (JsonObject camera : camerasList) {
                int id = camera.get("id").getAsInt();
                if (!cameraIds.add(id)) {
                    throw new IllegalArgumentException("Duplicate camera ID detected: " + id);
                }
                int frequency = camera.get("frequency").getAsInt();
                String key = camera.get("camera_key").getAsString();
                Camera cam = new Camera(id, frequency, key, cameraDataPath);
                cameraServices.add(new CameraService(cam));
            }

            // Parse LiDars
            JsonObject lidarsConfig = config.getAsJsonObject("Lidars");
            String lidarDataPath = lidarsConfig.get("lidars_data_path").getAsString();
            List<JsonObject> lidarsList = gson.fromJson(
                lidarsConfig.getAsJsonArray("LidarConfigurations"),
                new TypeToken<List<JsonObject>>() {}.getType()
            );
            List<LiDarService> lidarServices = new ArrayList<>();
            Set<Integer> lidarIds = new HashSet<>();
            for (JsonObject lidar : lidarsList) {
                int id = lidar.get("id").getAsInt();
                if (!lidarIds.add(id)) {
                    throw new IllegalArgumentException("Duplicate LiDar ID detected: " + id);
                }
                int frequency = lidar.get("frequency").getAsInt();
                LiDarWorkerTracker lidarTracker = new LiDarWorkerTracker(id, frequency, lidarDataPath);
                lidarServices.add(new LiDarService("LiDarService" + id, lidarTracker));
            }

            // Parse Pose data
            String poseDataPath = config.get("poseJsonFile").getAsString();
            GPSIMU gpsimu = new GPSIMU(poseDataPath);
            PoseService poseService = new PoseService(gpsimu);

            // Parse Time configuration
            int tickTime = config.get("TickTime").getAsInt();
            int duration = config.get("Duration").getAsInt();
            TimeService timeService = new TimeService(tickTime, duration);

            // Initialize FusionSlam
            FusionSlam fusionSlam = FusionSlam.getInstance();
            FusionSlamService fusionSlamService = new FusionSlamService(fusionSlam);

            // Update FusionSlam with service count
            fusionSlam.setserviceCounter(
                cameraServices.size() + lidarServices.size() + 2 // +2 for PoseService and FusionSlamService
            );

            // Create executor for threads
            ExecutorService executor = Executors.newFixedThreadPool(
                cameraServices.size() + lidarServices.size() + 3 // +3 for PoseService, FusionSlamService, TimeService
            );

            // Start all services
            cameraServices.forEach(executor::submit);
            lidarServices.forEach(executor::submit);
            executor.submit(poseService);
            executor.submit(fusionSlamService);

            // Start TimeService last
            executor.submit(timeService);

            // Shutdown executor after completion
            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }

            System.out.println("Simulation completed successfully!");

        } catch (IOException e) {
            System.out.println("Error reading configuration file: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("Configuration error: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Simulation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
