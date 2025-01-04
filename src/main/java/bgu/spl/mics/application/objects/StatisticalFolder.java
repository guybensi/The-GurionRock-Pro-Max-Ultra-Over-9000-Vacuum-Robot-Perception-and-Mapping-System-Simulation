package bgu.spl.mics.application.objects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import bgu.spl.mics.Event;

public class StatisticalFolder {
  
    // Fields using AtomicInteger for thread-safety
    private AtomicInteger systemRuntime;          // The total runtime of the system (in ticks)
    private AtomicInteger numDetectedObjects;     // The cumulative count of objects detected by all cameras
    private AtomicInteger numTrackedObjects;      // The cumulative count of objects tracked by all LiDAR workers
    private AtomicInteger numLandmarks;           // The total number of unique landmarks identified
    private Map<String, Event<?>> lastFrames;      // A map where the key is the sensor's name and value is the last frame it sent

    // Constructor
    public StatisticalFolder() {
        this.systemRuntime = new AtomicInteger(0);
        this.numDetectedObjects = new AtomicInteger(0);
        this.numTrackedObjects = new AtomicInteger(0);
        this.numLandmarks = new AtomicInteger(0);
        this.lastFrames = new ConcurrentHashMap<>();
    }
    // Singleton Holder for thread-safe מימוש כמו בכיתה
    private static class SingletonHolderStatisticalFolder {
        private static final StatisticalFolder INSTANCE = new StatisticalFolder();
    }

    public static StatisticalFolder getInstance() {
        return SingletonHolderStatisticalFolder.INSTANCE;
    }
    
    // Getters
    public int getSystemRuntime() {
        return systemRuntime.get();   
    }

    public int getNumDetectedObjects() {
        return numDetectedObjects.get();
    }

    public int getNumTrackedObjects() {
        return numTrackedObjects.get();
    }

    public int getNumLandmarks() {
        return numLandmarks.get();
    }

    // Methods to update the statistics
    public void updateSystemRuntime(int timeTick) {
        this.systemRuntime.addAndGet(timeTick); // Increment system runtime by the time tick
    }

    public void updateNumDetectedObjects(int detectedObjectsCount) {
        this.numDetectedObjects.addAndGet(detectedObjectsCount); // Increment detected objects count
    }

    public void updateNumTrackedObjects(int trackedObjectsCount) {
        this.numTrackedObjects.addAndGet(trackedObjectsCount); // Increment tracked objects count
    }

    public void updateNumLandmarks(int newLandmarksCount) {
        this.numLandmarks.addAndGet(newLandmarksCount); // Increment landmarks count
    }
   // Method to handle and update the last frame for a specific sensor
    public void updateLastFrame(String name, Event<?> event) {
        lastFrames.put(name, event);
    }

    // Method to get the last frame sent by each sensor
    public Map<String, Event<?>> getLastFrames() {
        return lastFrames;
    }
}