package com.indolyn.rill.core.execution.trace;

import java.util.ArrayList;
import java.util.List;

public final class TraceCollector {

    private static final ThreadLocal<List<TraceEvent>> EVENTS = new ThreadLocal<>();

    private TraceCollector() {
    }

    public static void start() {
        EVENTS.set(new ArrayList<>());
    }

    public static void record(
        String stage,
        String component,
        String sourceFile,
        String sourceMethod,
        String detail) {
        List<TraceEvent> events = EVENTS.get();
        if (events == null) {
            return;
        }
        events.add(new TraceEvent(stage, component, sourceFile, sourceMethod, detail));
    }

    public static List<TraceEvent> stop() {
        List<TraceEvent> events = EVENTS.get();
        EVENTS.remove();
        return events == null ? List.of() : List.copyOf(events);
    }
}
