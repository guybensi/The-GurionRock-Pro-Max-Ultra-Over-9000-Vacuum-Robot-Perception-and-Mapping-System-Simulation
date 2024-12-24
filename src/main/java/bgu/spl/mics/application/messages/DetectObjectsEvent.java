package bgu.spl.mics.application.messages;
import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedCloudPoints;

import java.util.List;

public class DetectObjectsEvent implements Event<StampedCloudPoints> {
    private final StampedDetectedObjects stampedDetectedObjects;

    public DetectObjectsEvent(StampedDetectedObjects stampedDetectedObjects) {
        this.stampedDetectedObjects = stampedDetectedObjects;
    }

    public StampedDetectedObjects getStampedDetectedObjects() {
        return stampedDetectedObjects;
}//יש לנו את זה בעבודה רק צריך לממש את המחלקה אל תבהלו 