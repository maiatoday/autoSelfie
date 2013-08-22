package za.co.maiatoday.autoselfie.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.os.Environment;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by maia on 2013/08/22.
 */
public class SelfieStatus {
    Bitmap bmpToPost;
    String status;
    Bitmap orig;

    private static final int MAX_FACES = 5;

    public void setOrig(Bitmap orig) {
        this.orig = orig;
        processSelfie();
    }

    public Bitmap getBmpToPost() {

        return bmpToPost;
    }

    public String getStatus() {
        return status;
    }

    public boolean processSelfie() {
        status = "#autoselfie";
        detectFaces();
        return true;
    }

    private void detectFaces() {
        if (null != orig) {
            int width = orig.getWidth();
            int height = orig.getHeight();

            FaceDetector detector = new FaceDetector(width, height, MAX_FACES);
            FaceDetector.Face[] faces = new FaceDetector.Face[MAX_FACES];

            bmpToPost = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Paint ditherPaint = new Paint();
            Paint drawPaint = new Paint();

            ditherPaint.setDither(true);
            drawPaint.setColor(Color.BLACK);
            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setStrokeWidth(16);

            Canvas canvas = new Canvas();
            canvas.setBitmap(bmpToPost);
            canvas.drawBitmap(orig, 0, 0, ditherPaint);

            int facesFound = detector.findFaces(bmpToPost, faces);
            PointF midPoint = new PointF();
            float eyeDistance = 0.0f;
            float confidence = 0.0f;

            Log.i("FaceDetector", "Number of faces found: " + facesFound);

            if (facesFound > 0) {
                for (int index = 0; index < facesFound; ++index) {
                    faces[index].getMidPoint(midPoint);
                    eyeDistance = faces[index].eyesDistance();
                    confidence = faces[index].confidence();

                    Log.i("FaceDetector",
                            "Confidence: " + confidence +
                                    ", Eye distance: " + eyeDistance +
                                    ", Mid Point: (" + midPoint.x + ", " + midPoint.y + ")");

//                    canvas.drawRect((int)midPoint.x - eyeDistance ,
//                            (int)midPoint.y - eyeDistance ,
//                            (int)midPoint.x + eyeDistance,
//                            (int)midPoint.y + eyeDistance, drawPaint);
                    canvas.drawLine((int) midPoint.x - eyeDistance,
                            (int) midPoint.y,
                            (int) midPoint.x + eyeDistance,
                            (int) midPoint.y, drawPaint);
                }
            }

            String filepath = Environment.getExternalStorageDirectory() + "/facedetect" + System.currentTimeMillis() + ".jpg";

            try {
                FileOutputStream fos = new FileOutputStream(filepath);

                bmpToPost.compress(Bitmap.CompressFormat.JPEG, 90, fos);

                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }
}
