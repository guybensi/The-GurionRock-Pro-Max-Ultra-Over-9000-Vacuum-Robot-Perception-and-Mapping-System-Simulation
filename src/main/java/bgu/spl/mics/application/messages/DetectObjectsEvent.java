package bgu.spl.mics.application.messages;
import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedCloudPoints;
import bgu.spl.mics.application.objects.StampedDetectedObject;

import java.util.List;

public class DetectObjectsEvent implements Event<StampedCloudPoints> {
    private final StampedDetectedObject stampedDetectedObjects;

    public DetectObjectsEvent(StampedDetectedObject stampedDetectedObjects) {
        this.stampedDetectedObjects = stampedDetectedObjects;
    }

    public StampedDetectedObject getStampedDetectedObjects() {
        return stampedDetectedObjects;
}//יש לנו את זה בעבודה רק צריך לממש את המחלקה אל תבהלו 