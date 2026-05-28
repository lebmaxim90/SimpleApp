package com.example.simpleapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GalleryFragment extends Fragment {

    private RecyclerView galleryRecyclerView;
    private Button addPhotoButton, addVideoButton;
    private GalleryAdapter galleryAdapter;
    private List<MediaItem> userMediaList;

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    saveMediaToLocalList(uri, "photo");
                }
            });

    private final ActivityResultLauncher<String> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    saveMediaToLocalList(uri, "video");
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        galleryRecyclerView = view.findViewById(R.id.galleryRecyclerView);
        addPhotoButton = view.findViewById(R.id.addPhotoButton);
        addVideoButton = view.findViewById(R.id.addVideoButton);

        userMediaList = new ArrayList<>();
        galleryAdapter = new GalleryAdapter(getContext(), userMediaList, item -> openMedia(item));
        galleryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        galleryRecyclerView.setAdapter(galleryAdapter);

        // Загружаем сохранённые медиа
        loadSavedMedia();

        addPhotoButton.setOnClickListener(v -> checkPermissionsAndPickMedia("photo"));
        addVideoButton.setOnClickListener(v -> checkPermissionsAndPickMedia("video"));

        return view;
    }

    private void checkPermissionsAndPickMedia(String type) {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
        } else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            if (type.equals("photo")) {
                photoPickerLauncher.launch("image/*");
            } else {
                videoPickerLauncher.launch("video/*");
            }
        } else {
            requestPermissions(permissions, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Разрешения получены", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Нужны разрешения для доступа к медиа", Toast.LENGTH_LONG).show();
        }
    }

    private void saveMediaToLocalList(Uri uri, String mediaType) {
        String fileName = getFileName(uri);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String dateStr = sdf.format(new Date());

        MediaItem item = new MediaItem(uri.toString(), fileName, dateStr, mediaType, uri);
        userMediaList.add(0, item); // Добавляем в начало списка

        saveMediaToPrefs();
        galleryAdapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "Добавлено: " + fileName, Toast.LENGTH_SHORT).show();
    }

    private void loadSavedMedia() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("GalleryPrefs", 0);
        String savedJson = prefs.getString("user_media", "");
        if (!savedJson.isEmpty()) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<List<MediaItem>>(){}.getType();
                List<MediaItem> loaded = gson.fromJson(savedJson, type);
                if (loaded != null) {
                    userMediaList.addAll(loaded);
                    galleryAdapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveMediaToPrefs() {
        Gson gson = new Gson();
        String json = gson.toJson(userMediaList);
        requireActivity().getSharedPreferences("GalleryPrefs", 0)
                .edit()
                .putString("user_media", json)
                .apply();
    }

    private String getFileName(Uri uri) {
        String fileName = "";
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    fileName = cursor.getString(nameIndex);
                }
            }
        }
        if (fileName.isEmpty()) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                fileName = cut != -1 ? path.substring(cut + 1) : path;
            }
        }
        return fileName;
    }

    private void openMedia(MediaItem item) {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
        if (item.getMediaType().equals("photo")) {
            intent.setDataAndType(item.getContentUri(), "image/*");
        } else {
            intent.setDataAndType(item.getContentUri(), "video/*");
        }
        startActivity(intent);
    }

    // ========== АДАПТЕР ВЫНЕСЕН В ОТДЕЛЬНЫЙ КЛАСС ==========

    public static class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

        private android.content.Context context;
        private List<MediaItem> items;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(MediaItem item);
        }

        public GalleryAdapter(android.content.Context context, List<MediaItem> items, OnItemClickListener listener) {
            this.context = context;
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_gallery, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MediaItem item = items.get(position);
            if (item.getMediaType().equals("photo")) {
                Glide.with(context)
                        .load(item.getContentUri())
                        .into(holder.imageView);
                holder.typeIcon.setText("📷");
            } else {
                Glide.with(context)
                        .load(item.getContentUri())
                        .into(holder.imageView);
                holder.typeIcon.setText("🎬");
            }
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView typeIcon;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.galleryImage);
                typeIcon = itemView.findViewById(R.id.mediaTypeIcon);
            }
        }
    }
}