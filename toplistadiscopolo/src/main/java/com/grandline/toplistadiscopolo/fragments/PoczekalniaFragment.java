package com.grandline.toplistadiscopolo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ListaPrzebojowDiscoPolo;
import com.grandline.toplistadiscopolo.R;
import com.grandline.toplistadiscopolo.adapters.LazyAdapter;

public class PoczekalniaFragment extends Fragment {
    
    private ListView listPocz;
    private LazyAdapter adapterPocz;
    private ListaPrzebojowDiscoPolo parentActivity;
    
    public static PoczekalniaFragment newInstance() {
        return new PoczekalniaFragment();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            parentActivity = (ListaPrzebojowDiscoPolo) getActivity();
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poczekalnia, container, false);
        
        listPocz = view.findViewById(R.id.listPocz);
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (parentActivity != null && getActivity() != null) {
            adapterPocz = new LazyAdapter(getActivity(), parentActivity.songsListPocz);
            listPocz.setAdapter(adapterPocz);
            
            listPocz.setOnItemClickListener((parent, clickedView, position, id) -> 
                parentActivity.showSongMenu(position, Constants.KEY_POCZEKALNIA));
        }
    }
    
    public void updateAdapter() {
        if (adapterPocz != null && isAdded() && !isRemoving() && getView() != null) {
            adapterPocz.notifyDataSetChanged();
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
        if (listPocz != null) {
            listPocz.clearAnimation();
            listPocz.clearFocus();
            listPocz.setOnItemClickListener(null);
            listPocz.setOnItemLongClickListener(null);
            listPocz.setOnScrollListener(null);
            listPocz.setAdapter(null);
        }
    }
    
    private void cleanupReferences() {
        adapterPocz = null;
        listPocz = null;
        parentActivity = null;
    }
}