/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.helpers;

import android.util.Log;

import com.android.os.nano.AtomsProto;
import com.android.os.nano.StatsLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * AppStartupHelper consist of helper methods to set the app
 * startup configs in statsd to track the app startup related
 * performance metrics and retrieve the necessary information from
 * statsd using the config id.
 */
public class AppStartupHelper implements ICollectorHelper<StringBuilder> {

    private static final String LOG_TAG = AppStartupHelper.class.getSimpleName();

    private static final String COLD_STARTUP = "cold_startup";
    private static final String WARM_STARTUP = "warm_startup";
    private static final String HOT_STARTUP = "hot_startup";
    private static final String COUNT = "count";
    private static final String TOTAL_COUNT = "total_count";

    private static final String STARTUP_FULLY_DRAWN_UNKNOWN = "startup_fully_drawn_unknown";
    private static final String STARTUP_FULLY_DRAWN_WITH_BUNDLE = "startup_fully_drawn_with_bundle";
    private static final String STARTUP_FULLY_DRAWN_WITHOUT_BUNDLE =
            "startup_fully_drawn_without_bundle";

    private static final String PROCESS_START = "process_start";
    private static final String PROCESS_START_DELAY = "process_start_delay";
    private static final String TRANSITION_DELAY_MILLIS = "transition_delay_millis";
    private static final String SOURCE_EVENT_DELAY_MILLIS = "source_event_delay_millis";
    private boolean isProcStartDetailsDisabled;

    private StatsdHelper mStatsdHelper = new StatsdHelper();

    /**
     * Set up the app startup statsd config to track the metrics during the app start occurred.
     */
    @Override
    public boolean startCollecting() {
        Log.i(LOG_TAG, "Adding app startup configs to statsd.");
        List<Integer> atomIdList = new ArrayList<>();
        atomIdList.add(AtomsProto.Atom.APP_START_OCCURRED_FIELD_NUMBER);
        atomIdList.add(AtomsProto.Atom.APP_START_FULLY_DRAWN_FIELD_NUMBER);
        if (!isProcStartDetailsDisabled) {
            atomIdList.add(AtomsProto.Atom.PROCESS_START_TIME_FIELD_NUMBER);
        }
        return mStatsdHelper.addEventConfig(atomIdList);
    }

