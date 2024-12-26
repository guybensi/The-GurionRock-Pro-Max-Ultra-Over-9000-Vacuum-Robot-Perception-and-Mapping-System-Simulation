package bgu.spl.mics.application.objects;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */
public class Camera {

    private int id;
    private int frequency;
    private STATUS status;  // The status of the camera (UP, DOWN, ERROR)
    private List<StampedDetectedObject> detectedObjectsList; // צריך ליצור מחלקה כזאת

    // Constructor for Camera.
    public Camera(int id, int frequency, String filePath, String cameraKey) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        loadDetectedObjectsFromFile(filePath, cameraKey);
    }


    // Getters and setters for the fields.
    public int getId() {
        return id;
    }

    public int getFrequency() {
        return frequency;
    }

    public STATUS getStatus() {
        return status;
    }

    public List<StampedDetectedObject> getDetectedObjectsList() {
        return detectedObjectsList;
    }

    // Method to simulate the camera detecting an object and adding it to the list.
    public void detectObject(StampedDetectedObject detectedObject) {
        detectedObjectsList.add(detectedObject);
    }

    public StampedDetectedObject getDetectedObjectsAtTime(int time) {
        for (StampedDetectedObject stampedObject : detectedObjectsList) {
            if (stampedObject.getTime() == time) {
                return stampedObject;
            }
        }
        return null; // Return an empty list if no objects were detected at the given time
    }

    /**
     * Reads detected objects from a JSON file and fills the detectedObjectsList.
     *
     * @param filePath The path to the JSON file.
     */
    public void loadDetectedObjectsFromFile(String filePath, String cameraKey) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            // Read the entire JSON file as a map of camera keys to their data
            java.lang.reflect.Type type = new TypeToken<Map<String, List<List<StampedDetectedObject>>>>() {}.getType();
            Map<String, List<List<StampedDetectedObject>>> cameraData = gson.fromJson(reader, type);

            // Get the data for the specified camera key
            List<List<StampedDetectedObject>> cameraObjects = cameraData.get(cameraKey);

            if (cameraObjects != null) {
                // Flatten the list of lists into a single list of StampedDetectedObject
                detectedObjectsList = new ArrayList<>();
                for (List<StampedDetectedObject> objectsAtTime : cameraObjects) {
                    detectedObjectsList.addAll(objectsAtTime);
                }
            } else {
                detectedObjectsList = new ArrayList<>(); // No data for this camera, initialize empty list
            }
        } catch (IOException ignored) {
            detectedObjectsList = new ArrayList<>(); // On error, initialize empty list
        }
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }
}
