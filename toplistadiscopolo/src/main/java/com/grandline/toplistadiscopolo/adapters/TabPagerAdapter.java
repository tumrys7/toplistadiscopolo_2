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
    
    private static final int TAB_COUNT = 6;

    public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_LISTA:
            default:
                return ListaFragment.newInstance();
            case TAB_POCZEKALNIA:
                return PoczekalniaFragment.newInstance();
            case TAB_NOWOSCI:
                return NowosciFragment.newInstance();
            case TAB_MOJALISTA:
                return MojaListaFragment.newInstance();
            case TAB_WYKONAWCY:
                return WykonawcyFragment.newInstance();
            case TAB_NOTOWANIA:
                return NotowaniaFragment.newInstance();
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}