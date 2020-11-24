package com.oney.WebRTCModule;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

public class VideoFrameCapturer implements VideoSink {

    private VideoFrame lastFrame;

    @Override
    public void onFrame(VideoFrame frame) {
        synchronized (this) {
            this.lastFrame = frame;
        }
    }

    public VideoFrame getLastFrame() {
        return lastFrame;
    }
}