    /**
     * Collect the app startup metrics tracked during the app startup occurred from the statsd.
     */
    @Override
    public Map<String, StringBuilder> getMetrics() {
        List<StatsLog.EventMetricData> eventMetricData = mStatsdHelper.getEventMetrics();
        Map<String, StringBuilder> appStartResultMap = new HashMap<>();
        Map<String, Integer> appStartCountMap = new HashMap<>();
        Map<String, Integer> tempResultCountMap = new HashMap<>();
        for (StatsLog.EventMetricData dataItem : eventMetricData) {
            AtomsProto.Atom atom = dataItem.atom;
            if (atom == null) {
                atom = dataItem.aggregatedAtomInfo.atom;
            }
            if (atom.hasAppStartOccurred()) {
                AtomsProto.AppStartOccurred appStartAtom = atom.getAppStartOccurred();
                String pkgName = appStartAtom.pkgName;
                String transitionType = String.valueOf(appStartAtom.type);
                int windowsDrawnMillis = appStartAtom.windowsDrawnDelayMillis;
                int transitionDelayMillis = appStartAtom.transitionDelayMillis;
                Log.i(
                        LOG_TAG,
                        String.format(
                                "Pkg Name: %s, Transition Type: %s, "
                                        + "WindowDrawnDelayMillis: %s, TransitionDelayMillis: %s",
                                pkgName,
                                transitionType,
                                windowsDrawnMillis,
                                transitionDelayMillis));

                String metricTypeKey = "";
                String metricTransitionKey = "";
                // To track number of startups per type per package.
                String metricCountKey = "";
                // To track total number of startups per type.
                String totalCountKey = "";
                String typeKey = "";
                switch (appStartAtom.type) {
                    case AtomsProto.AppStartOccurred.COLD:
                        typeKey = COLD_STARTUP;
                        break;
                    case AtomsProto.AppStartOccurred.WARM:
                        typeKey = WARM_STARTUP;
                        break;
                    case AtomsProto.AppStartOccurred.HOT:
                        typeKey = HOT_STARTUP;
                        break;
                    case AtomsProto.AppStartOccurred.UNKNOWN:
                        break;
                    default:
                        break;
                }
                if (!typeKey.isEmpty()) {
                    metricTypeKey = MetricUtility.constructKey(typeKey, pkgName);
                    metricCountKey = MetricUtility.constructKey(typeKey, COUNT, pkgName);
                    totalCountKey = MetricUtility.constructKey(typeKey, TOTAL_COUNT);

                    // Update the windows drawn delay metrics.
                    MetricUtility.addMetric(metricTypeKey, windowsDrawnMillis, appStartResultMap);
                    MetricUtility.addMetric(metricCountKey, appStartCountMap);
                    MetricUtility.addMetric(totalCountKey, appStartCountMap);

                    // Update the transition delay metrics.
                    metricTransitionKey = MetricUtility.constructKey(typeKey,
                            TRANSITION_DELAY_MILLIS, pkgName);
                    MetricUtility.addMetric(metricTransitionKey, transitionDelayMillis,
                            appStartResultMap);
                }
                if (appStartAtom.sourceEventDelayMillis != 0) {
                    int sourceEventDelayMillis = appStartAtom.sourceEventDelayMillis;
                    Log.i(LOG_TAG, String.format("Pkg Name: %s, SourceEventDelayMillis: %d",
                            pkgName, sourceEventDelayMillis));

                    String metricEventDelayKey = MetricUtility.constructKey(
                            SOURCE_EVENT_DELAY_MILLIS, pkgName);
                    MetricUtility.addMetric(metricEventDelayKey, sourceEventDelayMillis,
                            appStartResultMap);
                }
            }
            if (atom.hasAppStartFullyDrawn()) {
                AtomsProto.AppStartFullyDrawn appFullyDrawnAtom = atom.getAppStartFullyDrawn();
                String pkgName = appFullyDrawnAtom.pkgName;
                String transitionType = String.valueOf(appFullyDrawnAtom.type);
                long startupTimeMillis = appFullyDrawnAtom.appStartupTimeMillis;
                Log.i(LOG_TAG, String.format("Pkg Name: %s, Transition Type: %s, "
                        + "AppStartupTimeMillis: %d", pkgName, transitionType, startupTimeMillis));

                String metricKey = "";
                switch (appFullyDrawnAtom.type) {
                    case AtomsProto.AppStartFullyDrawn.UNKNOWN:
                        metricKey = MetricUtility.constructKey(
                                STARTUP_FULLY_DRAWN_UNKNOWN, pkgName);
                        break;
                    case AtomsProto.AppStartFullyDrawn.WITH_BUNDLE:
                        metricKey = MetricUtility.constructKey(
                                STARTUP_FULLY_DRAWN_WITH_BUNDLE, pkgName);
                        break;
                    case AtomsProto.AppStartFullyDrawn.WITHOUT_BUNDLE:
                        metricKey = MetricUtility.constructKey(
                                STARTUP_FULLY_DRAWN_WITHOUT_BUNDLE, pkgName);
                        break;
                    default:
                        metricKey = MetricUtility.constructKey(
                                STARTUP_FULLY_DRAWN_UNKNOWN, pkgName);
                        break;
                }
                if (!metricKey.isEmpty()) {
                    MetricUtility.addMetric(metricKey, startupTimeMillis, appStartResultMap);
                }
            }
            // ProcessStartTime reports startup time for both foreground and background process.
            if (atom.hasProcessStartTime()) {
                AtomsProto.ProcessStartTime processStartTimeAtom = atom.getProcessStartTime();
                String processName = processStartTimeAtom.processName;
                // Number of milliseconds it takes to finish start of the process.
                long processStartDelayMillis = processStartTimeAtom.processStartDelayMillis;
                // Treating activity hosting type as foreground and everything else as background.
                String hostingType =
                        processStartTimeAtom.hostingType.contains("activity") ? "fg" : "bg";
                Log.i(
                        LOG_TAG,
                        String.format(
                                "Process Name: %s, Start Type: %s, Hosting Type: %s,"
                                        + " ProcessStartDelayMillis: %d",
                                processName,
                                processStartTimeAtom.type,
                                hostingType,
                                processStartDelayMillis));
                String metricKey = "";
                // To track number of startups per type per package.
                String metricCountKey = "";
                // To track total number of startups per type.
                String totalCountKey = "";
                String typeKey = "";
                switch (processStartTimeAtom.type) {
                    case AtomsProto.ProcessStartTime.COLD:
                        typeKey = COLD_STARTUP;
                        break;
                    case AtomsProto.ProcessStartTime.WARM:
                        typeKey = WARM_STARTUP;
                        break;
                    case AtomsProto.ProcessStartTime.HOT:
                        typeKey = HOT_STARTUP;
                        break;
                    case AtomsProto.ProcessStartTime.UNKNOWN:
                        break;
                }
                if (!typeKey.isEmpty()) {
                    metricKey = MetricUtility.constructKey(typeKey,
                            PROCESS_START_DELAY, processName, hostingType);
                    metricCountKey = MetricUtility.constructKey(typeKey, PROCESS_START,
                            COUNT, processName, hostingType);
                    totalCountKey = MetricUtility.constructKey(typeKey, PROCESS_START,
                            TOTAL_COUNT);
                    // Update the metrics
                    if (isProcStartDetailsDisabled) {
                        MetricUtility.addMetric(metricCountKey, tempResultCountMap);
                    } else {
                        MetricUtility.addMetric(metricKey, processStartDelayMillis,
                                appStartResultMap);
                        MetricUtility.addMetric(metricCountKey, appStartCountMap);
                    }

                    MetricUtility.addMetric(totalCountKey, appStartCountMap);
                }
            }
        }

        if (isProcStartDetailsDisabled) {
            for (Entry<String, Integer> entry : tempResultCountMap.entrySet()) {
                Log.i(LOG_TAG, String.format("Process_delay_key: %s, Count: %d", entry.getKey(),
                        entry.getValue()));
            }
        }

        // Cast to StringBuilder as the raw app startup metric could be comma separated values
        // if there are multiple app launches.
        Map<String, StringBuilder> finalCountMap = appStartCountMap
                .entrySet()
                .stream()
                .collect(
                        Collectors.toMap(Map.Entry::getKey,
                                e -> new StringBuilder(Integer.toString(e.getValue()))));
        // Add the count map in the app start result map.
        appStartResultMap.putAll(finalCountMap);
        return appStartResultMap;
    }

    /**
     * Remove the statsd config used to track the app startup metrics.
     */
    @Override
    public boolean stopCollecting() {
        return mStatsdHelper.removeStatsConfig();
    }

    /**
     * Disable process start detailed metrics.
     */
    public void setDisableProcStartDetails() {
        isProcStartDetailsDisabled = true;
    }
}
