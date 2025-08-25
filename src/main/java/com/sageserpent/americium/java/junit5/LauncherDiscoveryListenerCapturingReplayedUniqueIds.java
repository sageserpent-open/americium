package com.sageserpent.americium.java.junit5;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class LauncherDiscoveryListenerCapturingReplayedUniqueIds
        implements LauncherDiscoveryListener {
    private final static Set<UniqueId> replayedTestCaseIds = new HashSet<>();

    public static Set<UniqueId> replayedUniqueIds() {
        return Collections.unmodifiableSet(replayedTestCaseIds);
    }

    @Override
    public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
        replayedTestCaseIds.addAll(request
                                           .getSelectorsByType(UniqueIdSelector.class)
                                           .stream()
                                           .map(UniqueIdSelector::getUniqueId)
                                           .collect(Collectors.toList()));

        LauncherDiscoveryListener.super.launcherDiscoveryStarted(request);
    }
}
