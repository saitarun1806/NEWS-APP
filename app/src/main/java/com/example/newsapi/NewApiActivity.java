package com.example.newsapi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class NewApiActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    NewsAdapter adapter;

    String selectedLanguage = "en";
    String selectedCountry = "in";
    String selectedCategory = "top";
    String searchKeyword = null;

    EditText searchBar;
    Spinner languageSpinner, categorySpinner;
    Button searchButton;
    View nextPageButton;
    View headerView;
    TextView userEmail;

    String uid;
    DatabaseReference userRef;

    String[] langs = {"English", "Hindi", "Telugu"};
    String[] langCodes = {"en", "hi", "te"};

    String[] categories = {"Top", "Sports", "Business", "Education", "Politics"};
    String[] catCodes = {"top", "sports", "business", "education", "politics"};

    boolean isSettingLanguageSpinner = false;
    boolean isSettingCategorySpinner = false;
    boolean isLoadingPreferences = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_api);
        NavigationView navigationView = findViewById(R.id.nav_view);
        headerView = navigationView.getHeaderView(0);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userEmail = headerView.findViewById(R.id.user_email);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ImageView menuIcon = findViewById(R.id.menuIcon);
        FirebaseAuth myauth=FirebaseAuth.getInstance();
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        searchBar = findViewById(R.id.searchBar);
        searchButton = findViewById(R.id.searchButton);
        languageSpinner = findViewById(R.id.languageFilter);
        categorySpinner = findViewById(R.id.categoryFilter);
        nextPageButton = findViewById(R.id.nextPageButton);
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color));
        FirebaseUser user = myauth.getCurrentUser();
        if (user != null) {
            userEmail.setText(user.getEmail());
        }


        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            NewsFetcher.nextPageToken = null;
            fetchNews();
            swipeRefreshLayout.setRefreshing(false);
        });


        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_home) {
                handleHomePressed();
            } else if (id == R.id.action_logout) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(NewApiActivity.this, LoginActivity.class));
                finish();
            } else if (id == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, langs);
        languageSpinner.setAdapter(langAdapter);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(catAdapter);

        // Load preferences from Firebase once
        loadUserPreferences();

        searchButton.setOnClickListener(v -> {
            String query = searchBar.getText().toString().trim();
            searchKeyword = query.isEmpty() ? null : query;
            NewsFetcher.nextPageToken = null;
            fetchNews();
        });

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (isSettingLanguageSpinner || isLoadingPreferences) {
                    isSettingLanguageSpinner = false;
                    return;
                }
                selectedLanguage = langCodes[pos];
                NewsFetcher.nextPageToken = null;
                fetchNews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (isSettingCategorySpinner || isLoadingPreferences) {
                    isSettingCategorySpinner = false;
                    return;
                }
                selectedCategory = catCodes[pos];
                NewsFetcher.nextPageToken = null;
                fetchNews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        nextPageButton.setOnClickListener(v -> {
            if (NewsFetcher.nextPageToken != null) {
                fetchNews();
            } else {
                Toast.makeText(this, "No more pages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleHomePressed() {
        searchBar.setText("");
        searchKeyword = null;
        NewsFetcher.nextPageToken = null;
        loadUserPreferences();
    }

    private void loadUserPreferences() {
        isLoadingPreferences = true;
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String savedLanguage = snapshot.child("preferredLanguage").getValue(String.class);
                String savedCategory = snapshot.child("preferredCategory").getValue(String.class);

                if (savedLanguage == null) savedLanguage = "en";
                if (savedCategory == null) savedCategory = "top";

                for (int i = 0; i < langCodes.length; i++) {
                    if (langCodes[i].equals(savedLanguage)) {
                        isSettingLanguageSpinner = true;
                        languageSpinner.setSelection(i);
                        selectedLanguage = savedLanguage;
                        break;
                    }
                }

                for (int i = 0; i < catCodes.length; i++) {
                    if (catCodes[i].equals(savedCategory)) {
                        isSettingCategorySpinner = true;
                        categorySpinner.setSelection(i);
                        selectedCategory = savedCategory;
                        break;
                    }
                }

                fetchNews();
                isLoadingPreferences = false;
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(NewApiActivity.this, "Failed to load preferences", Toast.LENGTH_SHORT).show();
                fetchNews();
                isLoadingPreferences = false;
            }
        });
    }

    private void fetchNews() {
        NewsFetcher.fetchNews(this, selectedLanguage, selectedCountry, selectedCategory, searchKeyword, new NewsFetchListener() {
            @Override
            public void onNewsFetched(List<NewsItem> newsList) {
                if (newsList == null || newsList.isEmpty()) {
                    Toast.makeText(NewApiActivity.this, "No news available for this filter", Toast.LENGTH_SHORT).show();
                    recyclerView.setAdapter(null);
                } else {
                    adapter = new NewsAdapter(NewApiActivity.this, newsList);
                    recyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(NewApiActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
