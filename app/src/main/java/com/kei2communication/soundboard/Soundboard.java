package com.kei2communication.soundboard;

import java.util.ArrayList;

public class Soundboard implements Comparable{

    public String name;
    private String image;
    private ArrayList<SBCard> soundboardCards;

    public Soundboard(String name, String image, ArrayList<SBCard> soundboardCards){
        this.name = name;
        this.image = image;
        this.soundboardCards = soundboardCards;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public void setImage(String image){
        this.image = image;
    }
    public String getImage() {
        return image;
    }
    public ArrayList<SBCard> getSoundboardCards() {
        return soundboardCards;
    }

    @Override
    public int compareTo(Object obj) {
        if(obj instanceof Soundboard) {
            Soundboard sb = (Soundboard) obj;
            return name.compareTo(sb.getName());
        }
        return 0;
    }
}
