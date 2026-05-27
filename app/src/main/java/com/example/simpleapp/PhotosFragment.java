package com.example.simpleapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;

public class PhotosFragment extends Fragment {

    private ImageView photoPreview;
    private Button selectPhotoButton;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Glide.with(this).load(uri).into(photoPreview);
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photos, container, false);

        photoPreview = view.findViewById(R.id.photoPreview);
        selectPhotoButton = view.findViewById(R.id.selectPhotoButton);

        selectPhotoButton.setOnClickListener(v -> checkPermissionsAndPickImage());

        return view;
    }

    private void checkPermissionsAndPickImage() {
        String[] permissions = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? new String[]{Manifest.permission.READ_MEDIA_IMAGES}
                : new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            imagePickerLauncher.launch("image/*");
        } else {
            requestPermissions(permissions, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch("image/*");
        } else {
            Toast.makeText(getContext(), "Нужны разрешения для доступа к фото", Toast.LENGTH_LONG).show();
        }
    }
}