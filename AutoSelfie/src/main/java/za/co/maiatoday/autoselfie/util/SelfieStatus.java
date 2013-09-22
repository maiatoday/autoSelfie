package za.co.maiatoday.autoselfie.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.Random;

import za.co.maiatoday.autoselfie.glitchP5.GlitchFX;

/**
 * Created by maia on 2013/08/22.
 */
public class SelfieStatus {
    private static final int DRIP_COUNT = 20;
    Bitmap bmpToPost;
    String status;
    Bitmap orig;
    boolean processDone = false;

    private static final int MAX_FACES = 5;
    private FaceDetector detector;
    private FaceDetector.Face[] faces;
    int facesFound;

    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector mDetector;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;
    //    GlitchP5 glitchP5;
    GlitchFX glitchfx;
    private int magic = 20;
    private int i1 = 0;

    void SelfieStatus() {
    }

    public void setProcessDone(boolean processDone) {
        this.processDone = processDone;
    }

    public Bitmap getOrig() {
        return orig;
    }

    public void setOrig(Bitmap orig) {
        this.orig = orig;
        this.bmpToPost = orig;
        processDone = false;
        magic = orig.getWidth() / 16;
    }

    public Bitmap getBmpToPost() {
        switch (i1) {
        case 0:
            bmpToPost = eyeRedDrips(bmpToPost);
            break;
        case 1:
            bmpToPost = eyeLargeBlocks(bmpToPost, 1);
            break;
        case 2:
            bmpToPost = eyeLargeBlocks(bmpToPost, 16);
            break;
        case 3:
            putEyesInAfterTheFact(bmpToPost);
            break;
        }

        return bmpToPost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean processSelfie() {
        if (orig == null) {
            return false;
        }
        if (processDone) {
            return true;
        }
        status = "#autoselfie";
        detectFaces();
        setupMats(orig);
        //Roll the dice to see which technique to use
        Random r = new Random();
        i1 = r.nextInt(4);
        switch (i1) {
        case 0:
            cannyKonny();
            break;
        default:
        case 1:
            primaryRoy();
            break;
        case 2:
            andAnotherOneForLuck();
            break;
        case 3:
            noPointInHoldingOn();
            break;
//        case 4:
//            blobbyContours();
//            break;

        }
        Log.i("SelfieStatus", status);
        processDone = true;
//        glitchP5 = new GlitchP5(bmpToPost);
        glitchfx = new GlitchFX(bmpToPost);
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
        Imgproc.Canny(mRgba, mIntermediateMat, 80, 90);
        Core.bitwise_not(mIntermediateMat, mIntermediateMat);

        bmpToPost = getImagefromMat(mIntermediateMat);
        // find the eyes and make red lines
        bmpToPost = eyeRedDrips(bmpToPost);

        status = "#autoselfie canny and Konny";
    }


    private void primaryRoy() {
        // black white  red yellow blue  and dots
//        Imgproc.cvtColor(mRgba, mMatToPost, Imgproc.COLOR_RGB2GRAY, 4);
        Random r = new Random();
        int i1 = r.nextInt(20) + 128;
        Imgproc.threshold(mRgba, mRgba, i1, 255, 0);
        bmpToPost = getImagefromMat(mRgba);
        bmpToPost = drawDots(bmpToPost);
        bmpToPost = eyeLargeBlocks(bmpToPost, 1);
        status = "#autoselfie 8bit Roy";
    }

    private void blobbyContours() {
        //down to 2bit colour, find blobs then perimeters of blobs
        // contours overlap to make a grid

        //setup blob detector
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255, 0, 0, 255);

        //do blob detect
        mDetector.process(mRgba);

        //prepare image
        Imgproc.cvtColor(mRgba, mIntermediateMat, Imgproc.COLOR_RGB2GRAY, 4);
        Random r = new Random();
        int i1 = r.nextInt(20) + 128;
        Imgproc.threshold(mIntermediateMat, mRgba, i1, 255, 0);

        //drawContours
        List<MatOfPoint> contours = mDetector.getContours();
        Log.i("SelfieStatus", "Contours count: " + contours.size());
        Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

//        Mat colorLabel = mRgba.submat(4, 68, 4, 68);
//        colorLabel.setTo(mBlobColorRgba);
//
//        Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
//        mSpectrum.copyTo(spectrumLabel);

        bmpToPost = getImagefromMat(mRgba);
        status = "#autoselfie blobbly Contours";
    }

