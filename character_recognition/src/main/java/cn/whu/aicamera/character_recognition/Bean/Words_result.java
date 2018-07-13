package cn.whu.aicamera.character_recognition.Bean;

import android.support.annotation.NonNull;

public class Words_result implements Comparable<Words_result> {
    private Probability probability;
    private Location location;
    private String words;
    public void setLocation(Location location) {
        this.location = location;
    }
    public Location getLocation() {
        return location;
    }

    public void setWords(String words) {
        this.words = words;
    }
    public String getWords() {
        return words;
    }

    public Probability getProbability() {
        return probability;
    }

    public void setProbability(Probability probability) {
        this.probability = probability;
    }

    /**
     * 根据Average置信度进行降序排序
     * @param words_result 要比较的对象
     * @return 当置信度大于比较对象时，返回-1， 小于返回1 ，等于则返回0
     */
    @Override
    public int compareTo(@NonNull Words_result words_result) {
        int CompareResult = 0;
        double diff = this.getProbability().getAverage() - words_result.getProbability().getAverage();
        if(diff > 0){
            CompareResult = -1;
        }else if (diff <0){
            CompareResult = 1;
        }
        return CompareResult;
    }
}
