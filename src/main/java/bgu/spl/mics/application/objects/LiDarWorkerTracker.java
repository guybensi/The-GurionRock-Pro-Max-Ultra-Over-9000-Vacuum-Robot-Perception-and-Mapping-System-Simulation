package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a LiDar worker on the robot.
 * Responsible for tracking objects in the environment at regular intervals.
 */
public class LiDarWorkerTracker {

    private int id;
    private int frequency;
    private STATUS status;
    private List<TrackedObject> lastTrackedObjects;
    private LiDarDataBase liDarDataBase; // Instance of LiDarDataBase

    // Constructor to initialize the LiDarWorkerTracker object.
    public LiDarWorkerTracker(int id, int frequency, String lidarDataFilePath) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ArrayList<>();
        this.liDarDataBase = LiDarDataBase.getInstance(lidarDataFilePath); // Initialize the singleton instance
    }


    public int getId() {
        return id;
    }

    public int getFrequency() {
        return frequency;
    }

    public STATUS getStatus() {
        return status;
    }
    public void setStatus(STATUS status) {
        this.status = status;
    }

    public List<TrackedObject> getLastTrackedObjects() {
        return lastTrackedObjects;
    }

    public List<StampedCloudPoints> getLiDarData() {
        return liDarDataBase.getCloudPoints();
    }
    public List<CloudPoint> getCoordinates(String id, int time) {
        List<StampedCloudPoints> cloudPointsList = liDarDataBase.getCloudPoints();
        for (StampedCloudPoints stampedCloudPoints : cloudPointsList) {
            if (stampedCloudPoints.getId().equals(id) && stampedCloudPoints.getTime() == time) {
                return stampedCloudPoints.getCloudPoints();
            }
        }
        return new ArrayList<>(); // Return an empty list if no match is found
    }
//---------------------------פונקציה שמחזירה מידע לסרוויס
}
