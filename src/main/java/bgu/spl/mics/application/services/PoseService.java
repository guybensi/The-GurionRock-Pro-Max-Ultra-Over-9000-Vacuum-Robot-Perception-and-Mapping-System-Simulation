package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;

/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {

    private final GPSIMU gpsimu;

    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu) {
        super("PoseService");
        this.gpsimu = gpsimu;
    }

    /**
     * Initializes the PoseService.
     * Subscribes to TickBroadcast and sends PoseEvents at every tick based on the current pose.
     */
    @Override
    protected void initialize() {
        // Subscribe to TickBroadcast to handle ticks
        subscribeBroadcast(TickBroadcast.class, tick -> {
            gpsimu.SetTick(tick.getTime());
            System.out.println(getName() + ": got a tick and the tick is, " + tick.getTime());
            if (gpsimu.getStatus() == STATUS.UP) {
                Pose currentPose = gpsimu.getPoseAtTime();
                if (currentPose != null) {
                    // Broadcast PoseEvent with the current pose and sender name
                    System.out.println(getName() + ": sent PoseEvent at time"+ tick.getTime() );
                    sendEvent(new PoseEvent(currentPose, getName()));
                }
                if (gpsimu.getStatus() == STATUS.DOWN){
                    System.out.println(getName() + ": is down and terminated");
                    terminate();
                    sendBroadcast(new TerminatedBroadcast(getName())); 
                }
            } else {
                System.out.println(getName() + ": is down2 and terminated");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));     
            }
        });
        //--------------------------------------לבדוק------------------------------------------------------------
        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
            if (broadcast.getSenderId() == "TimeService"){
                System.out.println(getName() + ": got TerminatedBroadcast from TimeService");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));  
            }
        });
        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
            System.out.println(getName() + ": got crashed");
            terminate();
        });
    }
    
}