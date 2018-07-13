package cn.whu.aicamera.character_recognition.Bean;


public class Probability {

    private double average;
    private double min;
    private int variance;
    public void setAverage(double average) {
        this.average = average;
    }
    public double getAverage() {
        return average;
    }

    public void setMin(double min) {
        this.min = min;
    }
    public double getMin() {
        return min;
    }

    public void setVariance(int variance) {
        this.variance = variance;
    }
    public int getVariance() {
        return variance;
    }

}
