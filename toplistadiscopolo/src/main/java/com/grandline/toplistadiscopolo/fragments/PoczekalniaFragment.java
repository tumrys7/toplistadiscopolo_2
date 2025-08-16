package com.grandline.toplistadiscopolo.fragments;

import android.os.Bundle;
import android.util.Log;
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

            listPocz.setOnItemClickListener((parent, clickedView, position, id) -> {
                // Validate position before calling showSongMenu
                if (parentActivity != null && parentActivity.songsListPocz != null && 
                    position >= 0 && position < parentActivity.songsListPocz.size()) {
                    parentActivity.showSongMenu(position, Constants.KEY_POCZEKALNIA);
                } else {
                    Log.e("PoczekalniaFragment", "Invalid click position " + position + 
                          " for list size " + (parentActivity != null && parentActivity.songsListPocz != null ? 
                          parentActivity.songsListPocz.size() : 0));
                }
            });
                    
            // Notify parent activity that this fragment is ready for updates
            Log.i("PoczekalniaFragment", "Fragment is ready, notifying parent activity");
            parentActivity.onFragmentReady("PoczekalniaFragment");
        }
    }

    public void updateAdapter() {
        if (isAdded() && !isRemoving() && getView() != null && listPocz != null) {
            try {
                // Log data size for debugging
                int dataSize = (parentActivity != null && parentActivity.songsListPocz != null) ? 
                    parentActivity.songsListPocz.size() : 0;
                Log.i("PoczekalniaFragment", "Updating adapter with " + dataSize + " items");
                Log.i("PoczekalniaFragment", "Fragment state - isAdded: " + isAdded() + ", adapter exists: " + (adapterPocz != null) + 
                    ", list exists: " + (listPocz != null) + ", parentActivity exists: " + (parentActivity != null));
                
                if (dataSize > 0) {
                    if (adapterPocz != null) {
                        Log.i("PoczekalniaFragment", "Notifying existing adapter of data changes");
                        adapterPocz.safeNotifyDataSetChanged();
                    } else {
                        // Recreate adapter if it's null but we have data
                        Log.i("PoczekalniaFragment", "Recreating adapter with " + dataSize + " items");
                        adapterPocz = new LazyAdapter(getActivity(), parentActivity.songsListPocz);
                        listPocz.setAdapter(adapterPocz);
                    }
                } else {
                    Log.w("PoczekalniaFragment", "No data available to display (dataSize: " + dataSize + ")");
                    // Still try to notify adapter in case data was cleared
                    if (adapterPocz != null) {
                        adapterPocz.safeNotifyDataSetChanged();
                    }
                }
            } catch (Exception e) {
                Log.e("PoczekalniaFragment", "Error updating adapter: " + e.getMessage(), e);
            }
        } else {
            Log.w("PoczekalniaFragment", "Cannot update adapter - Fragment state - isAdded: " + isAdded() + 
                ", isRemoving: " + isRemoving() + ", hasView: " + (getView() != null) + 
                ", hasList: " + (listPocz != null));
        }
    }

    // Method to recreate adapter when data structure changes
    public void refreshAdapter() {
        if (parentActivity != null && parentActivity.songsListPocz != null && listPocz != null &&
                isAdded() && !isRemoving() && getView() != null) {
            Log.i("PoczekalniaFragment", "Force refreshing adapter");
            assert getActivity() != null;
            adapterPocz = new LazyAdapter(getActivity(), parentActivity.songsListPocz);
            listPocz.setAdapter(adapterPocz);
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