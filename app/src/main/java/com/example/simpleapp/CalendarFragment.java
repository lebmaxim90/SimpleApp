package com.example.simpleapp;

import android.Manifest;
import android.content.ContentResolver;
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
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment {

    private TextView monthYearText;
    private GridLayout calendarGrid;
    private RecyclerView mediaList;
    private ImageButton prevMonthBtn, nextMonthBtn;

    private Calendar currentCalendar;
    private SimpleDateFormat monthFormat = new SimpleDateFormat("LLLL yyyy", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    private Map<String, List<MediaItem>> mediaByDate; // Карта: дата -> список медиа
    private List<MediaItem> currentDateMedia;
    private MediaAdapter mediaAdapter;
    private String selectedDate;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        monthYearText = view.findViewById(R.id.monthYearText);
        calendarGrid = view.findViewById(R.id.calendarGrid);
        mediaList = view.findViewById(R.id.mediaList);
        prevMonthBtn = view.findViewById(R.id.prevMonthBtn);
        nextMonthBtn = view.findViewById(R.id.nextMonthBtn);

        currentCalendar = Calendar.getInstance();
        mediaByDate = new HashMap<>();
        currentDateMedia = new ArrayList<>();
        mediaAdapter = new MediaAdapter(currentDateMedia, item -> openMedia(item));
        mediaList.setLayoutManager(new LinearLayoutManager(getContext()));
        mediaList.setAdapter(mediaAdapter);

        // Запрашиваем разрешения
        checkPermissionsAndLoadMedia();

        prevMonthBtn.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        nextMonthBtn.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        return view;
    }

    private void checkPermissionsAndLoadMedia() {
        String[] permissions = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}
                : new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            loadAllMediaFromDevice();
            updateCalendar();
        } else {
            requestPermissions(permissions, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadAllMediaFromDevice();
            updateCalendar();
        } else {
            Toast.makeText(getContext(), "Нужны разрешения для доступа к медиа", Toast.LENGTH_LONG).show();
        }
    }

    private void loadAllMediaFromDevice() {
        try {
            mediaByDate.clear();
            ContentResolver resolver = requireContext().getContentResolver();

            // Проверяем разрешения перед загрузкой
            if (!hasStoragePermission()) {
                return;
            }

            // ========== ЗАГРУЗКА ФОТО ==========
            String[] photoProjection = {
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_TAKEN
            };

            Cursor photoCursor = null;
            try {
                photoCursor = resolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        photoProjection,
                        null,
                        null,
                        null
                );

                if (photoCursor != null && photoCursor.moveToFirst()) {
                    do {
                        try {
                            String path = photoCursor.getString(0);
                            String name = photoCursor.getString(1);
                            long dateTaken = photoCursor.getLong(2);

                            if (path != null && name != null && dateTaken > 0) {
                                String dateStr = convertTimestampToDate(dateTaken);
                                if (dateStr != null) {
                                    Uri uri = Uri.parse("file://" + path);
                                    MediaItem item = new MediaItem(path, name, dateStr, "photo", uri);

                                    if (!mediaByDate.containsKey(dateStr)) {
                                        mediaByDate.put(dateStr, new ArrayList<>());
                                    }
                                    mediaByDate.get(dateStr).add(item);
                                }
                            }
                        } catch (Exception e) {
                            // Пропускаем проблемный файл
                            e.printStackTrace();
                        }
                    } while (photoCursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (photoCursor != null && !photoCursor.isClosed()) {
                    photoCursor.close();
                }
            }

            // ========== ЗАГРУЗКА ВИДЕО ==========
            String[] videoProjection = {
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_TAKEN
            };

            Cursor videoCursor = null;
            try {
                videoCursor = resolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        videoProjection,
                        null,
                        null,
                        null
                );

                if (videoCursor != null && videoCursor.moveToFirst()) {
                    do {
                        try {
                            String path = videoCursor.getString(0);
                            String name = videoCursor.getString(1);
                            long dateTaken = videoCursor.getLong(2);

                            if (path != null && name != null && dateTaken > 0) {
                                String dateStr = convertTimestampToDate(dateTaken);
                                if (dateStr != null) {
                                    Uri uri = Uri.parse("file://" + path);
                                    MediaItem item = new MediaItem(path, name, dateStr, "video", uri);

                                    if (!mediaByDate.containsKey(dateStr)) {
                                        mediaByDate.put(dateStr, new ArrayList<>());
                                    }
                                    mediaByDate.get(dateStr).add(item);
                                }
                            }
                        } catch (Exception e) {
                            // Пропускаем проблемный файл
                            e.printStackTrace();
                        }
                    } while (videoCursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (videoCursor != null && !videoCursor.isClosed()) {
                    videoCursor.close();
                }
            }

            // Обновляем календарь после загрузки
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> updateCalendar());
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private String convertTimestampToDate(long timestamp) {
        if (timestamp <= 0) return null;
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return dateFormat.format(cal.getTime());
        } catch (Exception e) {
            return null;
        }
    }

    private void updateCalendar() {
        monthYearText.setText(monthFormat.format(currentCalendar.getTime()));
        drawCalendarDays();
    }

    private void drawCalendarDays() {
        calendarGrid.removeAllViews();

        // Дни недели
        String[] weekdays = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String day : weekdays) {
            TextView tv = new TextView(getContext());
            tv.setText(day);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv.setPadding(8, 8, 8, 8);
            tv.setTextColor(0xFF888888);
            calendarGrid.addView(tv);
        }

        // Первый день месяца
        Calendar tempCal = (Calendar) currentCalendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);
        int offset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7;

        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String currentDateStr = dateFormat.format(Calendar.getInstance().getTime());

        // Пустые ячейки перед 1-м числом
        for (int i = 0; i < offset; i++) {
            TextView empty = new TextView(getContext());
            empty.setVisibility(View.INVISIBLE);
            calendarGrid.addView(empty);
        }

        // Числа месяца
        for (int day = 1; day <= daysInMonth; day++) {
            tempCal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = dateFormat.format(tempCal.getTime());
            boolean hasMedia = mediaByDate.containsKey(dateStr);
            boolean isToday = dateStr.equals(currentDateStr);

            Button dayButton = new Button(getContext());
            dayButton.setText(String.valueOf(day));
            dayButton.setAllCaps(false);

            // Стилизация
            if (hasMedia) {
                dayButton.setBackgroundColor(0xFF4CAF50); // Зелёный фон — есть медиа
                dayButton.setTextColor(0xFFFFFFFF);
            } else if (isToday) {
                dayButton.setBackgroundColor(0xFF2196F3); // Синий фон — сегодня
                dayButton.setTextColor(0xFFFFFFFF);
            } else {
                dayButton.setBackgroundColor(0xFFEEEEEE);
                dayButton.setTextColor(0xFF000000);
            }

            // Обработчик клика
            String finalDateStr = dateStr;
            dayButton.setOnClickListener(v -> {
                selectedDate = finalDateStr;
                showMediaForDate(finalDateStr);
            });

            calendarGrid.addView(dayButton);
        }
    }

    private void showMediaForDate(String date) {
        List<MediaItem> items = mediaByDate.get(date);
        if (items != null && !items.isEmpty()) {
            currentDateMedia.clear();
            currentDateMedia.addAll(items);
            mediaAdapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "Найдено " + items.size() + " файлов", Toast.LENGTH_SHORT).show();
        } else {
            currentDateMedia.clear();
            mediaAdapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "Нет медиа на эту дату", Toast.LENGTH_SHORT).show();
        }
    }

    private void openMedia(MediaItem item) {
        // Открываем фото или видео
        if (item.getMediaType().equals("photo")) {
            // Можно открыть в галерее
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(item.getContentUri(), "image/*");
            startActivity(intent);
        } else {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(item.getContentUri(), "video/*");
            startActivity(intent);
        }
    }
}