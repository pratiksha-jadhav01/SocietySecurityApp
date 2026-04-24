package com.example.securiodb.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.securiodb.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class SocietyFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private String flatNo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_society, container, false);

        if (getArguments() != null) {
            flatNo = getArguments().getString("flatNo", "");
            Log.d("SocietyFragment", "Received flatNo: " + flatNo);
        }

        tabLayout = view.findViewById(R.id.tabLayoutSociety);
        viewPager = view.findViewById(R.id.viewPagerSociety);

        setupViewPager();
        return view;
    }

    private void setupViewPager() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                Bundle args = new Bundle();
                args.putString("flatNo", flatNo);
                if (position == 0) {
                    ComplaintFragment frag = new ComplaintFragment();
                    frag.setArguments(args);
                    return frag;
                } else {
                    NoticeBoardFragment frag = new NoticeBoardFragment();
                    frag.setArguments(args);
                    return frag;
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Complaints" : "Notice Board");
        }).attach();
        
        // Ensure it doesn't keep all fragments in memory if not needed, 
        // but 2 is fine. Default is 1.
        viewPager.setOffscreenPageLimit(1);
    }
}
