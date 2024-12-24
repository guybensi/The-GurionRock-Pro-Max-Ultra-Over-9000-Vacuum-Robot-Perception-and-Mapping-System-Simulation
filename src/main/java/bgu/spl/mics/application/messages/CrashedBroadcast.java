package bgu.spl.mics.application.messages;
import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast {
    private final String errorMessage;

    public CrashedBroadcast(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}