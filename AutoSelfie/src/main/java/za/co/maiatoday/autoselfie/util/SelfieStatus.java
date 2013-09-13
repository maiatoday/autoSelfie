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

import java.util.Random;

/**
 * Created by maia on 2013/08/22.
 */
public class SelfieStatus {
    Bitmap bmpToPost;
    String status;
    Bitmap orig;

    private static final int MAX_FACES = 5;
    private FaceDetector detector;
    private FaceDetector.Face[] faces;

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

        mRgba = new Mat();
        mGray = new Mat();
        mRgbaInnerWindow = null;
        mGrayInnerWindow = null;
        mIntermediateMat = new Mat();
        Utils.bitmapToMat(bmpToPost, mRgba);
        Utils.bitmapToMat(bmpToPost, mGray);
        createAuxiliaryMats();
        //choose another algorithm
        Random r = new Random();
        int i1 = r.nextInt(5);
        switch (i1) {
        case 0:
            cannyKonny();
            break;
        case 1:
            primaryRoy();
            break;
        case 2:
            blobbyContours();
            break;
        default:
        case 3:
            andAnotherOneForLuck();
            break;
        }
        Utils.matToBitmap(mRgba, bmpToPost);
        Log.i("SelfieStatus", status);
        return true;
    }


    private void detectFaces() {
        if (null != orig) {
            int width = orig.getWidth();
            int height = orig.getHeight();

            detector = new FaceDetector(width, height, MAX_FACES);
            faces = new FaceDetector.Face[MAX_FACES];

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

    private void cannyKonny() {

        status = "#autoselfie first openCV canny filter";
        // find the eyes and make red lines

//        if ((mRgbaInnerWindow == null) || (mGrayInnerWindow == null) || (mRgba.cols() != mSizeRgba.width) || (mRgba.height() != mSizeRgba.height))
        Imgproc.Canny(mRgbaInnerWindow, mIntermediateMat, 80, 90);
        Imgproc.cvtColor(mIntermediateMat, mRgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);

        Utils.matToBitmap(mRgba, bmpToPost);
    }

    private void primaryRoy() {
        // black white  red yellow blue  and dots   status = "#autoselfie blobbly Contours";
        status = "#autoselfie 8bit Roy";
    }

    private void blobbyContours() {
        //down to 2bit colour, find blobs then perimeters of blobs
        // contours overlap to make a grid
        status = "#autoselfie blobbly Contours";
        Imgproc.cvtColor(mRgba, mIntermediateMat, Imgproc.COLOR_RGB2GRAY, 4);
        Random r = new Random();
        int i1 = r.nextInt(20) + 128;
        Imgproc.threshold(mIntermediateMat, mRgba, i1, 255, 0);
        bmpToPost = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bmpToPost);
    }

    private void andAnotherOneForLuck() {
// the lucky one, orange, pink, red an yellow are lucky colours
        // eyes all catlike and spinning wheels

        status = "#autoselfie blobbly Contours";
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY, 4);
        Random r = new Random();
        int i1 = r.nextInt(20) + 128;
        Imgproc.threshold(mGrayInnerWindow, mRgba, i1, 255, 0);
        bmpToPost = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bmpToPost);
        status = "#autoselfie and another one for luck";
    }

    private void noPointInHoldingOn() {
        blobbyContours();
        status = "#autoselfie no point in holding on";
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

        if (mGrayInnerWindow == null && !mGray.empty())
            mGrayInnerWindow = mGray.submat(top, top + height, left, left + width);
//
//        if (mZoomCorner == null)
//            mZoomCorner = mRgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);
//
//        if (mZoomWindow == null)
//            mZoomWindow = mRgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
    }
}
