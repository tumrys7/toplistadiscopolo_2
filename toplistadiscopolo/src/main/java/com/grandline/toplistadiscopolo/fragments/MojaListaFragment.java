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
import com.grandline.toplistadiscopolo.adapters.MojaAdapter;
import java.util.ArrayList;

public class MojaListaFragment extends Fragment {

    private ListView listMojalista;
    private MojaAdapter adapterMojalista;
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

            listMojalista.setOnItemClickListener((parent, clickedView, position, id) -> {
                // Enhanced validation with adapter synchronization
                if (parentActivity != null && parentActivity.songsListMojalista != null && 
                    position >= 0 && position < parentActivity.songsListMojalista.size() &&
                    adapterMojalista != null && position < adapterMojalista.getCount()) {
                    
                    // Double-check the adapter has valid data at this position
                    Object item = adapterMojalista.getItem(position);
                    if (item != null) {
                        parentActivity.showSongMenu(position, Constants.KEY_MOJALISTA);
                    } else {
                        Log.w("MojaListaFragment", "Adapter item at position " + position + " is null, ignoring click");
                    }
                } else {
                    int listSize = (parentActivity != null && parentActivity.songsListMojalista != null) ? 
                                   parentActivity.songsListMojalista.size() : 0;
                    int adapterSize = (adapterMojalista != null) ? adapterMojalista.getCount() : 0;
                    Log.e("MojaListaFragment", "Invalid click position " + position + 
                          " for list size " + listSize + ", adapter size " + adapterSize);
                }
            });
                    
            // Notify parent activity that this fragment is ready for updates
            Log.i("MojaListaFragment", "Fragment is ready, notifying parent activity");
            parentActivity.onFragmentReady("MojaListaFragment");
        }
    }

    public void updateAdapter() {
        if (isAdded() && !isRemoving() && getView() != null && listMojalista != null) {
            try {
                // Log data size for debugging
                int dataSize = (parentActivity != null && parentActivity.songsListMojalista != null) ? 
                    parentActivity.songsListMojalista.size() : 0;
                Log.i("MojaListaFragment", "Updating adapter with " + dataSize + " items");
                
                if (dataSize > 0) {
                    if (adapterMojalista != null) {
                        adapterMojalista.safeNotifyDataSetChanged();
                    } else {
                        // Recreate adapter if it's null but we have data
                        Log.i("MojaListaFragment", "Recreating adapter with " + dataSize + " items");
                        adapterMojalista = new MojaAdapter(getActivity(), parentActivity.songsListMojalista);
                        listMojalista.setAdapter(adapterMojalista);
                    }
                } else {
                    // Handle empty data case - clear adapter or set empty adapter
                    if (adapterMojalista != null) {
                        Log.i("MojaListaFragment", "Data is empty, refreshing adapter to reflect empty state");
                        adapterMojalista.safeNotifyDataSetChanged();
                    } else if (parentActivity != null) {
                        // Create empty adapter to ensure ListView shows no items
                        Log.i("MojaListaFragment", "Creating empty adapter for empty data");
                        adapterMojalista = new MojaAdapter(getActivity(), new ArrayList<>());
                        listMojalista.setAdapter(adapterMojalista);
                    }
                }
            } catch (Exception e) {
                Log.e("MojaListaFragment", "Error updating adapter: " + e.getMessage());
            }
        }
    }

    // Method to recreate adapter when data structure changes
    public void refreshAdapter() {
        if (parentActivity != null && parentActivity.songsListMojalista != null && listMojalista != null &&
                isAdded() && !isRemoving() && getView() != null) {
            Log.i("MojaListaFragment", "Force refreshing adapter");
            assert getActivity() != null;
            adapterMojalista = new MojaAdapter(getActivity(), parentActivity.songsListMojalista);
            listMojalista.setAdapter(adapterMojalista);
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