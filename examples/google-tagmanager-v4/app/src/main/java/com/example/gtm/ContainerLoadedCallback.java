package com.example.gtm;

import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;

import java.util.ArrayList;
import java.util.List;

public class ContainerLoadedCallback implements ContainerHolder.ContainerAvailableListener {
    private static List<Container> mRegisterContainers = new ArrayList();

    @Override // com.google.android.gms.tagmanager.ContainerHolder.ContainerAvailableListener
    public void onContainerAvailable(ContainerHolder containerHolder, String s) {
    }

    ContainerLoadedCallback() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void registerCallbacksForContainer(Container container) {
        if (container != null) {
            mRegisterContainers.add(container);
        }
    }
}
