package com.vocsy.fakecall.newFakeCall.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vocsy.fakecall.R;
import com.vocsy.fakecall.newFakeCall.CallHistoryHelper;
import com.vocsy.fakecall.newFakeCall.HistoryAdapter;
import com.vocsy.fakecall.newFakeCall.HistoryModels;
import com.vocsy.fakecall.newFakeCall.MainActivity;

import java.util.ArrayList;
import java.util.List;

import vocsy.ads.AdsHandler;
import vocsy.ads.GoogleNativeAdAdapter;



public class HistoryFragment extends Fragment {

    public static List<HistoryModels> historyModels = new ArrayList<>();
    public static View view;
    RecyclerView historyRecyclerView;
    CallHistoryHelper helper;

    public static void historyTextMethod() {
        TextView historyText = view.findViewById(R.id.historyText);
        if (historyModels.isEmpty()) {
            historyText.setVisibility(View.VISIBLE);
        } else {
            historyText.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_history, container, false);
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity.historyOrNot = true;

        historyRecyclerView = view.findViewById(R.id.historyRecyclerView);

        helper = new CallHistoryHelper(getContext());

        // historyTextMethod();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, true);
        historyRecyclerView.setLayoutManager(layoutManager);
        layoutManager.setStackFromEnd(true);

        historyModels = helper.retriveData();
        HistoryAdapter adapter = new HistoryAdapter(getContext(), historyModels);

        if (AdsHandler.isAdsOn()) {
            GoogleNativeAdAdapter adAdapter = GoogleNativeAdAdapter.Builder.with(getActivity(), getString(R.string.admob_native_id), adapter).adItemInterval(3).build();
            historyRecyclerView.setAdapter(adAdapter);
        }else {
            historyRecyclerView.setAdapter(adapter);
        }

        historyTextMethod();

    }

}