package com.cgi.roadeye.android;

import android.graphics.Bitmap;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by valorcurse on 09/04/14.
 */
public class Utilities {
    public static Bitmap convertMatToBmp(Mat image) {
        Bitmap bmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(image, bmp);

        return bmp;
    }

    public static Mat drawHistogram(ArrayList<Integer> list, boolean vertical) {

        int height, width;
        int highestValue = Collections.max(list) > 0 ? Collections.max(list) : 1;

        if (vertical) {
            height = list.size();
            width = 120;
        } else {
            height = 120;
            width = list.size();
        }

        Mat histogram = new Mat(height, width, CvType.CV_8UC3, new Scalar(0, 0, 0));

        // Draw histogram
        for (int i = 0; i < list.size(); i++) {
            int value = list.get(i);


            if (vertical)
                Core.line(histogram,
                        new Point(width / 2, i),
                        new Point(width / 2 - (value * width) / (highestValue * 4), i),
                        new Scalar(0, 255, 0));
            else {
                Core.line(histogram,
                        new Point(i, height / 2),
                        new Point(i, height / 2 - (value * height) / (highestValue * 4)),
                        new Scalar(0, 255, 0));
            }
        }

        return histogram;
    }

    public static MatOfPoint matToPoints(Mat image) {
        ArrayList<Point> points = new ArrayList<Point>();
        for (int row = 0; row < image.rows(); row++) {
            for (int col = 0; col < image.cols(); col++) {
                double point = image.get(row, col)[0];

                if (point > 0)
                    points.add(new Point(col, row));
            }
        }

        MatOfPoint pointsMat = new MatOfPoint();
        pointsMat.fromList(points);

        return pointsMat;
    }

    public static boolean isInsideImage(Point tl, Point br, Mat image) {

        if (tl.x > 0 && tl.y > 0 &&
                br.x < image.size().width && br.y < image.size().height) {
            return true;
        }

        return false;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
