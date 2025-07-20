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
import com.grandline.toplistadiscopolo.adapters.NowosciAdapter;

public class NowosciFragment extends Fragment {
    
    private ListView listNowosci;
    private NowosciAdapter adapterNowosci;
    private ListaPrzebojowDiscoPolo parentActivity;
    
    public static NowosciFragment newInstance() {
        return new NowosciFragment();
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
        View view = inflater.inflate(R.layout.fragment_nowosci, container, false);
        
        listNowosci = view.findViewById(R.id.listNowosci);
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (parentActivity != null && getActivity() != null) {
            adapterNowosci = new NowosciAdapter(getActivity(), parentActivity.songsListNowosci);
            listNowosci.setAdapter(adapterNowosci);
            
            listNowosci.setOnItemClickListener((parent, clickedView, position, id) -> 
                parentActivity.showSongMenu(position, Constants.KEY_NOWOSCI));
        }
    }
    
    public void updateAdapter() {
        if (adapterNowosci != null && isAdded() && !isRemoving() && getView() != null) {
            adapterNowosci.notifyDataSetChanged();
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
        if (listNowosci != null) {
            listNowosci.clearAnimation();
            listNowosci.clearFocus();
            listNowosci.setOnItemClickListener(null);
            listNowosci.setOnItemLongClickListener(null);
            listNowosci.setOnScrollListener(null);
            listNowosci.setAdapter(null);
        }
    }
    
    private void cleanupReferences() {
        adapterNowosci = null;
        listNowosci = null;
        parentActivity = null;
    }
}