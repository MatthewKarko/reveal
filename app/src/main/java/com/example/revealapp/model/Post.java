package com.example.revealapp.model;


import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.PropertyName;

public class Post {
    // PropertyName makes it the same name as the firebase db
    private String id;

    @PropertyName("Title")
    private String postTitle;

    @PropertyName("Theme")
    private DocumentReference theme;

    @PropertyName("User")
    private DocumentReference userId;

    private String userName;

    @PropertyName("Picture")
    private String picture;

    private String userPhotoProfile;

    @PropertyName("NumLikes")
    private int numOfLike;

    @PropertyName("NumDislikes")
    private int numOfDislike;

    private String postDescription;
    private String themeTitle;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Post(String id, DocumentReference userId, DocumentReference theme, String postTitle, String userName,String picture,
                int numOfLike, int numOfDislike, String userPhotoProfile, String username, String postDescription, String themeTitle) {
        this.id = id;
        this.userId = userId;
        this.theme = theme;
        this.postTitle = postTitle;
        this.picture = picture;
        this.numOfLike = numOfLike;
        this.numOfDislike = numOfDislike;
        this.userPhotoProfile = userPhotoProfile;
        this.postDescription = postDescription;
        this.themeTitle = themeTitle;
        this.userName = userName;
    }

    public Post(){}

    @PropertyName("User")
    public DocumentReference getUserId() {
        return userId;
    }

    @PropertyName("User")
    public void setUserId(DocumentReference userId) {
        this.userId = userId;
    }

    public DocumentReference getTheme() {
        return theme;
    }

    public void setTheme(DocumentReference theme) {
        this.theme = theme;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }




    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    @PropertyName("NumLikes")
    public int getNumOfLike() {
        return numOfLike;
    }

    @PropertyName("NumLikes")
    public void setNumOfLike(int numOfLike) {
        this.numOfLike = numOfLike;
    }

    @PropertyName("NumDislikes")
    public int getNumOfDislike() {
        return numOfDislike;
    }

    @PropertyName("NumDislikes")
    public void setNumOfDislike(int numOfDislike) {
        this.numOfDislike = numOfDislike;
    }

    public String getUserPhotoProfile() {
        return userPhotoProfile;
    }

    public void setUserPhotoProfile(String userPhotoProfile) {
        this.userPhotoProfile = userPhotoProfile;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }


    public String getPostDescription() {
        return postDescription;
    }

    public void setPostDescription(String postDescription) {
        this.postDescription = postDescription;
    }

    public String getThemeTitle() {
        return themeTitle;
    }

    public void setThemeTitle(String themeTitle) {
        this.themeTitle = themeTitle;
    }
}