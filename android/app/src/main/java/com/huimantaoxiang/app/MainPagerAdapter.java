package com.huimantaoxiang.app;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new IdentifyFragment();
            case 1:
                return new TaobaoFragment();
            case 2:
                return new CommunityFragment();
            case 3:
                return new WorkshopFragment();
            case 4:
                return new IrrigationFragment();
            case 5:
                return new PriceFragment();
            default:
                return new IdentifyFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 6; // 6个功能模块
    }
}
