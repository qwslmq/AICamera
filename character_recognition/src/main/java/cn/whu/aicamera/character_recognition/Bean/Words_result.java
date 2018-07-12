package cn.whu.aicamera.character_recognition.Bean;

public class Words_result {
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
}
