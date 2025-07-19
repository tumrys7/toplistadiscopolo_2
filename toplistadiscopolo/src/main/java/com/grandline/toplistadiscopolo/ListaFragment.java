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

public class ListaFragment extends Fragment {
    private ListView list;
    private LazyAdapter adapter;
    private ArrayList<HashMap<String, String>> songsList;

    public ListaFragment() {
        // Required empty public constructor
    }

    public static ListaFragment newInstance() {
        return new ListaFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        songsList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista, container, false);
        
        list = view.findViewById(R.id.list);
        adapter = new LazyAdapter(getActivity(), songsList);
        list.setAdapter(adapter);
        
        // Set click listener
        list.setOnItemClickListener((parent, view1, position, id) -> {
            if (getActivity() instanceof ListaPrzebojowDiscoPolo) {
                ((ListaPrzebojowDiscoPolo) getActivity()).showSongMenu(position, Constants.KEY_LISTA);
            }
        });
        
        return view;
    }

    public void updateAdapter(ArrayList<HashMap<String, String>> newSongsList) {
        if (songsList != null && adapter != null) {
            songsList.clear();
            songsList.addAll(newSongsList);
            adapter.notifyDataSetChanged();
        }
    }

    public ArrayList<HashMap<String, String>> getSongsList() {
        return songsList;
    }

    public LazyAdapter getAdapter() {
        return adapter;
    }
}