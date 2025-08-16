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

public class ListaFragment extends Fragment {

    private ListView list;
    private LazyAdapter adapter;
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

            list.setOnItemClickListener((parent, clickedView, position, id) -> {
                // Validate position before calling showSongMenu
                if (parentActivity != null && parentActivity.songsList != null && 
                    position >= 0 && position < parentActivity.songsList.size()) {
                    parentActivity.showSongMenu(position, Constants.KEY_LISTA);
                } else {
                    Log.e("ListaFragment", "Invalid click position " + position + 
                          " for list size " + (parentActivity != null && parentActivity.songsList != null ? 
                          parentActivity.songsList.size() : 0));
                }
            });
                    
            // Notify parent activity that this fragment is ready for updates
            Log.i("ListaFragment", "Fragment is ready, notifying parent activity");
            parentActivity.onFragmentReady("ListaFragment");
        }
    }

    public void updateAdapter() {
        if (isAdded() && !isRemoving() && getView() != null && list != null) {
            try {
                // Log data size for debugging
                int dataSize = (parentActivity != null && parentActivity.songsList != null) ? 
                    parentActivity.songsList.size() : 0;
                Log.i("ListaFragment", "Updating adapter with " + dataSize + " items");
                Log.i("ListaFragment", "Fragment state - isAdded: " + isAdded() + ", adapter exists: " + (adapter != null) + 
                    ", list exists: " + (list != null) + ", parentActivity exists: " + (parentActivity != null));
                
                if (dataSize > 0) {
                    if (adapter != null) {
                        Log.i("ListaFragment", "Notifying existing adapter of data changes");
                        adapter.safeNotifyDataSetChanged();
                    } else {
                        // Recreate adapter if it's null but we have data
                        Log.i("ListaFragment", "Recreating adapter with " + dataSize + " items");
                        adapter = new LazyAdapter(getActivity(), parentActivity.songsList);
                        list.setAdapter(adapter);
                    }
                } else {
                    Log.w("ListaFragment", "No data available to display (dataSize: " + dataSize + ")");
                    // Still try to notify adapter in case data was cleared
                    if (adapter != null) {
                        adapter.safeNotifyDataSetChanged();
                    }
                }
            } catch (Exception e) {
                Log.e("ListaFragment", "Error updating adapter: " + e.getMessage(), e);
            }
        } else {
            Log.w("ListaFragment", "Cannot update adapter - Fragment state - isAdded: " + isAdded() + 
                ", isRemoving: " + isRemoving() + ", hasView: " + (getView() != null) + 
                ", hasList: " + (list != null));
        }
    }
    // Method to recreate adapter when data structure changes
    public void refreshAdapter() {
        if (parentActivity != null && parentActivity.songsList != null && list != null &&
                isAdded() && !isRemoving() && getView() != null) {
            assert getActivity() != null;
            adapter = new LazyAdapter(getActivity(), parentActivity.songsList);
            list.setAdapter(adapter);
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