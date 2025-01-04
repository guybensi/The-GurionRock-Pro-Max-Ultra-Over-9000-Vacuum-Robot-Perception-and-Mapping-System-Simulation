package bgu.spl.mics.application.objects;

import java.io.File;
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
    private STATUS status; 
    private List<StampedDetectedObject> detectedObjectsList; 
    private int maxTime;
    private String errMString;

    public Camera(int id, int frequency, String filePath, String cameraKey) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.errMString = null;
        this.detectedObjectsList = new ArrayList<>(); // יוזמה של רשימה ריקה
        loadDetectedObjectsFromFile(filePath, cameraKey);
        if (!detectedObjectsList.isEmpty()) {
            this.maxTime = detectedObjectsList.stream()
                              .mapToInt(StampedDetectedObject::getTime)
                              .max()
                              .orElse(4);
        } else {
            this.maxTime = 3; // ברירת מחדל במידה ולא נטענו נתונים
        }
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
    public String getErrMString() {
        return errMString;
    }


    public List<StampedDetectedObject> getDetectedObjectsList() {
        return detectedObjectsList;
    }

    public StampedDetectedObject getDetectedObjectsAtTime(int time) {
        checkIfDone(time);
        for (StampedDetectedObject stampedObject : detectedObjectsList) {
            if (stampedObject.getTime() == time) {
                for (DetectedObject obj : stampedObject.getDetectedObjects()) {
                    if ("ERROR".equals(obj.getId())) {
                        errMString = obj.getDescription();
                        setStatus(STATUS.ERROR); 
                        break; 
                    }
                }
                return stampedObject;
            }
        }
        return null; 
    }

    public void loadDetectedObjectsFromFile(String filePath, String cameraKey) {
        try (FileReader reader = new FileReader(filePath)) {
            System.out.println("Camera attempting to read file: " + new File(filePath).getAbsolutePath());
            Gson gson = new Gson();
    
            // קריאת הנתונים מהקובץ
            java.lang.reflect.Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> cameraData = gson.fromJson(reader, type);
    
            // בדיקת קיום המפתח עבור המצלמה
            if (!cameraData.containsKey(cameraKey)) {
                System.err.println("Camera key '" + cameraKey + "' not found in file.");
                detectedObjectsList = new ArrayList<>();
                return;
            }
    
            Object rawData = cameraData.get(cameraKey);
    
            if (rawData instanceof List) {
                // בדיקה האם מדובר ברשימה שטוחה או מקוננת
                List<?> dataList = (List<?>) rawData;
    
                if (!dataList.isEmpty() && dataList.get(0) instanceof List) {
                    // רשימה מקוננת
                    List<List<StampedDetectedObject>> nestedList = gson.fromJson(
                        gson.toJson(dataList),
                        new TypeToken<List<List<StampedDetectedObject>>>() {}.getType()
                    );
    
                    detectedObjectsList = new ArrayList<>();
                    for (List<StampedDetectedObject> innerList : nestedList) {
                        detectedObjectsList.addAll(innerList);
                    }
    
                } else {
                    // רשימה שטוחה
                    detectedObjectsList = gson.fromJson(
                        gson.toJson(dataList),
                        new TypeToken<List<StampedDetectedObject>>() {}.getType()
                    );
                }
    
                // חישוב הזמן המקסימלי
                maxTime = detectedObjectsList.stream()
                                             .mapToInt(StampedDetectedObject::getTime)
                                             .max()
                                             .orElse(0);
    
                System.out.println("Camera " + id + " successfully loaded " + detectedObjectsList.size() + " detected objects.");
            } else {
                System.err.println("Camera key '" + cameraKey + "' contains invalid data structure.");
                detectedObjectsList = new ArrayList<>();
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            detectedObjectsList = new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Unexpected error while processing file: " + e.getMessage());
            e.printStackTrace();
            detectedObjectsList = new ArrayList<>();
        }
    
        // הדפסת הנתונים שנטענו
        if (!detectedObjectsList.isEmpty()) {
            System.out.println("Camera " + id + " detected objects:");
            for (StampedDetectedObject obj : detectedObjectsList) {
                System.out.println("  Time: " + obj.getTime() + ", Objects: " + obj.getDetectedObjects());
            }
        } else {
            System.err.println("Camera " + id + " detectedObjectsList is empty.");
        }
    }
    
    

    public void checkIfDone(int currentTime) {
        if (currentTime >= this.maxTime) {
            setStatus(STATUS.DOWN);
        }
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }
}