    private void andAnotherOneForLuck() {
        // the lucky one, orange, pink, red an yellow are lucky colours
        // eyes all catlike and spinning wheels
        Imgproc.cvtColor(mRgba, mIntermediateMat, Imgproc.COLOR_RGB2GRAY, 4);
        Random r = new Random();
        int i1 = r.nextInt(20) + 128;
        Imgproc.threshold(mIntermediateMat, mRgba, i1, 255, 0);
        bmpToPost = getImagefromMat(mRgba);
        bmpToPost = eyeLargeBlocks(bmpToPost, 16);
        status = "#autoselfie and another one for luck";
    }

    private void noPointInHoldingOn() {
        mRgbaInnerWindow = setInnerMatfromEyes(mRgba);
        Imgproc.Canny(mRgba, mIntermediateMat, 80, 90);
        Imgproc.cvtColor(mIntermediateMat, mIntermediateMat, Imgproc.COLOR_GRAY2BGRA, 4);
        mGrayInnerWindow = setInnerMatfromEyes(mIntermediateMat);
        if (mGrayInnerWindow != null && mRgbaInnerWindow != null) {
            mRgbaInnerWindow.copyTo(mGrayInnerWindow);
        }
        bmpToPost = getImagefromMat(mIntermediateMat);
        status = "#autoselfie no point in holding on";
    }

    private Bitmap putEyesInAfterTheFact(Bitmap b) {

        Utils.bitmapToMat(b, mIntermediateMat);
        mGrayInnerWindow = setInnerMatfromEyes(mIntermediateMat);
        mRgbaInnerWindow.copyTo(mGrayInnerWindow);
        bmpToPost = getImagefromMat(mIntermediateMat);
        return bmpToPost;
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

    private Mat setInnerMatfromEyes(Mat orig) {
        int top = 0;
        int height = 0;
        int left = 0;
        int width = 0;
        PointF midPoint = new PointF();
        float eyeDistance = 0.0f;

        if (facesFound > 0) {
            faces[0].getMidPoint(midPoint);
            eyeDistance = faces[0].eyesDistance();

            top = (int) midPoint.y - orig.rows() / 24;
            left = (int) midPoint.x - (int) eyeDistance;
            height = orig.rows() / 12;
            width = (int) eyeDistance * 2;
            return orig.submat(top, top + height, left, left + width);
        }
        return null;
    }

    private Bitmap getImagefromMat(Mat result) {
        Bitmap outBmp = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, outBmp);
        return outBmp;
    }

    //All the eye drawing methods

