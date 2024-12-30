package org.firstinspires.ftc.teamcode.util;

/**
 * Created by tycho on 2/18/2017, but borrowed some of this from Team Fixit's VortexUtils. All hail Team Fixit!!!!!
 */

import android.graphics.Bitmap;
import android.graphics.Canvas;
//import android.support.annotation.Nullable;

import android.util.Log;
import android.view.View;

//import com.vuforia.CameraCalibration; //to do revert after https://github.com/FIRST-Tech-Challenge/FtcRobotController/issues/26
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
//import org.firstinspires.ftc.robotcore.internal.vuforia.

import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.teamcode.vision.colorblob.ColorBlobDetector;
import org.firstinspires.ftc.teamcode.RC;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.android.Utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;



public class VisionUtils {

    public final static int NOT_VISIBLE = 0;

    public final static int BEACON_BLUE_RED = 1;//blue left, red right
    public final static int BEACON_RED_BLUE = 2;//blue right, red left
    public final static int BEACON_ALL_BLUE = 3;
    public final static int BEACON_NO_BLUE = 4;

    public final static int OBJECT_BLUE = 1;
    public final static int OBJECT_RED = 2;

    //hsv blue beacon range colours
    //DON'T CHANGE THESE NUMBERS
    public final static Scalar BEACON_BLUE_LOW = new Scalar(108, 0, 220);
    public final static Scalar BEACON_BLUE_HIGH = new Scalar(178, 255, 255);

    public final static Scalar OTHER_BLUE_LOW = new Scalar(105, 120, 110);
    public final static Scalar OTHER_BLUE_HIGH = new Scalar(185, 255, 255);
    public final static Scalar OTHER_RED_LOW = new Scalar(222, 101, 192);
    public final static Scalar OTHER_RED_HIGH = new Scalar(47, 251, 255);

    public final static Scalar RED_CRYPTO = new Scalar(47, 251, 255);

    public static Mat bitmapToMat (Bitmap bit, int cvType) {
        Mat newMat = new Mat(bit.getHeight(), bit.getWidth(), cvType);

        Utils.bitmapToMat(bit, newMat);

        return newMat;
    }

    public static Mat applyMask(Mat img, Scalar low, Scalar high) {

        Mat mask = new Mat(img.size(), CvType.CV_8UC3);

        if (high.val[0] < low.val[0]) {
            Scalar lowMed = new Scalar(255, high.val[1], high.val[2]);
            Scalar medHigh = new Scalar(0, low.val[1], low.val[2]);

            Mat maskLow = new Mat(img.size(), CvType.CV_8UC3);
            Mat maskHigh = new Mat(img.size(), CvType.CV_8UC3);

            Core.inRange(img, low, lowMed, maskLow);
            Core.inRange(img, medHigh, high, maskHigh);

            Core.bitwise_or(maskLow, maskHigh, mask);
        } else {
            Core.inRange(img, low, high, mask);
        }//else

        return mask;
    }


    public static int getJewelConfig(Bitmap bm) {


        if (bm != null) {


            //turning the corner pixel coordinates into a proper bounding box
            Mat image = bitmapToMat(bm, CvType.CV_8UC3);

            //filtering out non-beacon-blue colours in HSV colour space
            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2HSV_FULL);

            //get filtered mask
            //if pixel is within acceptable blue-beacon-colour range, it's changed to white.
            //Otherwise, it's turned to black
            Mat mask = new Mat();

            Core.inRange(image, BEACON_BLUE_LOW, BEACON_BLUE_HIGH, mask);
            Moments mmnts = Imgproc.moments(mask, true);

            //calculating centroid of the resulting binary mask via image moments
            Log.i("CentroidX", "" + ((mmnts.get_m10() / mmnts.get_m00())));
            Log.i("CentroidY", "" + ((mmnts.get_m01() / mmnts.get_m00())));

            //checking if blue either takes up the majority of the image (which means the beacon is all blue)
            //or if there's barely any blue in the image (which means the beacon is all red or off)
//            if (mmnts.get_m00() / mask.total() > 0.8) {
//                return VortexUtils.BEACON_ALL_BLUE;
//            } else if (mmnts.get_m00() / mask.total() < 0.1) {
//                return VortexUtils.BEACON_NO_BLUE;
//            }//elseif

            //Note: for some reason, we end up with a image that is rotated 90 degrees
            //if centroid is in the bottom half of the image, the blue beacon is on the left
            //if the centroid is in the top half, the blue beacon is on the right
            if ((mmnts.get_m01() / mmnts.get_m00()) < image.rows() / 2) {
                return VisionUtils.BEACON_RED_BLUE;
            } else {
                return VisionUtils.BEACON_BLUE_RED;
            }//else
        }//if

