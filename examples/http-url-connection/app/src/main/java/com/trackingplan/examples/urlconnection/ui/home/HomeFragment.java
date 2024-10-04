package com.trackingplan.examples.urlconnection.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.trackingplan.examples.urlconnection.R;
import com.trackingplan.examples.urlconnection.databinding.FragmentHomeBinding;
import com.trackingplan.examples.urlconnection.ui.BottomSheetFragment;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        final Button showBottomButton = binding.showBottomButton;
        showBottomButton.setOnClickListener(view -> {
            BottomSheetFragment bottomSheet = new BottomSheetFragment();
            bottomSheet.show(requireActivity().getSupportFragmentManager(), bottomSheet.getTag());
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}