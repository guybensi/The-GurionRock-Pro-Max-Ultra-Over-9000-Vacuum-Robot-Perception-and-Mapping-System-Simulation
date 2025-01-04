package bgu.spl.mics.application.objects;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import bgu.spl.mics.Event;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam {
    private static class SingletonHolderFusionSlam {// מימוש כמו שהוצג בכיתה
         private static final FusionSlam INSTANCE = new FusionSlam();
    }
    public static FusionSlam getInstance() {
        return SingletonHolderFusionSlam.INSTANCE;
    }
    
    private ArrayList<LandMark> landmarks  = new ArrayList<>(); // Dynamic array of landmarks
    private Map<Integer, Pose> posesByTime = new HashMap<>(); // Map to hold poses by time
    private int serviceCounter = 0;
    private int tick = 0;
    
    
    /**
     * Updates the current pose of the robot.
     *
     * @param pose The new pose.
     */
    public void addPose(Pose pose) {
        posesByTime.put(pose.getTime(), pose); // Add the pose to the map

    }
    
    public Pose getPoseAtTime(int time) {
        return posesByTime.get(time);
    }
    public void setTick (int time){
        this.tick = time;
    }
    public int getTick (){
        return tick;
    }

    public void setserviceCounter(int count){
        this.serviceCounter = count;
    }

    /**
     * Processes a tracked object event to update or add landmarks.
     *
     * @param trackedObjects The list of tracked objects.
     */
    public synchronized void processTrackedObjects(List<TrackedObject> trackedObjects) {
        for (TrackedObject obj : trackedObjects) {
            String id = obj.getId();
            Pose relaventPose = getPoseAtTime(obj.getTime());
            if (relaventPose == null) {// לא אמור לקרות, רק בשביל הבדיקות
                System.out.println("No pose found for time: " + obj.getTime() + ". Skipping object: " + id);
                continue; // דילוג על האובייקט אם אין פוזה מתאימה
            }
            List<CloudPoint> globalCoordinates = transformToGlobal(obj.getCoordinates(), relaventPose);
            LandMark existingLandmark = findLandMarkById(id);
            if (existingLandmark != null) {
                // Update existing landmark by averaging coordinates
                updateLandmarkCoordinates(existingLandmark, globalCoordinates);
            } else {
                // Add new landmark
                LandMark newLandmark = new LandMark(id, obj.getDescription(), globalCoordinates);
                landmarks.add(newLandmark);
                StatisticalFolder.getInstance().updateNumLandmarks(1);
            }     
        }
         
    }
    /**
     * Updates the coordinates of an existing landmark by averaging the new coordinates with the old ones.
     *
     * @param existingLandmark The existing landmark to update.
     * @param newCoordinates   The new coordinates to average with the old ones.
     */
    private void updateLandmarkCoordinates(LandMark existingLandmark, List<CloudPoint> newCoordinates) {
        List<CloudPoint> existingCoordinates = existingLandmark.getCoordinates();
        List<CloudPoint> updatedCoordinates = new ArrayList<>();

        int size = Math.min(existingCoordinates.size(), newCoordinates.size());
        for (int i = 0; i < size; i++) {
            CloudPoint oldPoint = existingCoordinates.get(i);
            CloudPoint newPoint = newCoordinates.get(i);

            double avgX = (oldPoint.getX() + newPoint.getX()) / 2;
            double avgY = (oldPoint.getY() + newPoint.getY()) / 2;

            updatedCoordinates.add(new CloudPoint(avgX, avgY));
        }

        // Add any remaining points from newCoordinates
        if (newCoordinates.size() > existingCoordinates.size()) {
            updatedCoordinates.addAll(newCoordinates.subList(size, newCoordinates.size()));
        }

        // Add any remaining points from existingCoordinates
        if (existingCoordinates.size() > newCoordinates.size()) {
            updatedCoordinates.addAll(existingCoordinates.subList(size, existingCoordinates.size()));
        }
        existingLandmark.setCoordinates(updatedCoordinates);
    }


    /**
     * Finds a landmark by its ID.
     *
     * @param id The ID of the landmark.
     * @return The LandMark object if found, or null otherwise.
     */
    private LandMark findLandMarkById(String id) {
        for (LandMark landmark : landmarks) {
            if (landmark.getId().equals(id)) {
                return landmark;
            }
        }
        return null;
    }

    /**
     * Transforms local coordinates to global coordinates based on the robot's pose.
     *
     * @param localCoordinates The local coordinates to transform.
     * @param pose             The current pose of the robot.
     * @return A list of transformed global coordinates.
     */
    public List<CloudPoint> transformToGlobal(List<CloudPoint> localCoordinates, Pose pose) {
        List<CloudPoint> globalCoordinates = new ArrayList<>();
    
        double yawRadians = Math.toRadians(pose.getYaw());
        double cosYaw = Math.cos(yawRadians);
        double sinYaw = Math.sin(yawRadians);
    
        for (CloudPoint point : localCoordinates) {
            // חישוב הקואורדינטות הגלובליות
            double globalX = point.getX() * cosYaw - point.getY() * sinYaw + pose.getX();
            double globalY = point.getX() * sinYaw + point.getY() * cosYaw + pose.getY();
    
            // הוספת הנקודה המחושבת
            globalCoordinates.add(new CloudPoint(globalX, globalY));
        }
    
        return globalCoordinates;
    }
    

    /**
     * Returns the list of landmarks.
     *
     * @return A list of landmarks.
     */
    public List<LandMark> getLandmarks() {
        return Collections.unmodifiableList(new ArrayList<>(landmarks));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FusionSlam Map:\n");
        for (LandMark landmark : landmarks) {
            sb.append(landmark).append("\n");
        }
        return sb.toString();
    }
    public boolean isTerminated (){
        return (serviceCounter <= 0);
    }
    public int getserviceCounter(){
        return serviceCounter;
    }
    public void increasServiceCounter(){
        this.serviceCounter++;
    }
    public void decreaseServiceCounter() {
        this.serviceCounter--;
    } 
  
    public void generateOutputFileWithoutError(String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> outputData = new HashMap<>();
    
        // Add statistics
        StatisticalFolder stats = StatisticalFolder.getInstance();
        Map<String, Object> statistics = Map.of(
            "systemRuntime", stats.getSystemRuntime(),
            "numDetectedObjects", stats.getNumDetectedObjects(),
            "numTrackedObjects", stats.getNumTrackedObjects(),
            "numLandmarks", stats.getNumLandmarks()
        );
        outputData.put("statistics", statistics);
    
        // Add landmarks
        Map<String, Object> landmarks = new HashMap<>();
        for (LandMark landmark : getLandmarks()) {
            landmarks.put(landmark.getId(), landmark); // Gson will handle serialization
        }
        outputData.put("landMarks", landmarks);
    
        // Add poses
        outputData.put("poses", getAllPoses()); // Gson will serialize the list
    
        // Write JSON to file
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(outputData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public void generateOutputFileWithError(String filePath, String errorDescription, String faultySensor) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> outputData = new HashMap<>();
    
        // Add error-specific fields
        outputData.put("error", errorDescription);
        outputData.put("faultySensor", faultySensor);
    
        // Add last frames
        Map<String, Event<?>> lastFrames = StatisticalFolder.getInstance().getLastFrames();
        outputData.put("lastFrames", lastFrames);
    
        // Add poses
        outputData.put("poses", getAllPoses());
    
        // Add statistics
        StatisticalFolder stats = StatisticalFolder.getInstance();
        Map<String, Object> statistics = Map.of(
            "systemRuntime", stats.getSystemRuntime(),
            "numDetectedObjects", stats.getNumDetectedObjects(),
            "numTrackedObjects", stats.getNumTrackedObjects(),
            "numLandmarks", stats.getNumLandmarks()
        );
        outputData.put("statistics", statistics);
    
        // Add landmarks
        Map<String, Object> landmarks = new HashMap<>();
        for (LandMark landmark : getLandmarks()) {
            landmarks.put(landmark.getId(), landmark);
        }
        outputData.put("landMarks", landmarks);
    
        // Write JSON to file
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(outputData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public List<Pose> getAllPoses() {
        return new ArrayList<>(posesByTime.values());
    }
    public List<Pose> getPosesUpToTick(int time) {
        List<Pose> poses = new ArrayList<>();
        for (Map.Entry<Integer, Pose> entry : posesByTime.entrySet()) {
            if (entry.getKey() <= time) {
                poses.add(entry.getValue());
            } else {
                break;
            }
        }
        return poses;
    }
    
    
//-------------------פונקציות לבדיקות
    public void clearLandmarks() {
        landmarks.clear();
    }

    public void addLandmark(LandMark landmark) {
        landmarks.add(landmark);
    }
    public List<LandMark> getLandmarksMod() {
        return new ArrayList<>(landmarks); 
    }
    

}