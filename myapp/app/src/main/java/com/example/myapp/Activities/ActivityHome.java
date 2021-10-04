package com.example.myapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ActivityHome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Set Night mode
        // AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        LinearLayout llMutualFund = findViewById(R.id.home_activity_mutual_fund_layout);
        LinearLayout llBanks = findViewById(R.id.home_activity_banks_layout);
        LinearLayout llCrypto = findViewById(R.id.home_activity_crypto_layout);
        LinearLayout llStocks = findViewById(R.id.home_activity_stocks_layout);

        llMutualFund.setOnClickListener(view -> {
            Intent intent = new Intent(ActivityHome.this, ActivityMutualFund.class);
            startActivity(intent);
        });
        llBanks.setOnClickListener(view -> {
            Intent intent = new Intent(ActivityHome.this, ActivityBanks.class);
            startActivity(intent);
        });
        llCrypto.setOnClickListener(view -> {
            Intent intent = new Intent(ActivityHome.this, ActivityCrypto.class);
            startActivity(intent);
        });
        llStocks.setOnClickListener(view -> {
            Intent intent = new Intent(ActivityHome.this, ActivityStocks.class);
            startActivity(intent);
        });

        // Set bottom app bar
        BottomNavigationView bnv = findViewById(R.id.bottom_app_bar_navigation_view);
        bnv.setSelectedItemId(R.id.bottom_menu_home);
        bnv.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                // To remove the warning you might use if/else
                case R.id.bottom_menu_home:
                    return true;
                case R.id.bottom_menu_activities:
                    startActivity(new Intent(getApplicationContext(), ActivityTransactions.class));
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.bottom_menu_privacy:
                    startActivity(new Intent(getApplicationContext(), ActivityPrivacy.class));
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.bottom_menu_settings:
                    startActivity(new Intent(getApplicationContext(), ActivitySettings.class));
                    overridePendingTransition(0, 0);
                    return true;
            }
            return false;
        });
    }
}