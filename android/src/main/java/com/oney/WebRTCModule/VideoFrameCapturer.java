package com.oney.WebRTCModule;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import org.webrtc.JavaI420Buffer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.YuvConverter;
import org.webrtc.YuvHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.UUID;

public class VideoFrameCapturer implements VideoSink {

    private VideoFrame lastFrame;

    @Override
    public void onFrame(VideoFrame frame) {
        synchronized (this) {
            try {
                this.lastFrame.release();
            } catch (Exception e) {
                // Something has already called release before us, not sure where or how
            }
            this.lastFrame = new VideoFrame(frame.getBuffer(), frame.getRotation(), frame.getTimestampNs());
        }
    }

    public File saveLastFrameToFile(Context context) throws Exception {
        synchronized (this) {
            if (this.lastFrame == null) {
                throw new Exception("Frame does not exist");
            }

            VideoFrame.Buffer buffer = this.lastFrame.getBuffer();
            VideoFrame.I420Buffer i420Buffer = buffer.toI420();

            int width = i420Buffer.getWidth();
            int height = i420Buffer.getHeight();

            YuvImage yuvImage = new YuvImage(this.createNV21Data(i420Buffer), ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, byteArrayOutputStream);

            String uuid = UUID.randomUUID().toString() + ".jpg";
            File file = context.getFileStreamPath(uuid);
            FileOutputStream fileOutputStream = context.openFileOutput(uuid, Context.MODE_PRIVATE);
            byteArrayOutputStream.writeTo(fileOutputStream);

            fileOutputStream.close();
            byteArrayOutputStream.close();

            return file;
        }
    }

    private byte[] createNV21Data(VideoFrame.I420Buffer buffer) {
        ByteBuffer y = buffer.getDataY();
        ByteBuffer u = buffer.getDataU();
        ByteBuffer v = buffer.getDataV();
        int width = buffer.getWidth();
        int height = buffer.getHeight();
        final int chromaWidth = (width + 1) / 2;
        final int chromaHeight = (height + 1) / 2;
        final int minSize = width * height + chromaWidth * chromaHeight * 2;
        ByteBuffer yuvBuffer = ByteBuffer.allocateDirect(minSize);
        YuvHelper.I420ToNV12(y, buffer.getStrideY(), v, buffer.getStrideV(), u, buffer.getStrideU(), yuvBuffer, width, height);
        return yuvBuffer.array();
    }
}
