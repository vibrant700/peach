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

public class TaobaoFragment extends Fragment {

    private LinearLayout btnStartTaobao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_taobao, container, false);

        btnStartTaobao = view.findViewById(R.id.btn_start_taobao);
        btnStartTaobao.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), TaobaoAIActivity.class));
        });

        return view;
    }
}
