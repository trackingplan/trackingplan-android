package com.trackingplan.examples.urlconnection.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.trackingplan.examples.urlconnection.databinding.FragmentBottomSheetBinding;
import com.trackingplan.examples.urlconnection.databinding.FragmentHomeBinding;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private FragmentBottomSheetBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
