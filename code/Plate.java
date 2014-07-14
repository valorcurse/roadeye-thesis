package com.cgi.roadeye.android;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by valorcurse on 15/04/14.
 */
public class Plate {

    private Mat sourceImage;
    private Mat bandImage;
    private Mat plateImage;
    private Date startTime, endTime;
    private int x1, y1, x2, y2;
    private String information;
    private ArrayList<Mat> letters;

    Plate(Mat sourceImage) {
        this.sourceImage = sourceImage;
    }

    public Mat getSourceImage() {
        return sourceImage;
    }

    public void setSourceImage(Mat sourceImage) {
        this.sourceImage = sourceImage;
    }

    public Mat getBandImage() {
        return bandImage;
    }

    public void setBandImage(Mat bandImage) {
        this.bandImage = bandImage;
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public Mat getPlateImage() {
        return plateImage;
    }

    public void setPlateImage(Mat plateImage) {
        this.plateImage = plateImage;
    }

    public ArrayList<Mat> getLetters() {
        return letters;
    }

    public void setLetters(ArrayList<Mat> letters) {
        this.letters = letters;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