        return VisionUtils.NOT_VISIBLE;
    }//getJewelConfig


/* old vision stuff dead with Vuforia removed
    public static double getColumnPos(Image img, int columnId, ColorBlobDetector detector) {

        Mat overlay;
        overlay = new Mat();
        List <BlobStats> blobStats;

        //getting camera image...
        Bitmap bm = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.RGB_565);
        bm.copyPixelsFromBuffer(img.getPixels());
        Mat eye = bitmapToMat(bm, CvType.CV_8UC3);
        detector.process(eye, overlay);
        blobStats = detector.getBlobStats();
        double largest =0;
        int x = 0;

        //need some magic here to group the blobs into columns and find the error to the desired column

        //this is a very basic example where we find the x coordinate of the largest blob's centroid
        Iterator<BlobStats> each = blobStats.iterator();
        while (each.hasNext()) {
            BlobStats stat = each.next();
            if (stat.area > largest)
            {
                largest = stat.area;
                x = stat.x;
            }

        }

        return ErrorPixToDeg(x);

    }

 */


    Bitmap getBitmapFromView(View v)
    {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }


    //@Nullable
    /* Vuforia Removed
    public static Image getImageFromFrame(VuforiaLocalizer.CloseableFrame frame, int format) {

        long numImgs = frame.getNumImages();
        for (int i = 0; i < numImgs; i++) {
            if (frame.getImage(i).getFormat() == format) {
                return frame.getImage(i);
            }//if
        }//for

        return null;
    }

     */

    //this assumes the horizontal axis is the y-axis since the phone is vertical
    //robot angle is relative to "parallel with the beacon wall"
    public static VectorF navOffWall(VectorF trans, double robotAngle, VectorF offWall){
        return new VectorF(
                (float) (trans.get(0) - offWall.get(0) * Math.sin(Math.toRadians(robotAngle)) - offWall.get(2) * Math.cos(Math.toRadians(robotAngle))),
                trans.get(1) + offWall.get(1),
                (float) (-trans.get(2) + offWall.get(0) * Math.cos(Math.toRadians(robotAngle)) - offWall.get(2) * Math.sin(Math.toRadians(robotAngle)))
        );
    }

    public static VectorF navOffWall2(VectorF trans, double robotAngle, VectorF offWall){
        double theta = Math.toDegrees(Math.atan2(offWall.get(0), offWall.get(2)));

        return new VectorF(
                (float) (trans.get(0) - offWall.get(0) * Math.sin(Math.toRadians(robotAngle)) - offWall.get(2) * Math.cos(Math.toRadians(robotAngle))),
                trans.get(1),
                (float) (-trans.get(2) + offWall.get(0) * Math.cos(Math.toRadians(robotAngle)) - offWall.get(2) * Math.sin(Math.toRadians(robotAngle)))
        );
    }

    private static double ErrorPixToDeg(int blobx){
        int ViewWidth = 800;
        int ScreenCenterPix;
        int ErrorPix;
        double PixPerDegree;
        double errorDegrees;

        ScreenCenterPix = ViewWidth/2;
        ErrorPix = ScreenCenterPix - blobx;
        PixPerDegree = ViewWidth / 75; //FOV
        errorDegrees = ErrorPix/PixPerDegree;
        if (errorDegrees < 0) {
            errorDegrees += 360;
        }
        return errorDegrees;
    }
}