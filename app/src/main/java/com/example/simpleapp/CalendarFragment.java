package com.example.simpleapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private TextView selectedDateText;
    private EditText eventTitleInput;
    private MaterialButton addEventButton;
    private RecyclerView eventsList;
    private EventsAdapter adapter;
    private List<CalendarEvent> events;
    private String currentDate;
    private SharedPreferences prefs;
    private Gson gson;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Инициализация View
        selectedDateText = view.findViewById(R.id.selectedDateText);
        eventTitleInput = view.findViewById(R.id.eventTitleInput);
        addEventButton = view.findViewById(R.id.addEventButton);
        eventsList = view.findViewById(R.id.eventsList);

        // Инициализация
        prefs = requireActivity().getSharedPreferences("CalendarPrefs", 0);
        gson = new Gson();
        events = loadEvents();
        currentDate = getCurrentDate();

        // Настройка RecyclerView
        adapter = new EventsAdapter(events, currentDate);
        eventsList.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsList.setAdapter(adapter);

        // Обработчик выбора даты
        selectedDateText.setOnClickListener(v -> showDatePicker());

        // Обновляем отображение даты
        updateSelectedDateDisplay();

        // Обработчик добавления события
        addEventButton.setOnClickListener(v -> {
            String title = eventTitleInput.getText().toString().trim();
            if (!title.isEmpty()) {
                events.add(new CalendarEvent(currentDate, title));
                saveEvents();
                adapter.updateEvents(events, currentDate);
                eventTitleInput.setText("");
                Toast.makeText(getContext(), "Событие добавлено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Введите название события", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Выберите дату")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            currentDate = sdf.format(calendar.getTime());
            updateSelectedDateDisplay();
            adapter.updateEvents(events, currentDate);
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void updateSelectedDateDisplay() {
        selectedDateText.setText("📅 " + currentDate);
    }

    private List<CalendarEvent> loadEvents() {
        String json = prefs.getString("events", "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<CalendarEvent>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private void saveEvents() {
        String json = gson.toJson(events);
        prefs.edit().putString("events", json).apply();
    }
}