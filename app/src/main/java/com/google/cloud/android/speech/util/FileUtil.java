package com.google.cloud.android.speech.util;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Created by USER on 2017-10-11.
 */

public class FileUtil {
    private static String AUDIO_RECORDER_FOLDER = "Music";
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.pcm";
    private static final String VIDEO_RECORDER_TEMP_FILE = "record_video_temp.pcm";

    public static String getFilename(String title) {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }
        String fileName = (title == null | title.equals("")) ? String.valueOf(System.currentTimeMillis()) : title;
        return (file.getAbsolutePath() + "/" + fileName +
                AUDIO_RECORDER_FILE_EXT_WAV);
    }


    public static String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();


        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    public static String getTempVideoFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();


        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + VIDEO_RECORDER_TEMP_FILE);
    }


    public static void deleteFile(Context context, String path) {
        File fdelete = new File(path);
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Toast.makeText(context, path + " deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "deletion failed", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private static short[] shortMe(byte[] bytes) {
        short[] out = new short[bytes.length / 2]; // will drop last byte if odd number
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < out.length; i++) {
            out[i] = bb.getShort();
        }
        return out;
    }


    public static void copyWaveFile(String inFilename, String outFilename, long recorderSampleRate, int RECORDER_BPP, int bufferSize, int channel) {
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = recorderSampleRate;
        int channels = 1;
        long byteRate = RECORDER_BPP * recorderSampleRate * channels / 8;

        byte[] data = new byte[bufferSize];
        byte[] temp = new byte[bufferSize / 2];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size() / 2;
            totalDataLen = totalAudioLen + 36;


            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate, RECORDER_BPP);


            short[] stereoSamples;
            short[] monoSamples = new short[bufferSize / 4];

            if (channel == 1) {
                while (in.read(data) != -1) {
                    out.write(data);
                }
            } else {
//                while (in.read(data) != -1) {
//                    for (int i = 0; i < bufferSize / 2; i++) {
//                        int s =0;
//                        s+=data[i * 2];
//                        s+=data[i * 2 + 1];
//                        s /= 2;
//                        temp[i] = (byte) s;
////                        temp[i]= (byte) ((data[i*2]));
//                    }
//                    out.write(temp);
//
//                }

                while (in.read(data) != -1) {
                    stereoSamples = shortMe(data);
                    for (int i = 0; i < monoSamples.length; i++) {
                        monoSamples[i] = (short) ((stereoSamples[i * 2] + stereoSamples[(i * 2) + 1]) / 2);
                        temp[i * 2] = (byte) (monoSamples[i] & 0xff);
                        temp[i * 2 + 1] = (byte) ((monoSamples[i] >> 8) & 0xff);

                    }
                    out.write(temp);

                }
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate, int RECORDER_BPP) throws IOException {
        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1 (audio format : PCM)
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = (byte) RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }
}
