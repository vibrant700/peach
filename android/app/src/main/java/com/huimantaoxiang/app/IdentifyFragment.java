package com.huimantaoxiang.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class IdentifyFragment extends Fragment {

    private LinearLayout btnStartIdentify;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_identify, container, false);

        btnStartIdentify = view.findViewById(R.id.btn_start_identify);
        btnStartIdentify.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), IdentifyPeachActivity.class));
        });

        return view;
    }
}
