package com.example.newsapi;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NewsFetcher {

    private static final String API_KEY = "pub_04b0d95dc1c44916b9bccfc1441bb516";
    private static final String BASE_URL = "https://newsdata.io/api/1/news";
    public static String nextPageToken = null;

    public static void fetchNews(Context context, String language, String country, String category, String keyword, NewsFetchListener listener) {
        StringBuilder url = new StringBuilder(BASE_URL + "?apikey=" + API_KEY);

        if (language != null && !language.isEmpty()) url.append("&language=").append(language);
        if (country != null && !country.isEmpty()) url.append("&country=").append(country);
        if (category != null && !category.isEmpty()) url.append("&category=").append(category);
        if (keyword != null && !keyword.isEmpty()) url.append("&q=").append(keyword);
        if (nextPageToken != null) url.append("&page=").append(nextPageToken);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url.toString(), null,
                response -> {
                    try {
                        List<NewsItem> newsList = new ArrayList<>();
                        JSONArray array = response.getJSONArray("results");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            newsList.add(new NewsItem(
                                    obj.optString("title"),
                                    obj.optString("description"),
                                    obj.optString("image_url"),
                                    obj.optString("link"),
                                    obj.optString("pubDate")
                            ));
                        }
                        nextPageToken = response.optString("nextPage", null);
                        listener.onNewsFetched(newsList);
                    } catch (Exception e) {
                        listener.onError("Parsing error: " + e.getMessage());
                    }
                },
                error -> listener.onError("Network error: " + error.getMessage())
        );

        Volley.newRequestQueue(context).add(request);
    }
}