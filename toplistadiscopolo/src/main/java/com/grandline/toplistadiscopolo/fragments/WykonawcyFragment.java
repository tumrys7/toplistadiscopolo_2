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

import java.util.HashMap;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ListaPrzebojowDiscoPolo;
import com.grandline.toplistadiscopolo.R;
import com.grandline.toplistadiscopolo.adapters.WykAdapter;

public class WykonawcyFragment extends Fragment {
    
    private ListView listWykon;
    private WykAdapter adapterWyk;
    private EditText inputSearch;
    private ImageButton clearButton;
    private ListaPrzebojowDiscoPolo parentActivity;
    
    public static WykonawcyFragment newInstance() {
        return new WykonawcyFragment();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = (ListaPrzebojowDiscoPolo) getActivity();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wykonawcy, container, false);
        
        listWykon = view.findViewById(R.id.listWykon);
        inputSearch = view.findViewById(R.id.inputSearch);
        clearButton = view.findViewById(R.id.clearButton);
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (parentActivity != null) {
            adapterWyk = new WykAdapter(getActivity(), parentActivity.filteredWykonList);
            listWykon.setAdapter(adapterWyk);
            
            listWykon.setOnItemClickListener((parent, clickedView, position, id) -> {
                HashMap<String, String> mapo = (HashMap<String, String>) listWykon.getItemAtPosition(position);
                final String id_wykonawcy = mapo.get(Constants.KEY_ID_WYKON);
                parentActivity.showAuthSongs(id_wykonawcy);
            });

            inputSearch.addTextChangedListener(new TextWatcher() {
                public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                    if (parentActivity != null) {
                        parentActivity.filterWykonawcy(parentActivity.wykonList, cs);
                    }
                }
                
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub
                }
     
                public void afterTextChanged(Editable arg0) {
                    if (adapterWyk != null) {
                        adapterWyk.notifyDataSetChanged();
                    }
                }
            });
            
            clearButton.setOnClickListener(arg0 -> inputSearch.setText(""));
        }
    }
    
    public void updateAdapter() {
        if (adapterWyk != null && isAdded() && !isRemoving() && getView() != null) {
            adapterWyk.notifyDataSetChanged();
        }
    }
    
    @Override
    public void onDestroyView() {
        cleanupViews();
        super.onDestroyView();
    }
    
    @Override
    public void onDestroy() {
        cleanupReferences();
        super.onDestroy();
    }
    
    private void cleanupViews() {
        if (listWykon != null) {
            listWykon.clearAnimation();
            listWykon.clearFocus();
            listWykon.setOnItemClickListener(null);
            listWykon.setOnItemLongClickListener(null);
            listWykon.setOnScrollListener(null);
            listWykon.setAdapter(null);
        }
        
        if (inputSearch != null) {
            inputSearch.addTextChangedListener(null);
            inputSearch.setOnFocusChangeListener(null);
        }
        
        if (clearButton != null) {
            clearButton.setOnClickListener(null);
        }
    }
    
    private void cleanupReferences() {
        adapterWyk = null;
        listWykon = null;
        inputSearch = null;
        clearButton = null;
        parentActivity = null;
    }
}