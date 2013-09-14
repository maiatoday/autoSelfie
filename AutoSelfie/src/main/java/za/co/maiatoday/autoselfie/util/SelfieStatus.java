package za.co.maiatoday.autoselfie.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
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
    int facesFound;

    void SelfieStatus() {


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
        setupMats(orig);
        //Roll the dice to see which technique to use
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
        case 3:
            andAnotherOneForLuck();
            break;
        default:
        case 4:
            noPointInHoldingOn();
            break;
        }
        Log.i("SelfieStatus", status);
        return true;
    }


    private void detectFaces() {
        if (null != orig) {
            int width = orig.getWidth();
            int height = orig.getHeight();

            detector = new FaceDetector(width, height, MAX_FACES);
            faces = new FaceDetector.Face[MAX_FACES];

            Bitmap temp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Paint ditherPaint = new Paint();
            ditherPaint.setDither(true);

            Canvas canvas = new Canvas();
            canvas.setBitmap(temp);
            canvas.drawBitmap(orig, 0, 0, ditherPaint);

            facesFound = detector.findFaces(temp, faces);

            Log.i("FaceDetector", "Number of faces found: " + facesFound);
        }
    }

    private void cannyKonny() {
//        if ((mRgbaInnerWindow == null) || (mGrayInnerWindow == null) || (mRgba.cols() != mSizeRgba.width) || (mRgba.height() != mSizeRgba.height))
        Imgproc.Canny(mRgbaInnerWindow, mIntermediateMat, 80, 90);
        Imgproc.cvtColor(mIntermediateMat, mRgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
        Core.bitwise_not(mRgbaInnerWindow, mIntermediateMat);
        bmpToPost = getImagefromMat(mIntermediateMat);
        status = "#autoselfie first openCV canny filter";
        // find the eyes and make red lines
    }

    private void primaryRoy() {
        // black white  red yellow blue  and dots
//        Imgproc.cvtColor(mRgba, mMatToPost, Imgproc.COLOR_RGB2GRAY, 4);
        Random r = new Random();
        int i1 = r.nextInt(20) + 128;
        Imgproc.threshold(mRgba, mRgba, i1, 255, 0);
        bmpToPost = getImagefromMat(mRgba);
        status = "#autoselfie 8bit Roy";
    }

    private void blobbyContours() {
        //down to 2bit colour, find blobs then perimeters of blobs
        // contours overlap to make a grid
        Imgproc.cvtColor(mRgba, mIntermediateMat, Imgproc.COLOR_RGB2GRAY, 4);
        Random r = new Random();
        int i1 = r.nextInt(20) + 128;
        Imgproc.threshold(mIntermediateMat, mRgba, i1, 255, 0);
        bmpToPost = getImagefromMat(mRgba);
        status = "#autoselfie blobbly Contours";
    }

    private void andAnotherOneForLuck() {
// the lucky one, orange, pink, red an yellow are lucky colours
        // eyes all catlike and spinning wheels

        bmpToPost = blockFace(orig);
        status = "#autoselfie and another one for luck";
    }

    private void noPointInHoldingOn() {
        bmpToPost = deleteEyes(orig);
        status = "#autoselfie no point in holding on";
    }

    private Size mSize0;
    private Size mSizeRgba;
    private Size mSizeRgbaInner;
    private Mat mRgba;
    private Mat mMatToPost;
    private Mat mIntermediateMat;
    private Mat mRgbaInnerWindow;
    private Mat mGrayInnerWindow;

    private void setupMats(Bitmap orig) {
        mRgba = new Mat();
        mMatToPost = new Mat();
        mRgbaInnerWindow = null;
        mGrayInnerWindow = null;
        mIntermediateMat = new Mat();
        Utils.bitmapToMat(orig, mRgba);
        Utils.bitmapToMat(orig, mMatToPost);
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

        if (mGrayInnerWindow == null && !mMatToPost.empty())
            mGrayInnerWindow = mMatToPost.submat(top, top + height, left, left + width);
    }

    private Bitmap getImagefromMat(Mat result) {
        Bitmap outBmp = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, outBmp);
        return outBmp;
    }

    private Bitmap deleteEyes(Bitmap in) {
        Bitmap out = in.copy(in.getConfig(), true);
        Paint drawPaint = new Paint();

        drawPaint.setColor(Color.BLACK);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(in.getWidth() / 8);

        Canvas canvas = new Canvas();
        canvas.setBitmap(out);

        PointF midPoint = new PointF();
        float eyeDistance = 0.0f;
        float confidence = 0.0f;

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
        return out;
    }

    private Bitmap blockFace(Bitmap in) {
        Bitmap out = in.copy(in.getConfig(), true);
        Paint drawPaint = new Paint();

        drawPaint.setColor(Color.GREEN);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(1);

        Canvas canvas = new Canvas();
        canvas.setBitmap(out);

        PointF midPoint = new PointF();
        float eyeDistance = 0.0f;
        float confidence = 0.0f;

        if (facesFound > 0) {
            for (int index = 0; index < facesFound; ++index) {
                faces[index].getMidPoint(midPoint);
                eyeDistance = faces[index].eyesDistance();
                confidence = faces[index].confidence();

                Log.i("FaceDetector",
                    "Confidence: " + confidence +
                        ", Eye distance: " + eyeDistance +
                        ", Mid Point: (" + midPoint.x + ", " + midPoint.y + ")");
                for (int of = 0; of < 20; of += 4) {
                    canvas.drawRect((int) midPoint.x - eyeDistance + of,
                        (int) midPoint.y - eyeDistance + of,
                        (int) midPoint.x + eyeDistance + of,
                        (int) midPoint.y + eyeDistance + of, drawPaint);
                }
            }
        }
        return out;
    }
}
