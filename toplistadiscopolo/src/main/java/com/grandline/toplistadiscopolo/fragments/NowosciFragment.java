package com.grandline.toplistadiscopolo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ListaPrzebojowDiscoPolo;
import com.grandline.toplistadiscopolo.R;
import com.grandline.toplistadiscopolo.adapters.NowosciAdapter;

public class NowosciFragment extends Fragment {
    private ListView listNowosci;
    private NowosciAdapter adapterNowosci;
    private ArrayList<HashMap<String, String>> songsListNowosci;

    public NowosciFragment() {
        // Required empty public constructor
    }

    public static NowosciFragment newInstance() {
        return new NowosciFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        songsListNowosci = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nowosci, container, false);
        
        listNowosci = view.findViewById(R.id.listNowosci);
        adapterNowosci = new NowosciAdapter(getActivity(), songsListNowosci);
        listNowosci.setAdapter(adapterNowosci);
        
        // Set click listener
        listNowosci.setOnItemClickListener((parent, view1, position, id) -> {
            if (getActivity() instanceof ListaPrzebojowDiscoPolo) {
                ((ListaPrzebojowDiscoPolo) getActivity()).showSongMenu(position, Constants.KEY_NOWOSCI);
            }
        });
        
        return view;
    }

    public void updateAdapter(ArrayList<HashMap<String, String>> newSongsListNowosci) {
        if (songsListNowosci != null && adapterNowosci != null) {
            songsListNowosci.clear();
            songsListNowosci.addAll(newSongsListNowosci);
            adapterNowosci.notifyDataSetChanged();
        }
    }

    public ArrayList<HashMap<String, String>> getSongsListNowosci() {
        return songsListNowosci;
    }

    public NowosciAdapter getAdapterNowosci() {
        return adapterNowosci;
    }
}