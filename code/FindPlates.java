package com.cgi.roadeye.android;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

/**
 * Created by valorcurse on 09/04/14.
 */
class FindPlates implements Runnable {

    Plate sourcePlate;
    Vector<Plate> platesBuffer;
    double averageBand;

    public FindPlates(Plate sourcePlate, Vector<Plate> platesBuffer, double averageBand) {
        this.sourcePlate = sourcePlate;
        this.platesBuffer = platesBuffer;
        this.averageBand = averageBand;
    }

    public void run() {

        Mat sourceBand = sourcePlate.getBandImage();

        // Find possible vertical position of plates
        Integer[] plateLocation = findPlates(sourceBand);

        Point tl = new Point(plateLocation[0], sourcePlate.getY1());
        Point br = new Point(plateLocation[1], sourcePlate.getY2());

        double ratio = (br.x - tl.x) / (br.y - tl.y);

        // If plate doesn't meet the correct size ratio
        if (3 <= ratio && ratio <= 6) return;
            // If it's outside the source image
        else if (!Utilities.isInsideImage(tl, br, sourcePlate.getSourceImage())) return;
        else if (plateLocation[1] - plateLocation[0] > 0) {

            sourcePlate.setX1(plateLocation[0]);
            sourcePlate.setX2(plateLocation[1]);

            Mat plateMat = new Mat(sourcePlate.getSourceImage(),
                    new Rect(new Point(sourcePlate.getX1(), sourcePlate.getY1()),
                            new Point(sourcePlate.getX2(), sourcePlate.getY2()))
            );

            Mat plateMask = new Mat(sourcePlate.getBandImage(),
                    new Rect(new Point(sourcePlate.getX1(), 0),
                            new Point(sourcePlate.getX2(), sourcePlate.getBandImage().height()))
            );

            plateMat = rotateAndCrop(plateMat, plateMask);

            if (plateMat == null) return;

            if (plateMat.width() > 0 && plateMat.height() > 0) {

                // Turn image to grayscale
                Mat grayWarped = new Mat(plateMat.size(), CvType.CV_8U);
                Imgproc.cvtColor(plateMat, grayWarped, Imgproc.COLOR_BGR2GRAY);

                Mat binaryWarped = new Mat(grayWarped.size(), CvType.CV_8U);
                Imgproc.adaptiveThreshold(grayWarped, binaryWarped, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                        Imgproc.THRESH_BINARY, 31, 15);

                // Find contours
                ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(binaryWarped, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

                // Only get inside contours
                ArrayList<MatOfPoint> characterContours = new ArrayList<MatOfPoint>();
                for (int i = 0; i < contours.size(); i++) {
                    if (hierarchy.get(0, i)[3] != -1) {
                        characterContours.add(contours.get(i));
                    }
                }

                if (contours.size() < 6) return;

                sortContours(characterContours);

                Log.i("FindPlate", "CharacterContours size: " + characterContours.size());

                ArrayList<Mat> letters = new ArrayList<Mat>();

                Mat text = new Mat(binaryWarped.rows(), 0, CvType.CV_8U, new Scalar(0));
                for (int i = 0; i < characterContours.size(); i++) {

                    Mat letter = new Mat(binaryWarped.size(), CvType.CV_8U, new Scalar(0));
                    Imgproc.drawContours(letter, characterContours, i, new Scalar(255), Core.FILLED);

                    double orientationAngle = getOrientation(characterContours.get(i));
                    orientationAngle = Utilities.round(orientationAngle, 2);

                    Log.i("FindPlates", "angle to slant : " + orientationAngle);

                    orientationAngle = -(1.57 - orientationAngle);

                    Log.i("FindPlates", "angle to slant 2: " + orientationAngle);
//                    orientationAngle = 1.0;

                    Mat rotatedLetter = new Mat();
                    if (Math.abs(orientationAngle) < 0.5) {
                        Mat transformationMatrix = new Mat(2, 3, CvType.CV_32F);
                        transformationMatrix.put(0, 0, 1);                    // x-scale
                        transformationMatrix.put(0, 1, orientationAngle);    // shear
                        transformationMatrix.put(0, 2, 0);                    // x-translation
                        transformationMatrix.put(1, 0, 0);                    // rotation
                        transformationMatrix.put(1, 1, 1);                    // y-scale
                        transformationMatrix.put(1, 2, 0);                    // y-translation

                        Imgproc.warpAffine(letter, rotatedLetter, transformationMatrix, letter.size(), Imgproc.INTER_CUBIC);
                    } else {
                        rotatedLetter = letter;
                    }

                    // Create list of points
                    MatOfPoint pointsMat = Utilities.matToPoints(rotatedLetter);

                    if (pointsMat.size().area() == 0) continue;

                    Rect rotatedBoundingRect = Imgproc.boundingRect(pointsMat);
                    rotatedLetter = new Mat(rotatedLetter, rotatedBoundingRect);

//                    letters.add(rotatedLetter);
                    int deltaHeight = text.rows() - rotatedLetter.rows();
                    int top = deltaHeight / 2;
                    int bottom = deltaHeight - top;

                    Mat extendedRotated = new Mat();
                    Imgproc.copyMakeBorder(rotatedLetter, extendedRotated, top, bottom, 0, 20, Imgproc.BORDER_CONSTANT);

                    Mat concatText = new Mat(text.rows(), text.cols() + extendedRotated.cols(), CvType.CV_8U, new Scalar(0));

                    // Move right boundary to the left.
                    concatText.adjustROI(0, 0, 0, -extendedRotated.cols());
                    text.copyTo(concatText);

                    // Move the left boundary to the right, right boundary to the right.
                    concatText.adjustROI(0, 0, -text.cols(), extendedRotated.cols());
                    extendedRotated.copyTo(concatText);

                    concatText.adjustROI(0, 0, text.cols(), 0);

                    text = concatText;
                }

//                Mat textClone = new Mat(text.size(), CvType.CV_8U, new Scalar(0));
//                for (int i = 0; i < characterContours.size(); i++) {
//                    Imgproc.drawContours(textClone, characterContours, i, new Scalar(255), Core.FILLED);
//                }

                sourcePlate.setLetters(letters);
//                text.push_back(textClone);
                sourcePlate.setPlateImage(text);
            }

            sourcePlate.setEndTime(new Date());
            platesBuffer.add(sourcePlate);
        }

    }

    double getOrientation(MatOfPoint points) {

        //Construct a buffer used by the pca analysis
        int pointsSize = (int) (points.size().height * points.size().width);
        Mat dataPoints = new Mat(pointsSize, 2, CvType.CV_64FC1);
        for (int i = 0; i < dataPoints.rows(); i += 2) {
            double[] values = points.get(i, 0);

            dataPoints.put(i, 0, values[0]);
            dataPoints.put(i, 1, values[1]);
        }

        Mat mean = new Mat();
        Mat eigenVectors = new Mat();
        Core.PCACompute(dataPoints, mean, eigenVectors);

        Log.i("FindPlates", "angle1: " + (Math.atan2(eigenVectors.get(0, 0)[0], eigenVectors.get(0, 1)[0])));
        if (eigenVectors.get(0, 0).length > 1)
            Log.i("FindPlates", "angle2: " + (Math.atan2(eigenVectors.get(0, 0)[1], eigenVectors.get(0, 1)[1])));

        double result = Math.atan2(eigenVectors.get(0, 0)[0], eigenVectors.get(0, 1)[0]);

        if (result != result) return 1.57;
        else return result;
    }

    private Integer[] findPlates(Mat sourceImage) {
        ArrayList<Integer> columnIntensity = columnIntensity(sourceImage);


        // Calculate derived of column intensity
        int kernelSize = 10;
        ArrayList<Integer> derivedIntensity = new ArrayList<Integer>();
        for (int i = 0; i < columnIntensity.size(); i++) {
            int derived = (i - kernelSize >= 0) ? (columnIntensity.get(i) - columnIntensity.get(i - kernelSize)) / kernelSize : 0;
            derivedIntensity.add(derived);
        }

        // Smooth the graph??
        kernelSize = 7;
        ArrayList<Integer> smoothedIntensity = new ArrayList<Integer>();
        for (int i = 0; i < derivedIntensity.size(); i++) {
            int absValue = Math.abs(derivedIntensity.get(i));

            for (int j = 0; j < kernelSize; j++) {
                int leftValue = (i - j >= 0) ? derivedIntensity.get(i - j) : 0,
                        rightValue = (i + j) < derivedIntensity.size() ? derivedIntensity.get(i + j) : 0;

                absValue += Math.abs(leftValue) + Math.abs(rightValue);
            }

            smoothedIntensity.add(absValue);
        }

        // Average the values
        averageFilter(smoothedIntensity, 2);

        Integer[] largestCluster = {0, 0};
        for (int i = 0; i < smoothedIntensity.size(); i++) {
            int currentValue = smoothedIntensity.get(i);

            if (currentValue > 0) {

                for (int k = i; k < smoothedIntensity.size(); k++) {
                    int nextValue = smoothedIntensity.get(k);

                    if (nextValue == 0) {
                        if ((k - i) > (largestCluster[1] - largestCluster[0])) {
                            largestCluster[0] = i;
                            largestCluster[1] = k;
                        }

                        i = k;
                        break;
                    }
                }
            }
        }

        return largestCluster;
    }

    private ArrayList<Integer> columnIntensity(Mat image) {
        ArrayList<Integer> intensityList = new ArrayList<Integer>();

        for (int column = 0; column < image.cols(); column++) {

            int sumOfIntensity = 0;
            for (int row = 0; row < image.rows(); row++) {
                sumOfIntensity += image.get(row, column)[0];
            }

            intensityList.add(sumOfIntensity);
        }

        return intensityList;
    }

    private void averageFilter(ArrayList<Integer> list, int size) {

        if (list.size() == 0) return;

        // Calculate total average
        int sum = 0, listSize = list.size();
        for (int i = 0; i < listSize; i++) {
            sum += Math.abs(list.get(i));
        }
        int average = sum / listSize;

        // Average values in list
        for (int i = 0; i < listSize; i++) {

            int averagedValue = list.get(i);

            for (int j = 0; j < size; j++) {
                int leftValue = i - j >= 0 ? list.get(i - j) : 0,
                        rightValue = i + j < listSize ? list.get(i + j) : 0;

                averagedValue += leftValue + rightValue;
            }

            averagedValue /= size + 1;


            list.set(i, Math.abs(averagedValue) >= average / 1.5 ? averagedValue : 0);
        }
    }

    private Mat rotateAndCrop(Mat image, Mat mask) {

        if (image.size().area() == 0 && mask.size().area() == 0)
            return null;

        // Create list of points
        ArrayList<Point> points = new ArrayList<Point>();
        for (int row = 0; row < mask.rows(); row++) {
            for (int col = 0; col < mask.cols(); col++) {
                double point = mask.get(row, col)[0];

                if (point > 0)
                    points.add(new Point(col, row));
            }
        }

        MatOfPoint2f pointsMat = new MatOfPoint2f();
        pointsMat.fromList(points);
        RotatedRect box = Imgproc.minAreaRect(pointsMat);

        Point[] vertices = new Point[4];
        box.points(vertices);
        sortVertices(vertices);

        Mat verticesMat = new Mat(4, 1, CvType.CV_32FC2);
        verticesMat.put(0, 0,
                vertices[0].x, vertices[0].y,
                vertices[1].x, vertices[1].y,
                vertices[2].x, vertices[2].y,
                vertices[3].x, vertices[3].y
        );

        double width = (box.size.height < box.size.width) ? box.size.width : box.size.height;
        double height = (width == box.size.width) ? box.size.height : box.size.width;

        Point[] outputVertices = {
                new Point(0, 0),
                new Point(width - 1, 0),
                new Point(width - 1, height - 1),
                new Point(0, height - 1)
        };
        sortVertices(outputVertices);

        Mat warpMat = new Mat(4, 1, CvType.CV_32FC2);
        warpMat.put(0, 0,
                outputVertices[0].x, outputVertices[0].y,
                outputVertices[1].x, outputVertices[1].y,
                outputVertices[2].x, outputVertices[2].y,
                outputVertices[3].x, outputVertices[3].y
        );

        Size warpSize = new Size(width, height);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(verticesMat, warpMat);
        Mat warpedMat = new Mat(warpSize, image.type());
        Imgproc.warpPerspective(image, warpedMat, perspectiveTransform, warpSize,
                Imgproc.BORDER_REPLICATE);

        return warpedMat;
    }

    private void sortVertices(Point[] vertices) {
        // Sort all vertices based on y coordinate
        Arrays.sort(vertices, new Comparator<Point>() {
            public int compare(Point a, Point b) {
                return (a.x < b.x) ? -1 : 1;
            }
        });

        // Sort upper vertices based on x-coordinate
        Point[] firstPoints = {vertices[0], vertices[1]};
        Arrays.sort(firstPoints, new Comparator<Point>() {
            public int compare(Point a, Point b) {
                return (a.y > b.y) ? -1 : 1;
            }
        });
        vertices[0] = firstPoints[0];
        vertices[1] = firstPoints[1];

        // Sort lower vertices based on x-coordinate
        Point[] lastPoints = {vertices[2], vertices[3]};
        Arrays.sort(lastPoints, new Comparator<Point>() {
            public int compare(Point a, Point b) {
                return (a.y < b.y) ? -1 : 1;
            }
        });
        vertices[2] = lastPoints[0];
        vertices[3] = lastPoints[1];
    }

    private void sortContours(ArrayList<MatOfPoint> contours) {
        Collections.sort(contours, new ContourSizeComparator());

        if (contours.size() > 6)
            contours.subList(6, contours.size()).clear();

        Collections.sort(contours, new ContourLocationComparator());
    }
}

class ContourSizeComparator implements Comparator<MatOfPoint> {
    @Override
    public int compare(MatOfPoint a, MatOfPoint b) {
        Rect boundingBoxA = Imgproc.boundingRect(a);
        Rect boundingBoxB = Imgproc.boundingRect(b);

        if (boundingBoxA.size().area() > boundingBoxB.size().area())
            return -1;
        else if (boundingBoxA.size().area() < boundingBoxB.size().area())
            return 1;
        else
            return 0;
    }
}

class ContourLocationComparator implements Comparator<MatOfPoint> {
    @Override
    public int compare(MatOfPoint a, MatOfPoint b) {
        Rect boundingBoxA = Imgproc.boundingRect(a);
        Rect boundingBoxB = Imgproc.boundingRect(b);

        if (boundingBoxA.x < boundingBoxB.x)
            return -1;
        else if (boundingBoxA.x > boundingBoxB.x)
            return 1;
        else
            return 0;
    }
}