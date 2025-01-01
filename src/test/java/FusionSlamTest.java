import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FusionSlamTest {

    private FusionSlam fusionSlam;

    @BeforeEach
    void setup() {
        fusionSlam = FusionSlam.getInstance();
        fusionSlam.clearLandmarks(); // ניקוי ה־landmarks במצב הנכון
    }

    @Test
    void testGlobalTransformation() {
        // בדיקה להמרה לגלובלי
        Pose pose = new Pose(1, 1.0f, 1.0f, 45.0f);
        List<CloudPoint> localCoordinates = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0)
        );

        List<CloudPoint> globalCoordinates = fusionSlam.transformToGlobal(localCoordinates, pose);

        assertNotNull(globalCoordinates, "Global transformation result should not be null.");
        assertEquals(2, globalCoordinates.size(), "The number of global coordinates should match the local coordinates.");
        // בדיקות נוספות לתוצאה אם נדרשות
    }

    @Test
    void testAddNewLandmark() {
        Pose pose = new Pose(1, 1.0f, 0.0f, 1.0f);
        fusionSlam.addPose(pose);

        List<CloudPoint> coordinates = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0)
        );

        TrackedObject trackedObject = new TrackedObject("Landmark1", 1, "Test Landmark", coordinates);
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        List<LandMark> landmarks = fusionSlam.getLandmarksMod();
        assertEquals(1, landmarks.size(), "A new landmark should have been added.");
        assertEquals("Landmark1", landmarks.get(0).getId(), "Landmark ID should match.");
        assertEquals(2, landmarks.get(0).getCoordinates().size(), "Landmark should have correct number of coordinates.");
    }

    @Test
    void testUpdateExistingLandmark() {
        Pose pose = new Pose(1, 1.0f, 0.0f, 1.0f);
        fusionSlam.addPose(pose);

        List<CloudPoint> initialCoordinates = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0)
        );

        LandMark existingLandmark = new LandMark("Landmark1", "Test Landmark", initialCoordinates);
        fusionSlam.addLandmark(existingLandmark);

        List<CloudPoint> newCoordinates = Arrays.asList(
                new CloudPoint(3.0, 3.0),
                new CloudPoint(4.0, 4.0)
        );

        TrackedObject trackedObject = new TrackedObject("Landmark1", 1, "Test Landmark", newCoordinates);
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        List<LandMark> landmarks = fusionSlam.getLandmarksMod();
        assertEquals(1, landmarks.size(), "There should still be only one landmark.");
        assertEquals("Landmark1", landmarks.get(0).getId(), "Landmark ID should match.");
        assertEquals(2, landmarks.get(0).getCoordinates().size(), "Landmark should still have the same number of coordinates.");
    }

    @Test
    void testProcessTrackedObjectsWithNoPose() {
        List<CloudPoint> coordinates = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0)
        );

        TrackedObject trackedObject = new TrackedObject("Landmark1", 2, "Test Landmark", coordinates);
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        List<LandMark> landmarks = fusionSlam.getLandmarksMod();
        assertEquals(0, landmarks.size(), "No landmark should be added if there is no pose.");
    }

    @Test
    void testProcessTrackedObjectsWithMultipleObjects() {
        Pose pose = new Pose(1, 1.0f, 45.0f, 1.0f);
        fusionSlam.addPose(pose);

        List<CloudPoint> coordinates1 = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0)
        );

        List<CloudPoint> coordinates2 = Arrays.asList(
                new CloudPoint(3.0, 3.0),
                new CloudPoint(4.0, 4.0)
        );

        TrackedObject trackedObject1 = new TrackedObject("Landmark1", 1, "First Landmark", coordinates1);
        TrackedObject trackedObject2 = new TrackedObject("Landmark2", 1, "Second Landmark", coordinates2);

        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject1, trackedObject2));

        List<LandMark> landmarks = fusionSlam.getLandmarksMod();
        assertEquals(2, landmarks.size(), "Two landmarks should be added.");
        assertEquals("Landmark1", landmarks.get(0).getId(), "First landmark ID should match.");
        assertEquals("Landmark2", landmarks.get(1).getId(), "Second landmark ID should match.");
    }
}
