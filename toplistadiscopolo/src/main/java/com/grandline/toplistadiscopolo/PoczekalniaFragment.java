package com.grandline.toplistadiscopolo;

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

public class PoczekalniaFragment extends Fragment {
    private ListView listPocz;
    private LazyAdapter adapterPocz;
    private ArrayList<HashMap<String, String>> songsListPocz;

    public PoczekalniaFragment() {
        // Required empty public constructor
    }

    public static PoczekalniaFragment newInstance() {
        return new PoczekalniaFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        songsListPocz = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poczekalnia, container, false);
        
        listPocz = view.findViewById(R.id.listPocz);
        adapterPocz = new LazyAdapter(getActivity(), songsListPocz);
        listPocz.setAdapter(adapterPocz);
        
        // Set click listener
        listPocz.setOnItemClickListener((parent, view1, position, id) -> {
            if (getActivity() instanceof ListaPrzebojowDiscoPolo) {
                ((ListaPrzebojowDiscoPolo) getActivity()).showSongMenu(position, Constants.KEY_POCZEKALNIA);
            }
        });
        
        return view;
    }

    public void updateAdapter(ArrayList<HashMap<String, String>> newSongsListPocz) {
        if (songsListPocz != null && adapterPocz != null) {
            songsListPocz.clear();
            songsListPocz.addAll(newSongsListPocz);
            adapterPocz.notifyDataSetChanged();
        }
    }

    public ArrayList<HashMap<String, String>> getSongsListPocz() {
        return songsListPocz;
    }

    public LazyAdapter getAdapterPocz() {
        return adapterPocz;
    }
}