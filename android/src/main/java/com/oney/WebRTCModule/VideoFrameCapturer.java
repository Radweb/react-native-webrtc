package com.oney.WebRTCModule;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.webrtc.JavaI420Buffer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.YuvConverter;
import org.webrtc.YuvHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.UUID;

public class VideoFrameCapturer implements VideoSink {

    private VideoFrame.I420Buffer i420Buffer;
    private int lastFrameRotation = 0;
    private OrientationEventListener orientationEventListener;
    private int deviceOrientation = 0;

    public VideoFrameCapturer(Context context) {
        this.orientationEventListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == -1) {
                    deviceOrientation = 0;
                } else if (orientation > 355 || orientation < 5) {
                    deviceOrientation = 0;
                } else if (orientation > 85 && orientation < 95) {
                    deviceOrientation = 90;
                } else if (orientation > 175 && orientation < 185) {
                    deviceOrientation = 0;
                } else if (orientation > 265 && orientation < 275) {
                    deviceOrientation = -90;
                }
            }
        };

        if (this.orientationEventListener.canDetectOrientation()) {
            this.orientationEventListener.enable();
        } else {
            this.orientationEventListener.disable();
        }
    }

    @Override
    public void onFrame(VideoFrame frame) {
        synchronized (this) {
            this.i420Buffer = frame.getBuffer().toI420();
            this.lastFrameRotation = frame.getRotation();
        }
    }

    public File saveLastFrameToFile(Context context) throws IOException {
        synchronized (this) {
            if (this.i420Buffer == null) {
                throw new IOException("Frame does not exist");
            }

            int width = this.i420Buffer.getWidth();
            int height = this.i420Buffer.getHeight();

            YuvImage yuvImage = new YuvImage(this.createNV21Data(this.i420Buffer), ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 90, byteArrayOutputStream);

            String originalUuid = UUID.randomUUID().toString() + ".jpg";
            FileOutputStream fileOutputStream = context.openFileOutput(originalUuid, Context.MODE_PRIVATE);
            byteArrayOutputStream.writeTo(fileOutputStream);

            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();

            String rotatedUuid = UUID.randomUUID().toString() + ".jpg";
            File rotatedFile = context.getFileStreamPath(rotatedUuid);

            FileInputStream rotationInputStream = context.openFileInput(originalUuid);
            FileOutputStream rotationOutputStream = context.openFileOutput(rotatedUuid, Context.MODE_PRIVATE);
            this.rotateImage(rotationInputStream, rotationOutputStream, this.lastFrameRotation);

            rotationInputStream.close();
            rotationOutputStream.flush();
            rotationOutputStream.close();

            context.deleteFile(originalUuid);

            return rotatedFile;
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

    private void rotateImage(FileInputStream fileInputStream, FileOutputStream fileOutputStream, int frameRotation) {
        Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);

        Matrix matrix = new Matrix();
        matrix.postRotate(this.calculateImageRotation(frameRotation));

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
    }

    public int calculateImageRotation(int frameRotation) {
        switch (this.deviceOrientation) {
            case 90:
                return 180;
            case -90:
                return 0;
            default:
                return 90;
        }
    }

    public int getScreenOrientation(Context context)
    {
        return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
    }
}