    private Bitmap eyeRedDrips(Bitmap in) {

        Bitmap out = in.copy(in.getConfig(), true);
        Paint drawPaint = new Paint();

        drawPaint.setColor(Color.RED);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(5);

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
                Random r = new Random();
                int flip = r.nextInt(2);
                int maxDripLength = in.getHeight() / 8;

                float halfEyeWidth = eyeDistance / 4;
                float eyeLeftCentreX = (int) midPoint.x - eyeDistance / 2;
                float belowEyesY = (int) midPoint.y + halfEyeWidth / 2;

                canvas.drawLine(eyeLeftCentreX - halfEyeWidth,
                    belowEyesY,
                    eyeLeftCentreX + halfEyeWidth,
                    belowEyesY, drawPaint);

                float eyeRightCentreX = (int) midPoint.x + eyeDistance / 2;
                canvas.drawLine((int) eyeRightCentreX - halfEyeWidth,
                    belowEyesY,
                    eyeRightCentreX + halfEyeWidth,
                    belowEyesY, drawPaint);

                float dx = eyeDistance / 2 / DRIP_COUNT;
                float startx = eyeLeftCentreX - halfEyeWidth;
                if (flip == 0) {
                    eyeDrips(drawPaint, canvas, maxDripLength, belowEyesY, dx, startx);
                } else {
                    startx = eyeRightCentreX - halfEyeWidth;
                    eyeDrips(drawPaint, canvas, maxDripLength, belowEyesY, dx, startx);
                }
            }
        }
        return out;
    }

    private void eyeDrips(Paint drawPaint, Canvas canvas, int maxDripLength, float belowEyesY, float dx, float startx) {

        for (int li = 0; li < DRIP_COUNT; li++) {
            Random r = new Random();
            int ll = r.nextInt(maxDripLength);
            canvas.drawLine(startx,
                belowEyesY,
                startx,
                belowEyesY + ll, drawPaint);
            startx += dx;
        }
    }

    private Bitmap eyeDelete(Bitmap in) {
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

    private Bitmap eyeLargeBlocks(Bitmap in, int count) {
        Bitmap out = in.copy(in.getConfig(), true);
        Paint drawPaint = new Paint();

        drawPaint.setColor(Color.MAGENTA);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(1);

        Canvas canvas = new Canvas();
        canvas.setBitmap(out);

        PointF midPoint = new PointF();
        float eyeDistance = 0.0f;
        float confidence = 0.0f;
        int jump = in.getWidth() / 64;

        if (facesFound > 0) {
            for (int index = 0; index < facesFound; ++index) {
                faces[index].getMidPoint(midPoint);
                eyeDistance = faces[index].eyesDistance();
                confidence = faces[index].confidence();

                Log.i("FaceDetector",
                    "Confidence: " + confidence +
                        ", Eye distance: " + eyeDistance +
                        ", Mid Point: (" + midPoint.x + ", " + midPoint.y + ")");
                for (int of = 0; of < count; of += 1) {
                    if (of % 2 == 0) {
                        drawPaint.setColor(Color.MAGENTA);
                    } else if (of % 3 == 0) {
                        drawPaint.setColor(Color.RED);
                    } else {
                        drawPaint.setColor(Color.YELLOW);
                    }
                    canvas.drawRect((int) midPoint.x - eyeDistance + of,
                        (int) midPoint.y - eyeDistance / 2 + of * jump,
                        (int) midPoint.x + eyeDistance + of * jump,
                        (int) midPoint.y + eyeDistance / 2 + of * jump, drawPaint);
                }
            }
        }
        return out;
    }

    private Bitmap drawDots(Bitmap in) {
        Bitmap out = in.copy(in.getConfig(), true);
        Paint drawPaint = new Paint();
        int radius = in.getWidth() / 200;
        drawPaint.setStyle(Paint.Style.FILL);

        Canvas canvas = new Canvas();
        canvas.setBitmap(out);
        int newBlack = Color.argb(0xff, 0x16, 0x16, 0x16);
        for (int x = 0; x < in.getWidth(); x += radius * 4) {
            for (int y = 0; y < in.getHeight(); y += radius * 4) {
                int p = in.getPixel(x, y);
//                int pixelAlpha=   Color.alpha(p);
//                int red =  Color.red(p);
//                int green=    Color.green(p);
//                int blue=    Color.blue(p);
//                int newColor = Color.argb(pixelAlpha, red + 5, green + 5, blue + 5);
                if (p == Color.BLACK) {
                    drawPaint.setColor(newBlack);
                    canvas.drawCircle(x, y, radius, drawPaint);
                } else if (p == Color.RED) {
                    drawPaint.setColor(Color.MAGENTA);
                    canvas.drawCircle(x, y, radius, drawPaint);
                }
            }
        }

        return out;
    }


    public void glitchImage(RectF bounds) {
        if (glitchfx == null) return;
        glitchfx.open();
        glitchfx.glitch((int) bounds.centerX(), (int) bounds.centerY(), (int) bounds.width(), (int) bounds.height(), magic, magic);
        glitchfx.close();
        bmpToPost = glitchfx.getBitmap();
    }


}
