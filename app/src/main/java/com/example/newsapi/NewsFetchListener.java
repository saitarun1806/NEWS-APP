package com.example.newsapi;

import java.util.List;

public interface NewsFetchListener {
    void onNewsFetched(List<NewsItem> newsList);
    void onError(String error);
}