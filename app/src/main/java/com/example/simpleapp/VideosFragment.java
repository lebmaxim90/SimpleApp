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
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class VideosFragment extends Fragment {

    private PlayerView playerView;
    private ExoPlayer player;
    private Button selectVideoButton;

    private final ActivityResultLauncher<String> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && player != null) {
                    player.setMediaItem(MediaItem.fromUri(uri));
                    player.prepare();
                    player.play();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos, container, false);

        playerView = view.findViewById(R.id.playerView);
        selectVideoButton = view.findViewById(R.id.selectVideoButton);

        // Создаём плеер
        player = new ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(player);

        selectVideoButton.setOnClickListener(v -> checkPermissionsAndPickVideo());

        return view;
    }

    private void checkPermissionsAndPickVideo() {
        String[] permissions = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? new String[]{Manifest.permission.READ_MEDIA_VIDEO}
                : new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            videoPickerLauncher.launch("video/*");
        } else {
            requestPermissions(permissions, 200);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 200 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            videoPickerLauncher.launch("video/*");
        } else {
            Toast.makeText(getContext(), "Нужны разрешения для доступа к видео", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player != null) player.release();
    }
}