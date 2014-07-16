package com.cgi.roadeye.android;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

/**
 * Created by valorcurse on 09/04/14.
 */
class FindBands implements Runnable {

    private Mat sourceImage;
    private double averageBand;
    private double clippingConstant;
    private Vector<Plate> bandsBuffer;
    int nrOfBands = 3;


    public FindBands(Mat sourceImage, Vector<Plate> bandsBuffer, double averageBand, double clippingConstant) {
        this.sourceImage = sourceImage;
        this.averageBand = averageBand;
        this.clippingConstant = clippingConstant;
        this.bandsBuffer = bandsBuffer;
    }

    public void run() {

        Date startTime = new Date();

        Mat hsvImage = new Mat();

        // Convert to HSV
        Imgproc.cvtColor(sourceImage, hsvImage, Imgproc.COLOR_RGB2HSV, 0);

        // Mask all colors except yellow
        Mat maskImage = new Mat();

        // Yellow Hue -> 20 - 30
        Core.inRange(hsvImage, new Scalar(20, 100, 100), new Scalar(30, 255, 255), maskImage);

        // Erode mask to remove noise
        int erosionSize = 1;
//        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(2 * erosionSize + 1, 2 * erosionSize + 1),
//                new Point(erosionSize, erosionSize));
//        Imgproc.erode(maskImage, maskImage, kernel);

        // Calculate vertical edges
        Mat verticalEdges = new Mat();
        Imgproc.Sobel(maskImage, verticalEdges, maskImage.depth(), 1, 0, 3, 1, 0, Imgproc.BORDER_DEFAULT);

        // Find possible vertical position of plates
        ArrayList<Integer[]> bands = clipBands(verticalEdges);

        if (bands != null && bands.size() > 0) {

            // Retrieve bands areas
            for (Integer[] band : bands) {

                if (band[1] - band[0] > 0) {

                    Mat bandImage = new Mat(maskImage, new Rect(new Point(0, band[0]),
                            new Point(maskImage.cols(), band[1])));

                    if (bandImage.size().area() > 0 && band[1] - band[0] > 0) {

                        Plate newPlate = new Plate(sourceImage);
                        newPlate.setBandImage(bandImage);
                        newPlate.setY1(band[0]);
                        newPlate.setY2(band[1]);
                        newPlate.setStartTime(startTime);

                        bandsBuffer.add(newPlate);
                    }
                }
            }
        }
    }

    private ArrayList<Integer[]> clipBands(Mat image) {
        ArrayList<Integer> rowIntensity = rowIntensity(image);

        if (rowIntensity.size() == 0) return new ArrayList<Integer[]>();

        averageFilter(rowIntensity, (int) averageBand);

//            float clippingConstant = 0.20f;
        ArrayList<Integer[]> clippingIndexes = new ArrayList<Integer[]>();
        for (int j = 0; j < nrOfBands; j++) {

            int peakValue = Collections.max(rowIntensity),
                    peakIndex = rowIntensity.indexOf(peakValue);

            int leftIndex = 0, rightIndex = 0;

            // Find left boundary
            for (int i = peakIndex; i >= 1; i--) {
                int currentPosition = rowIntensity.get(i);

                if (currentPosition <= peakValue * clippingConstant) {
                    leftIndex = i + 1;
                    break;
                }
            }
            leftIndex = Math.max(0, leftIndex);

            // Find right boundary
            for (int i = peakIndex; i < rowIntensity.size(); i++) {
                int currentPosition = rowIntensity.get(i);

                if (currentPosition <= peakValue * clippingConstant) {
                    rightIndex = i - 1;
                    break;
                }
            }
            rightIndex = Math.min(rowIntensity.size(), rightIndex);

            // Zeroize list
            for (int i = leftIndex; i <= rightIndex; i++) {
                rowIntensity.set(i, 0);
            }

            clippingIndexes.add(new Integer[]{leftIndex, rightIndex});

//                        new Point(verticalHistogram.width(), clipIndex[1] + heightOffset),
//                        new Scalar(255, 0, 0));
//            }
        }

        return clippingIndexes;
    }

    private ArrayList<Integer> rowIntensity(Mat image) {
        ArrayList<Integer> intensityList = new ArrayList<Integer>();

        for (int row = 0; row < image.rows(); row++) {

            int sumOfIntensity = 0;
            for (int col = 0; col < image.cols(); col++) {
                sumOfIntensity += image.get(row, col)[0];
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
}