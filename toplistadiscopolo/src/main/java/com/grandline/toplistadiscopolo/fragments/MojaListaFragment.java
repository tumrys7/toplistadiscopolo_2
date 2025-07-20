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
import com.grandline.toplistadiscopolo.adapters.MojaAdapter;

public class MojaListaFragment extends Fragment {
    
    private ListView listMojalista;
    private MojaAdapter adapterMojalista;
    private ListaPrzebojowDiscoPolo parentActivity;
    
    public static MojaListaFragment newInstance() {
        return new MojaListaFragment();
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
        View view = inflater.inflate(R.layout.fragment_moja_lista, container, false);
        
        listMojalista = view.findViewById(R.id.listMojalista);
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (parentActivity != null && getActivity() != null) {
            adapterMojalista = new MojaAdapter(getActivity(), parentActivity.songsListMojalista);
            listMojalista.setAdapter(adapterMojalista);
            
            listMojalista.setOnItemClickListener((parent, clickedView, position, id) -> 
                parentActivity.showSongMenu(position, Constants.KEY_MOJALISTA));
        }
    }
    
    public void updateAdapter() {
        if (adapterMojalista != null && isAdded() && !isRemoving() && getView() != null) {
            adapterMojalista.safeNotifyDataSetChanged();
        }
    }
    
    public void refreshWithAdReward() {
        if (parentActivity != null) {
            parentActivity.refreshMojaListaWithAdReward();
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
        if (listMojalista != null) {
            listMojalista.clearAnimation();
            listMojalista.clearFocus();
            listMojalista.setOnItemClickListener(null);
            listMojalista.setOnItemLongClickListener(null);
            listMojalista.setOnScrollListener(null);
            listMojalista.setAdapter(null);
        }
    }
    
    private void cleanupReferences() {
        adapterMojalista = null;
        listMojalista = null;
        parentActivity = null;
    }
}