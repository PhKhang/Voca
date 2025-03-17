package com.example.voca.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.voca.ui.LoginActivity;
import com.example.voca.ui.ProfileActivity;
import com.example.voca.ui.SongsManagementActivity;
import com.example.voca.ui.record.RecordActivity;
import com.example.voca.databinding.FragmentHomeBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        FirebaseApp.initializeApp(requireContext());
        binding = FragmentHomeBinding.inflate(inflater, container, false);


        binding.btnSignout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "signout", Toast.LENGTH_SHORT).show();
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(requireContext(), "Không có người dùng nào đăng nhập!", Toast.LENGTH_SHORT).show();
            }

            if (user != null) {
                for (UserInfo profile : user.getProviderData()) {
                    String providerId = profile.getProviderId();

                    if (providerId.equals("google.com")) {
                        // Đăng xuất Google
                        GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
                        Toast.makeText(requireContext(), "Đăng xuất gg sign in", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            mAuth.signOut();

            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish(); // Đóng MainActivity
        });


        binding.openRecordingPage.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), RecordActivity.class);
            startActivity(intent);
        });

        binding.openSongsManagementPage.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SongsManagementActivity.class);
            startActivity(intent);
        });

        binding.openProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ProfileActivity.class);
            startActivity(intent);
        });

        View root = binding.getRoot();
        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}