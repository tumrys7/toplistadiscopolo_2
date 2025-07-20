package com.grandline.toplistadiscopolo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.grandline.toplistadiscopolo.Constants;
import com.grandline.toplistadiscopolo.ListaPrzebojowDiscoPolo;
import com.grandline.toplistadiscopolo.R;
import com.grandline.toplistadiscopolo.adapters.LazyAdapter;

public class NotowaniaFragment extends Fragment {
    private ListView list2012;
    private LazyAdapter adapter2012;
    private Spinner spinnerNotowaniaPrzedzialy;
    private ArrayAdapter<String> adapterNotowPrzedzialy;
    private ListaPrzebojowDiscoPolo parentActivity;
    private boolean isSpinnerClicked = false;

    public static NotowaniaFragment newInstance() {
        return new NotowaniaFragment();
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
        View view = inflater.inflate(R.layout.fragment_notowania, container, false);
        
        list2012 = view.findViewById(R.id.list2012);
        spinnerNotowaniaPrzedzialy = view.findViewById(R.id.spinnerNotowaniaPrzedzialy);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (parentActivity != null && getActivity() != null) {
            adapter2012 = new LazyAdapter(getActivity(), parentActivity.notowaniaList);
            list2012.setAdapter(adapter2012);
            
            adapterNotowPrzedzialy = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, parentActivity.listNotowPrzedzialy);
            adapterNotowPrzedzialy.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerNotowaniaPrzedzialy.setAdapter(adapterNotowPrzedzialy);
            
            list2012.setOnItemClickListener((parent, clickedView, position, id) -> 
                parentActivity.showSongMenu(position, Constants.KEY_LISTA_2012));

            spinnerNotowaniaPrzedzialy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    if (isSpinnerClicked && parentActivity != null) {
                        String notowanieId = parentActivity.notowPrzedzialyList.get(pos).get(Constants.KEY_NOTOWANIE_ZA);
                        parentActivity.handleSpinnerSelection(notowanieId, pos);
                    }
                    isSpinnerClicked = true;
                }

                public void onNothingSelected(AdapterView<?> arg0) {
                    // TODO Auto-generated method stub
                }
            });
        }
    }

    public void updateAdapter() {
        if (adapter2012 != null && isAdded() && !isRemoving() && getView() != null) {
            adapter2012.notifyDataSetChanged();
        }
    }

    public void updateSpinnerAdapter() {
        if (adapterNotowPrzedzialy != null && isAdded() && !isRemoving() && getView() != null) {
            adapterNotowPrzedzialy.notifyDataSetChanged();
        }
    }

    public void setSpinnerSelection(int position) {
        if (spinnerNotowaniaPrzedzialy != null) {
            spinnerNotowaniaPrzedzialy.setSelection(position);
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
        if (list2012 != null) {
            list2012.clearAnimation();
            list2012.clearFocus();
            list2012.setOnItemClickListener(null);
            list2012.setOnItemLongClickListener(null);
            list2012.setOnScrollListener(null);
            list2012.setAdapter(null);
        }
        
        if (spinnerNotowaniaPrzedzialy != null) {
            spinnerNotowaniaPrzedzialy.setOnItemSelectedListener(null);
            spinnerNotowaniaPrzedzialy.setAdapter(null);
        }
    }

    private void cleanupReferences() {
        adapter2012 = null;
        adapterNotowPrzedzialy = null;
        list2012 = null;
        spinnerNotowaniaPrzedzialy = null;
        parentActivity = null;
    }
}