package com.huimantaoxiang.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WorkshopFragment extends Fragment {

    private LinearLayout btnStartWorkshop;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workshop, container, false);

        btnStartWorkshop = view.findViewById(R.id.btn_start_workshop);
        btnStartWorkshop.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), PeachWorkshopActivity.class));
        });

        return view;
    }
}
