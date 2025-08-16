package com.grandline.toplistadiscopolo.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.grandline.toplistadiscopolo.fragments.ListaFragment;
import com.grandline.toplistadiscopolo.fragments.PoczekalniaFragment;
import com.grandline.toplistadiscopolo.fragments.NowosciFragment;
import com.grandline.toplistadiscopolo.fragments.MojaListaFragment;
import com.grandline.toplistadiscopolo.fragments.WykonawcyFragment;
import com.grandline.toplistadiscopolo.fragments.NotowaniaFragment;

public class TabPagerAdapter extends FragmentStateAdapter {
    
    // Tab position constants
    public static final int TAB_LISTA = 0;
    public static final int TAB_POCZEKALNIA = 1;
    public static final int TAB_NOWOSCI = 2;
    public static final int TAB_MOJALISTA = 3;
    public static final int TAB_WYKONAWCY = 4;
    public static final int TAB_NOTOWANIA = 5;
    
    public static final int NUM_TABS = 6;

    public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_POCZEKALNIA:
                return new PoczekalniaFragment();
            case TAB_NOWOSCI:
                return new NowosciFragment();
            case TAB_MOJALISTA:
                return new MojaListaFragment();
            case TAB_WYKONAWCY:
                return new WykonawcyFragment();
            case TAB_NOTOWANIA:
                return new NotowaniaFragment();
            default:
                return new ListaFragment();
        }
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
    
    // Override getItemId to ensure proper fragment identification for ViewPager2
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    // Override containsItem to work with getItemId for proper fragment management
    @Override
    public boolean containsItem(long itemId) {
        return itemId >= 0 && itemId < NUM_TABS;
    }
}