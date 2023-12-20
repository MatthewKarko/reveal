package com.example.revealapp.model;

public class User {
    private String userId;
    private String name;
    private String username;
    private String email;
    private String userProfilePhoto;

    public User() {
    }

    public User(String userId, String name, String email, String userProfilePhoto, String username) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.userProfilePhoto = userProfilePhoto;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserProfilePhoto() {
        return userProfilePhoto;
    }

    public void setUserProfilePhoto(String userProfilePhoto) {
        this.userProfilePhoto = userProfilePhoto;
    }
}