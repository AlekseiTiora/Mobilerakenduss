package com.example.mobilerakenduss;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class AudioEncoder {
    private static final String TAG = "AudioEncoder";
    private static final String MIME_TYPE = "audio/flac";
    private static final int SAMPLE_RATE = 16000;
    private static final int BIT_RATE = 16000;
    private static final int CHANNEL_COUNT = 1;
    private static final int MAX_BUFFER_SIZE =1024 * 1024;

    public void encodeToFlac(String inputFile, String outputFile) {
        try {

            // Путь к входному аудиофайлу
            FileInputStream fis = new FileInputStream(inputFile);

            // Путь к выходному аудиофайлу
            String outputFilePath = "path/to/output/file.flac";
            FileOutputStream fos = new FileOutputStream(outputFile);

            MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, CHANNEL_COUNT);
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);

            MediaCodec codec = MediaCodec.createEncoderByType(MIME_TYPE);
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();

            ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
            ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean outputDone = false;

            while (!outputDone) {
                if (!inputDone) {
                    int inputBufIndex = codec.dequeueInputBuffer(-1);
                    if (inputBufIndex >= 0) {
                        ByteBuffer inputBuf = codecInputBuffers[inputBufIndex];
                        inputBuf.clear();

                        byte[] buffer = new byte[MAX_BUFFER_SIZE];
                        int bytesRead = fis.read(buffer, 0, buffer.length);
                        if (bytesRead == -1) {
                            codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            inputBuf.put(buffer, 0, bytesRead);
                            codec.queueInputBuffer(inputBufIndex, 0, bytesRead, 0, 0);
                        }
                    }
                }

                int outputBufIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
                if (outputBufIndex >= 0) {
                    ByteBuffer outputBuf = codecOutputBuffers[outputBufIndex];
                    byte[] chunk = new byte[bufferInfo.size];
                    outputBuf.get(chunk);
                    outputBuf.clear();

                    if (chunk.length > 0) {
                        fos.write(chunk);
                    }

                    codec.releaseOutputBuffer(outputBufIndex, false);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true;
                    }
                }
            }

            codec.stop();
            codec.release();
            fis.close();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
