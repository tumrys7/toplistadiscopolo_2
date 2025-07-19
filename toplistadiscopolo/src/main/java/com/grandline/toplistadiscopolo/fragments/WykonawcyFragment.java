package com.grandline.toplistadiscopolo.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ListaPrzebojowDiscoPolo;
import com.grandline.toplistadiscopolo.R;
import com.grandline.toplistadiscopolo.UtworyWykonawcy;
import com.grandline.toplistadiscopolo.adapters.WykAdapter;

public class WykonawcyFragment extends Fragment {
    private ListView listWykon;
    private WykAdapter adapterWyk;
    private ArrayList<HashMap<String, String>> wykonList;
    private ArrayList<HashMap<String, String>> filteredWykonList;
    private EditText inputSearch;
    private ImageButton clearButton;

    public WykonawcyFragment() {
        // Required empty public constructor
    }

    public static WykonawcyFragment newInstance() {
        return new WykonawcyFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wykonList = new ArrayList<>();
        filteredWykonList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wykonawcy, container, false);
        
        listWykon = view.findViewById(R.id.listWykon);
        inputSearch = view.findViewById(R.id.inputSearch);
        clearButton = view.findViewById(R.id.clearButton);
        
        adapterWyk = new WykAdapter(getActivity(), filteredWykonList);
        listWykon.setAdapter(adapterWyk);
        
        // Set click listener
        listWykon.setOnItemClickListener((parent, view1, position, id) -> {
            HashMap<String, String> mapo = (HashMap<String, String>) listWykon.getItemAtPosition(position);
            final String id_wykonawcy = mapo.get(Constants.KEY_ID_WYKON);
            if (getActivity() instanceof ListaPrzebojowDiscoPolo) {
                ((ListaPrzebojowDiscoPolo) getActivity()).showAuthSongs(id_wykonawcy);
            }
        });

        // Search functionality
        inputSearch.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                filterWykonawcy(wykonList, cs);
            }
            
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub
            }
 
            public void afterTextChanged(Editable arg0) {
                adapterWyk.notifyDataSetChanged();
            }
        });
        
        clearButton.setOnClickListener(arg0 -> inputSearch.setText(""));
        
        return view;
    }

    private void filterWykonawcy(ArrayList<HashMap<String, String>> wykonList, CharSequence searchText) {
        filteredWykonList.clear();
        if (searchText.length() == 0) {
            filteredWykonList.addAll(wykonList);
        } else {
            String searchString = searchText.toString().toLowerCase();
            for (HashMap<String, String> item : wykonList) {
                String wykonawca = item.get(Constants.KEY_WYKONAWCA);
                if (wykonawca != null && wykonawca.toLowerCase().contains(searchString)) {
                    filteredWykonList.add(item);
                }
            }
        }
    }

    public void updateAdapter(ArrayList<HashMap<String, String>> newWykonList) {
        if (wykonList != null && adapterWyk != null) {
            wykonList.clear();
            wykonList.addAll(newWykonList);
            filteredWykonList.clear();
            filteredWykonList.addAll(newWykonList);
            adapterWyk.notifyDataSetChanged();
        }
    }

    public ArrayList<HashMap<String, String>> getWykonList() {
        return wykonList;
    }

    public ArrayList<HashMap<String, String>> getFilteredWykonList() {
        return filteredWykonList;
    }

    public WykAdapter getAdapterWyk() {
        return adapterWyk;
    }
}