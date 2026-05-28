package com.example.simpleapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new CalendarFragment();  // Календарь
            case 1:
                return new GalleryFragment();   //Галерея (ручное добавление)
            default:
                return new CalendarFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // число вкладок
    }
}