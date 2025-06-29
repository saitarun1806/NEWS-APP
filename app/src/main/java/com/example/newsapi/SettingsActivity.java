package com.example.newsapi;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    Spinner languageSpinner, categorySpinner;
    Button saveButton;

    String[] langs = {"English", "Hindi", "Telugu"};
    String[] langCodes = {"en", "hi", "te"};

    String[] categories = {"Top", "Sports", "Business", "Education", "Politics"};
    String[] catCodes = {"top", "sports", "business", "education", "politics"};

    DatabaseReference userRef;
    String uid;

    boolean isSettingLanguageSpinner = false;
    boolean isSettingCategorySpinner = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        languageSpinner = findViewById(R.id.languageSpinner);
        categorySpinner = findViewById(R.id.categorySpinner);
        saveButton = findViewById(R.id.saveButton);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        // Set up spinners
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, langs);
        languageSpinner.setAdapter(langAdapter);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(catAdapter);

        // Load existing saved preferences
        loadUserPreferences();

        // Save button
        saveButton.setOnClickListener(v -> {
            int langPos = languageSpinner.getSelectedItemPosition();
            int catPos = categorySpinner.getSelectedItemPosition();

            String selectedLang = langCodes[langPos];
            String selectedCat = catCodes[catPos];

            userRef.child("preferredLanguage").setValue(selectedLang);
            userRef.child("preferredCategory").setValue(selectedCat)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Preferences saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void loadUserPreferences() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String savedLanguage = snapshot.child("preferredLanguage").getValue(String.class);
                String savedCategory = snapshot.child("preferredCategory").getValue(String.class);

                if (savedLanguage != null) {
                    for (int i = 0; i < langCodes.length; i++) {
                        if (langCodes[i].equals(savedLanguage)) {
                            isSettingLanguageSpinner = true;
                            languageSpinner.setSelection(i);
                            break;
                        }
                    }
                }

                if (savedCategory != null) {
                    for (int i = 0; i < catCodes.length; i++) {
                        if (catCodes[i].equals(savedCategory)) {
                            isSettingCategorySpinner = true;
                            categorySpinner.setSelection(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(SettingsActivity.this, "Error loading preferences", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
