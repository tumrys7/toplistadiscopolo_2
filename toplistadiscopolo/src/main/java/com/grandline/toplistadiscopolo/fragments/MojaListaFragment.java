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
import com.grandline.toplistadiscopolo.adapters.MojaAdapter;

public class MojaListaFragment extends Fragment {
    private ListView listMojalista;
    private MojaAdapter adapterMojalista;
    private ArrayList<HashMap<String, String>> songsListMojalista;

    public MojaListaFragment() {
        // Required empty public constructor
    }

    public static MojaListaFragment newInstance() {
        return new MojaListaFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        songsListMojalista = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moja_lista, container, false);
        
        listMojalista = view.findViewById(R.id.listMojalista);
        adapterMojalista = new MojaAdapter(getActivity(), songsListMojalista);
        listMojalista.setAdapter(adapterMojalista);
        
        // Set click listener
        listMojalista.setOnItemClickListener((parent, view1, position, id) -> {
            if (getActivity() instanceof ListaPrzebojowDiscoPolo) {
                ((ListaPrzebojowDiscoPolo) getActivity()).showSongMenu(position, Constants.KEY_MOJALISTA);
            }
        });
        
        return view;
    }

    public void updateAdapter(ArrayList<HashMap<String, String>> newSongsListMojalista) {
        if (songsListMojalista != null && adapterMojalista != null) {
            songsListMojalista.clear();
            songsListMojalista.addAll(newSongsListMojalista);
            adapterMojalista.notifyDataSetChanged();
        }
    }

    public ArrayList<HashMap<String, String>> getSongsListMojalista() {
        return songsListMojalista;
    }

    public MojaAdapter getAdapterMojalista() {
        return adapterMojalista;
    }
}