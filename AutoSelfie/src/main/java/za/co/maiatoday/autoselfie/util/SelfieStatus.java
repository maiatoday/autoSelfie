package za.co.maiatoday.autoselfie.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by maia on 2013/08/22.
 */
public class SelfieStatus {
    Bitmap bmpToPost;
    String status;
    Bitmap orig;

    private static final int MAX_FACES = 5;

    void SelfieStatus() {

        mIntermediateMat = new Mat();
        createAuxiliaryMats();
    }

    public void setOrig(Bitmap orig) {
        this.orig = orig;
        this.bmpToPost = orig;
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
//        //choose another algorithm
//        Random r = new Random();
//        int i1=r.nextInt(5);
//        switch (i1) {
//            case 0:
//                break;
//        }
        firstTryOpenCV();

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
            drawPaint.setStrokeWidth(bmpToPost.getWidth() / 8);

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
        }
    }

    private void firstTryOpenCV() {
        mRgba = new Mat();
        mRgbaInnerWindow = null;
        mIntermediateMat = new Mat();
        Utils.bitmapToMat(orig, mRgba);

//        if ((mRgbaInnerWindow == null) || (mGrayInnerWindow == null) || (mRgba.cols() != mSizeRgba.width) || (mRgba.height() != mSizeRgba.height))
        createAuxiliaryMats();
        Imgproc.Canny(mRgbaInnerWindow, mIntermediateMat, 80, 90);
        Imgproc.cvtColor(mIntermediateMat, mRgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);

        Utils.matToBitmap(mRgba, bmpToPost);

        status = "#autoselfie first openCV canny filter";

    }

    private Size mSize0;
    private Size mSizeRgba;
    private Size mSizeRgbaInner;
    private Mat mRgba;
    private Mat mGray;
    private Mat mIntermediateMat;
    private Mat mRgbaInnerWindow;
    private Mat mGrayInnerWindow;
    private Mat mZoomWindow;
    private Mat mZoomCorner;
    private Mat mSepiaKernel;

    private void createAuxiliaryMats() {
        if (mRgba.empty())
            return;

        mSizeRgba = mRgba.size();

        int rows = (int) mSizeRgba.height;
        int cols = (int) mSizeRgba.width;

        int left = cols / 8;
        int top = rows / 8;

        int width = cols * 3 / 4;
        int height = rows * 3 / 4;

        if (mRgbaInnerWindow == null)
            mRgbaInnerWindow = mRgba.submat(top, top + height, left, left + width);
        mSizeRgbaInner = mRgbaInnerWindow.size();

//        if (mGrayInnerWindow == null && !mGray.empty())
//            mGrayInnerWindow = mGray.submat(top, top + height, left, left + width);
//
//        if (mZoomCorner == null)
//            mZoomCorner = mRgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);
//
//        if (mZoomWindow == null)
//            mZoomWindow = mRgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
    }
}
