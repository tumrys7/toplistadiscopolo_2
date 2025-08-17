package com.grandline.toplistadiscopolo.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ListaPrzebojowDiscoPolo;
import com.grandline.toplistadiscopolo.R;
import com.grandline.toplistadiscopolo.adapters.WykAdapter;

import java.util.HashMap;

public class WykonawcyFragment extends Fragment {

    private ListView listWykon;
    private WykAdapter adapterWyk;
    private SearchView searchView;
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
        View view = inflater.inflate(R.layout.fragment_wykonawcy, container, false);

        listWykon = view.findViewById(R.id.listWykon);
        searchView = view.findViewById(R.id.search_view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (parentActivity != null && getActivity() != null) {
            adapterWyk = new WykAdapter(getActivity(), parentActivity.filteredWykonList);
            listWykon.setAdapter(adapterWyk);

            listWykon.setOnItemClickListener((parent, clickedView, position, id) -> {
                @SuppressWarnings("unchecked")
                HashMap<String, String> mapo = (HashMap<String, String>) listWykon.getItemAtPosition(position);
                final String id_wykonawcy = mapo.get(Constants.KEY_ID_WYKON);
                parentActivity.showAuthSongs(id_wykonawcy);
            });
            
            // Configure SearchView
            setupSearchView();
            
            // Notify parent activity that this fragment is ready for updates
            Log.i("WykonawcyFragment", "Fragment is ready, notifying parent activity");
            parentActivity.onFragmentReady("WykonawcyFragment");
        }
    }
    
    private void setupSearchView() {
        if (searchView == null) return;
        
        // Make SearchView clickable and focusable
        searchView.setIconified(false);
        searchView.setIconifiedByDefault(false);
        searchView.setFocusable(true);
        searchView.setClickable(true);
        
        // Get the EditText inside SearchView
        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchEditText != null) {
            // Set hint text from resources
            searchEditText.setHint(getString(R.string.tab_wykonawcy_filtruj));
            searchEditText.setHintTextColor(Color.parseColor("#546E7A"));
            searchEditText.setTextColor(getResources().getColor(R.color.md_theme_onPrimary));
        }
        
        // Clear focus initially to prevent keyboard from showing immediately
        searchView.clearFocus();
        
        // Set up click listener to show keyboard when SearchView is clicked
        searchView.setOnClickListener(v -> {
            searchView.setIconified(false);
            searchView.requestFocusFromTouch();
            if (searchEditText != null) {
                searchEditText.requestFocus();
            }
        });
        
        // Set up query text listener for live search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Hide keyboard on submit
                searchView.clearFocus();
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter the list based on search text
                if (parentActivity != null) {
                    parentActivity.filterWykonawcy(parentActivity.wykonList, newText);
                    // Update the adapter after filtering
                    updateAdapter();
                }
                return true;
            }
        });
        
        // Set up close listener to reset the list when search is cleared
        searchView.setOnCloseListener(() -> {
            if (parentActivity != null) {
                parentActivity.filterWykonawcy(parentActivity.wykonList, "");
                updateAdapter();
            }
            return false;
        });
    }

    public void updateAdapter() {
        if (isAdded() && !isRemoving() && getView() != null && listWykon != null) {
            try {
                // Log data size for debugging
                int dataSize = (parentActivity != null && parentActivity.filteredWykonList != null) ? 
                    parentActivity.filteredWykonList.size() : 0;
                Log.i("WykonawcyFragment", "Updating adapter with " + dataSize + " items");
                Log.i("WykonawcyFragment", "Fragment state - isAdded: " + isAdded() + ", adapter exists: " + (adapterWyk != null) + 
                    ", list exists: " + (listWykon != null) + ", parentActivity exists: " + (parentActivity != null));
                
                if (dataSize > 0) {
                    if (adapterWyk != null) {
                        Log.i("WykonawcyFragment", "Notifying existing adapter of data changes");
                        adapterWyk.safeNotifyDataSetChanged();
                    } else {
                        // Recreate adapter if it's null but we have data
                        Log.i("WykonawcyFragment", "Recreating adapter with " + dataSize + " items");
                        adapterWyk = new WykAdapter(getActivity(), parentActivity.filteredWykonList);
                        listWykon.setAdapter(adapterWyk);
                    }
                } else {
                    Log.w("WykonawcyFragment", "No data available to display (dataSize: " + dataSize + ")");
                    // Still try to notify adapter in case data was cleared
                    if (adapterWyk != null) {
                        adapterWyk.safeNotifyDataSetChanged();
                    }
                }
            } catch (Exception e) {
                Log.e("WykonawcyFragment", "Error updating adapter: " + e.getMessage(), e);
            }
        } else {
            Log.w("WykonawcyFragment", "Cannot update adapter - Fragment state - isAdded: " + isAdded() + 
                ", isRemoving: " + isRemoving() + ", hasView: " + (getView() != null) + 
                ", hasList: " + (listWykon != null));
        }
    }

    // Method to recreate adapter when data structure changes
    public void refreshAdapter() {
        if (parentActivity != null && parentActivity.filteredWykonList != null && listWykon != null &&
                isAdded() && !isRemoving() && getView() != null) {
            Log.i("WykonawcyFragment", "Force refreshing adapter");
            assert getActivity() != null;
            adapterWyk = new WykAdapter(getActivity(), parentActivity.filteredWykonList);
            listWykon.setAdapter(adapterWyk);
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

        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnClickListener(null);
            searchView.setOnCloseListener(null);
            searchView.clearFocus();
        }
    }

    private void cleanupReferences() {
        adapterWyk = null;
        listWykon = null;
        searchView = null;
        parentActivity = null;
    }
}