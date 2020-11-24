package com.oney.WebRTCModule;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import org.webrtc.JavaI420Buffer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

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
            this.lastFrame = frame;
        }
    }

    public VideoFrame getLastFrame() {
        return lastFrame;
    }

    public File saveLastFrameToFile(Context context) throws IOException {
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

    private byte[] createNV21Data(VideoFrame.I420Buffer buffer) {
        final int width = buffer.getWidth();
        final int height = buffer.getHeight();
        final int chromaStride = width;
        final int chromaWidth = (width + 1) / 2;
        final int chromaHeight = (height + 1) / 2;
        final int ySize = width * height;
        final ByteBuffer nv21Buffer = ByteBuffer.allocateDirect(ySize + chromaStride * chromaHeight);
        // We don't care what the array offset is since we only want an array that is direct.
        final byte[] nv21Data = nv21Buffer.array();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                final byte yValue = buffer.getDataY().get(y * buffer.getStrideY() + x);
                nv21Data[y * width + x] = yValue;
            }
        }
        for (int y = 0; y < chromaHeight; ++y) {
            for (int x = 0; x < chromaWidth; ++x) {
                final byte uValue = buffer.getDataU().get(y * buffer.getStrideU() + x);
                final byte vValue = buffer.getDataV().get(y * buffer.getStrideV() + x);
                nv21Data[ySize + y * chromaStride + 2 * x + 0] = vValue;
                nv21Data[ySize + y * chromaStride + 2 * x + 1] = uValue;
            }
        }
        return nv21Data;
    }

    private VideoFrame.I420Buffer createTestI420Buffer() {
        final int width = 16;
        final int height = 16;
        final int[] yData = new int[] {156, 162, 167, 172, 177, 182, 187, 193, 199, 203, 209, 214, 219,
                224, 229, 235, 147, 152, 157, 162, 168, 173, 178, 183, 188, 193, 199, 205, 210, 215, 220,
                225, 138, 143, 148, 153, 158, 163, 168, 174, 180, 184, 190, 195, 200, 205, 211, 216, 128,
                133, 138, 144, 149, 154, 159, 165, 170, 175, 181, 186, 191, 196, 201, 206, 119, 124, 129,
                134, 140, 145, 150, 156, 161, 166, 171, 176, 181, 187, 192, 197, 109, 114, 119, 126, 130,
                136, 141, 146, 151, 156, 162, 167, 172, 177, 182, 187, 101, 105, 111, 116, 121, 126, 132,
                137, 142, 147, 152, 157, 162, 168, 173, 178, 90, 96, 101, 107, 112, 117, 122, 127, 132, 138,
                143, 148, 153, 158, 163, 168, 82, 87, 92, 97, 102, 107, 113, 118, 123, 128, 133, 138, 144,
                149, 154, 159, 72, 77, 83, 88, 93, 98, 103, 108, 113, 119, 124, 129, 134, 139, 144, 150, 63,
                68, 73, 78, 83, 89, 94, 99, 104, 109, 114, 119, 125, 130, 135, 140, 53, 58, 64, 69, 74, 79,
                84, 89, 95, 100, 105, 110, 115, 120, 126, 131, 44, 49, 54, 59, 64, 70, 75, 80, 85, 90, 95,
                101, 106, 111, 116, 121, 34, 40, 45, 50, 55, 60, 65, 71, 76, 81, 86, 91, 96, 101, 107, 113,
                25, 30, 35, 40, 46, 51, 56, 61, 66, 71, 77, 82, 87, 92, 98, 103, 16, 21, 26, 31, 36, 41, 46,
                52, 57, 62, 67, 72, 77, 83, 89, 94};
        final int[] uData = new int[] {110, 113, 116, 118, 120, 123, 125, 128, 113, 116, 118, 120, 123,
                125, 128, 130, 116, 118, 120, 123, 125, 128, 130, 132, 118, 120, 123, 125, 128, 130, 132,
                135, 120, 123, 125, 128, 130, 132, 135, 138, 123, 125, 128, 130, 132, 135, 138, 139, 125,
                128, 130, 132, 135, 138, 139, 142, 128, 130, 132, 135, 138, 139, 142, 145};
        final int[] vData = new int[] {31, 45, 59, 73, 87, 100, 114, 127, 45, 59, 73, 87, 100, 114, 128,
                141, 59, 73, 87, 100, 114, 127, 141, 155, 73, 87, 100, 114, 127, 141, 155, 168, 87, 100,
                114, 128, 141, 155, 168, 182, 100, 114, 128, 141, 155, 168, 182, 197, 114, 127, 141, 155,
                168, 182, 196, 210, 127, 141, 155, 168, 182, 196, 210, 224};
        return JavaI420Buffer.wrap(width, height, toByteBuffer(yData),
                /* strideY= */ width, toByteBuffer(uData), /* strideU= */ width / 2, toByteBuffer(vData),
                /* strideV= */ width / 2,
                /* releaseCallback= */ null);
    }

    /** Convert a byte array to a direct ByteBuffer. */
    private ByteBuffer toByteBuffer(int[] array) {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(array.length);
        buffer.put(toByteArray(array));
        buffer.rewind();
        return buffer;
    }

    /**
     * Convert an int array to a byte array and make sure the values are within the range [0, 255].
     */
    private byte[] toByteArray(int[] array) {
        final byte[] res = new byte[array.length];
        for (int i = 0; i < array.length; ++i) {
            final int value = array[i];
            res[i] = (byte) value;
        }
        return res;
    }
}
