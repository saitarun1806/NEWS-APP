package com.example.newsapi;

public class NewsItem {
    public String title;
    public String description;
    public String image;
    public String url;
    public String published;

    public NewsItem(String title, String description, String image, String url, String published) {
        this.title = title;
        this.description = description;
        this.image = image;
        this.url = url;
        this.published = published;
    }
}