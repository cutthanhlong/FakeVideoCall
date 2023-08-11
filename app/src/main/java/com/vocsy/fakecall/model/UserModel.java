package com.vocsy.fakecall.model;

public class UserModel {
    public int id;
    public String name;
    public String phonenumber;
    public String photo;
    public String background;
    public String video;
    public String type;
    public String email;
    public String audio;
    public String favourite;
    public String avb;


    public UserModel() {

    }

    public UserModel(int id, String name, String phonenumber, String photo, String background, String video, String type, String email, String audio, String favourite, String avb) {
        this.id = id;
        this.name = name;
        this.phonenumber = phonenumber;
        this.photo = photo;
        this.background = background;
        this.video = video;
        this.type = type;
        this.email = email;
        this.audio = audio;
        this.favourite = favourite;
        this.avb = avb;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public String getFavourite() {
        return favourite;
    }

    public void setFavourite(String favourite) {
        this.favourite = favourite;
    }

    public String getAvb() {
        return avb;
    }

    public void setAvb(String avb) {
        this.avb = avb;
    }
}
