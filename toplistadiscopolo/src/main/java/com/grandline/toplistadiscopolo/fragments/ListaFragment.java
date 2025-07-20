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

public class ListaFragment extends Fragment {
    
    private ListView list;
    private LazyAdapter adapter;
    private ListaPrzebojowDiscoPolo parentActivity;
    
    public static ListaFragment newInstance() {
        return new ListaFragment();
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
        View view = inflater.inflate(R.layout.fragment_lista, container, false);
        
        list = view.findViewById(R.id.list);
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (parentActivity != null && getActivity() != null) {
            adapter = new LazyAdapter(getActivity(), parentActivity.songsList);
            list.setAdapter(adapter);
            
            list.setOnItemClickListener((parent, clickedView, position, id) -> 
                parentActivity.showSongMenu(position, Constants.KEY_LISTA));
        }
    }
    
    public void updateAdapter() {
        if (adapter != null && isAdded() && !isRemoving() && getView() != null) {
            adapter.notifyDataSetChanged();
        }
    }
    
    public void refreshWithAdReward() {
        if (parentActivity != null) {
            parentActivity.refreshListaWithAdReward();
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
        if (list != null) {
            list.clearAnimation();
            list.clearFocus();
            list.setOnItemClickListener(null);
            list.setOnItemLongClickListener(null);
            list.setOnScrollListener(null);
            list.setAdapter(null);
        }
    }
    
    private void cleanupReferences() {
        adapter = null;
        list = null;
        parentActivity = null;
    }
}