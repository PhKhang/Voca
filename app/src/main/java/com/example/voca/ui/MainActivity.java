package com.example.voca.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.voca.R;
import com.example.voca.databinding.ActivityMainBinding;
import com.example.voca.ui.auth.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    Uri fileUri;
    private ActivityMainBinding binding;
    private MaterialToolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
//        Button pick = findViewById(R.id.pick);
//        pick.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // Choose a directory using the system's file picker.
//                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.setType("*/*");
//
//                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, "");
//
//                startActivityForResult(intent, 1);
//            }
//        });

//        Button up = findViewById(R.id.up);
//        up.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                System.out.println("Clicked");
//                new FileUploader().run(getApplicationContext(), fileUri, new FileUploader.OnUploadCompleteListener() {
//                    @Override
//                    public void onSuccess(String url) {
//
//                    }
//
//                    @Override
//                    public void onFailure() {
//
//                    }
//                });
//            }
//        });

//        Button delete = findViewById(R.id.delete);
//        delete.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                System.out.println("Delete clicked");
//                new FileUploader().deleteFileByURL("https://pub-9baa3a81ecf34466aeb5591929ebf0b3.r2.dev/Indoor%20Kei%20Nara%20Trackmaker%20(Instrumental).mp3");
//            }
//        });

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        toolbar = findViewById(R.id.topAppBar);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_sing, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_profile).build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            toolbar.setTitle(destination.getLabel());
        });
//        topAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem menuItem) {
//                if (menuItem.getItemId() == R.id.action_sign_out) {
//                    Toast.makeText(MainActivity.this, "Signing out...", Toast.LENGTH_SHORT).show();
//                    return true;
//                } else {
//                    return false;
//                }
//            }
//        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == 1 && resultCode == AppCompatActivity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            if (resultData != null) {
                fileUri = resultData.getData();
                System.out.println("The file is: " + fileUri.getLastPathSegment());
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa ngăn xếp
        startActivity(intent);
        finish();
    }
}