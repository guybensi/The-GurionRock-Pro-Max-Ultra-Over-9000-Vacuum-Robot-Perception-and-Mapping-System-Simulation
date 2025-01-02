import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import bgu.spl.mics.application.objects.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class LiDarWorkerTrackerTest {

    private LiDarWorkerTracker lidarWorkerTracker;

    @BeforeEach
    void setup() {
        // אתחול של LiDarWorkerTracker
        lidarWorkerTracker = new LiDarWorkerTracker(1, 10, ""); // לא נשתמש בקובץ, נתיב ריק
    }

    @Test
    void testProcessingEventSuccessfully() {
        // יצירת אובייקט StampedDetectedObject עם אובייקטים תקינים
        List<DetectedObject> detectedObjects = List.of(
            new DetectedObject("Wall_1", "Wall"),
            new DetectedObject("Wall_2", "Wall")
        );
        StampedDetectedObject stampedDetectedObject = new StampedDetectedObject(10, detectedObjects); // זמן 10

        // הפעלת הפונקציה prosseingEvent על אובייקט שהגדרנו
        List<TrackedObject> trackedObjects = lidarWorkerTracker.prosseingEvent(stampedDetectedObject);

        // בדוק אם הרשימה לא ריקה
        assertNotNull(trackedObjects, "Tracked objects should not be null.");
        assertFalse(trackedObjects.isEmpty(), "Tracked objects list should not be empty.");

        // בדוק אם כל אובייקט ברשימה מעובד כראוי
        assertEquals("Wall_1", trackedObjects.get(0).getId(), "The object ID should match.");
        assertEquals(10, trackedObjects.get(0).getTime(), "The object time should match.");
        assertEquals("Wall", trackedObjects.get(0).getDescription(), "The object description should match.");
    }

    @Test
    void testProcessingEventWithError() {
        // יצירת אובייקט StampedDetectedObject עם ID ERROR
        List<DetectedObject> detectedObjects = List.of(
            new DetectedObject("ERROR", "Error")
        );
        StampedDetectedObject stampedDetectedObjectWithError = new StampedDetectedObject(8, detectedObjects); // זמן 8

        // הפעלת הפונקציה prosseingEvent
        lidarWorkerTracker.prosseingEvent(stampedDetectedObjectWithError);

        // בדוק אם הסטטוס השתנה ל-ERROR
        assertEquals(STATUS.ERROR, lidarWorkerTracker.getStatus(), "LiDarWorker status should be set to ERROR.");
    }

    @Test
    void testProcessingEventNoObjectsAtTime() {
        // יצירת אובייקט עם זמן שלא קיים בנתוני ה-LiDar
        StampedDetectedObject stampedDetectedObjectWithInvalidTime = new StampedDetectedObject(20, List.of()); // לא יהיו אובייקטים

        // הפעלת הפונקציה prosseingEvent
        List<TrackedObject> trackedObjects = lidarWorkerTracker.prosseingEvent(stampedDetectedObjectWithInvalidTime);

        // נוודא שהרשימה ריקה במקרה של אובייקטים לא חוקיים
        assertTrue(trackedObjects.isEmpty(), "Tracked objects list should be empty for invalid time.");
    }

    @Test
    void testCoordinatesAreFetchedCorrectly() {
        // יצירת אובייקט עם ID וזמן תקין
        List<DetectedObject> detectedObjects = List.of(
            new DetectedObject("Wall_1", "Wall")
        );
        StampedDetectedObject stampedDetectedObject = new StampedDetectedObject(10, detectedObjects); // זמן 10

        // הפעלת הפונקציה prosseingEvent
        List<TrackedObject> trackedObjects = lidarWorkerTracker.prosseingEvent(stampedDetectedObject);

        // נוודא שהרשימה לא ריקה עבור הקואורדינטות
        assertNotNull(trackedObjects.get(0).getCoordinates(), "Coordinates should not be null.");
        assertFalse(trackedObjects.get(0).getCoordinates().isEmpty(), "Coordinates list should not be empty.");
    }
}
