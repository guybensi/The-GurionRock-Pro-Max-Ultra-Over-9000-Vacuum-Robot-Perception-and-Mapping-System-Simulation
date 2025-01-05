import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObject;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class CameraTest {

    private Camera camera;

    @BeforeEach
    void setup() {
        java.net.URL resource = getClass().getClassLoader().getResource("camera_example_input.json");
        assertNotNull(resource, "The file 'example_input.json' was not found in src/test/resources.");
        System.out.println("File found: " + resource.getPath());

        try {
            String filePath = Paths.get(resource.toURI()).toString();
            camera = new Camera(1, 5, filePath, "camera1");
        } catch (Exception e) {
            fail("Failed to initialize camera: " + e.getMessage());
        }
    }

    @Test
    void testGetDetectedObjectsAtValidTimeWithoutError() {
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(13);
        assertNotNull(result, "The result should not be null for valid time.");
        assertEquals(13, result.getTime(), "The returned time should match the requested time.");
        assertEquals(2, result.getDetectedObjects().size(), "The detected objects list size should be correct.");
        assertEquals("Wall_1", result.getDetectedObjects().get(0).getId(), "The object ID should match.");
        assertEquals("Wall", result.getDetectedObjects().get(0).getDescription(), "The object description should match.");
    }

    @Test
    void testGetDetectedObjectsAtValidTimeWithError() {
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(8);
        assertNotNull(result, "The result should not be null for valid time.");
        assertEquals(8, result.getTime(), "The returned time should match the requested time.");
        assertEquals(3, result.getDetectedObjects().size(), "The detected objects list size should be correct.");

        assertEquals(STATUS.ERROR, camera.getStatus(), "Camera status should be set to ERROR.");
        assertEquals(
            "GLaDOS has repurposed the robot to conduct endless cake-fetching tests. Success is a lie.",
            camera.getErrMString(),
            "Error message string should match the detected object's description."
        );
    }

    @Test
    void testGetDetectedObjectsAtInvalidTime() {
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(20);
        assertNull(result, "The result should be null for a time with no detected objects.");
    }

    @Test
    void testCheckIfDoneSetsStatusToDown() {
        camera.getDetectedObjectsAtTime(12);
        assertEquals(STATUS.UP, camera.getStatus(), "for time 12, Camera status should remain UP for valid time.");

        camera.getDetectedObjectsAtTime(20);
        assertEquals(STATUS.DOWN, camera.getStatus(), "for time 20, Camera status should be set to DOWN after maxTime.");
    }


    @Test
    void testLoadEmptyJsonFile() {
        java.net.URL resource = getClass().getClassLoader().getResource("empty_camera_data.json");
        assertNotNull(resource, "The file 'empty_camera_data.json' was not found in src/test/resources.");

        try {
            String filePath = Paths.get(resource.toURI()).toString();
            Camera emptyCamera = new Camera(1, 5, filePath, "camera1");
            assertTrue(emptyCamera.getDetectedObjectsList().isEmpty(), "Detected objects list should be empty for an empty JSON file.");
        } catch (Exception e) {
            fail("Unexpected exception while loading empty JSON: " + e.getMessage());
        }
    }

    @Test
    void testLoadSpecificCameraKey() {
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(3);
        assertNotNull(result, "Detected objects for camera1 should not be null.");
        assertEquals("Wall_3", result.getDetectedObjects().get(0).getId(), "The object ID should match.");

        java.net.URL resource = getClass().getClassLoader().getResource("camera_example_input.json");
        assertNotNull(resource, "The file 'camera_example_input.json' was not found in src/test/resources.");
        try {
            String filePath = Paths.get(resource.toURI()).toString();
            Camera secondCamera = new Camera(2, 5, filePath, "camera2");
            StampedDetectedObject secondResult = secondCamera.getDetectedObjectsAtTime(3);
            assertNotNull(secondResult, "Detected objects for camera2 should not be null.");
            assertEquals("Wall_1", secondResult.getDetectedObjects().get(0).getId(), "The object ID should match for camera2.");
        } catch (Exception e) {
            fail("Unexpected exception while loading camera2: " + e.getMessage());
        }
    }

    @Test
    void testEmptyDetectedObjectsAtValidTime() {
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(12);
        assertNotNull(result, "The result should not be null even if there are no detected objects.");
        assertTrue(result.getDetectedObjects().isEmpty(), "The detected objects list should be empty.");
    }

    @Test
    void testCameraFrequency() {
        assertEquals(5, camera.getFrequency(), "Camera frequency should be initialized correctly.");
    }
}
