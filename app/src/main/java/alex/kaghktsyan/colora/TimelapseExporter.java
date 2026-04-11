package alex.kaghktsyan.colora;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Surface;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TimelapseExporter {

    public interface ExportListener {
        void onProgress(int progress);
        void onComplete(Uri uri);
        void onError(String error);
    }

    public static void export(Context context, File frameDir, ExportListener listener) {
        new Thread(() -> {
            MediaCodec encoder = null;
            MediaMuxer muxer = null;
            Surface inputSurface = null;
            try {
                File[] frames = frameDir.listFiles();
                if (frames == null || frames.length < 2) {
                    throw new Exception("Недостаточно кадров для создания видео");
                }
                Arrays.sort(frames);

                // Get dimensions from first frame
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(frames[0].getAbsolutePath(), options);
                int width = (options.outWidth / 2) * 2;
                int height = (options.outHeight / 2) * 2;

                String fileName = "Timelapse_" + System.currentTimeMillis() + ".mp4";
                File tempFile = new File(context.getCacheDir(), fileName);

                MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

                encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                inputSurface = encoder.createInputSurface();
                encoder.start();

                muxer = new MediaMuxer(tempFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                int videoTrackIndex = -1;
                boolean muxerStarted = false;

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                long frameDurationUs = 1000000 / 30; // 30 FPS

                for (int i = 0; i < frames.length; i++) {
                    Bitmap bitmap = BitmapFactory.decodeFile(frames[i].getAbsolutePath());
                    if (bitmap == null) continue;

                    Canvas canvas = inputSurface.lockHardwareCanvas();
                    canvas.drawBitmap(bitmap, 0, 0, null);
                    inputSurface.unlockCanvasAndPost(canvas);
                    bitmap.recycle();

                    videoTrackIndex = drainEncoder(encoder, muxer, bufferInfo, false, videoTrackIndex, muxerStarted, i * frameDurationUs);
                    if (videoTrackIndex >= 0) muxerStarted = true;

                    final int progress = (int) ((i / (float) frames.length) * 100);
                    new Handler(Looper.getMainLooper()).post(() -> listener.onProgress(progress));
                }

                drainEncoder(encoder, muxer, bufferInfo, true, videoTrackIndex, muxerStarted, frames.length * frameDurationUs);
                
                encoder.stop();
                muxer.stop();

                Uri uri = saveToGallery(context, tempFile, fileName);
                new Handler(Looper.getMainLooper()).post(() -> listener.onComplete(uri));

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> listener.onError("Ошибка экспорта: " + e.getMessage()));
                e.printStackTrace();
            } finally {
                if (encoder != null) encoder.release();
                if (muxer != null) muxer.release();
                if (inputSurface != null) inputSurface.release();
            }
        }).start();
    }

    private static int drainEncoder(MediaCodec encoder, MediaMuxer muxer, MediaCodec.BufferInfo bufferInfo, 
                                   boolean endOfStream, int trackIndex, boolean muxerStarted, long pts) {
        if (endOfStream) {
            encoder.signalEndOfInputStream();
        }

        while (true) {
            int outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break;
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) throw new RuntimeException("Format changed twice");
                trackIndex = muxer.addTrack(encoder.getOutputFormat());
                muxer.start();
                muxerStarted = true;
            } else if (outputBufferIndex >= 0) {
                ByteBuffer encodedData = encoder.getOutputBuffer(outputBufferIndex);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0 && muxerStarted) {
                    bufferInfo.presentationTimeUs = pts;
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                }
                encoder.releaseOutputBuffer(outputBufferIndex, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break;
            }
        }
        return trackIndex;
    }

    private static Uri saveToGallery(Context context, File videoFile, String fileName) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Colora");
        }

        Uri uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new Exception("Не удалось создать запись в галерее");

        try (OutputStream out = context.getContentResolver().openOutputStream(uri);
             FileInputStream in = new FileInputStream(videoFile)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        videoFile.delete();
        return uri;
    }
}
