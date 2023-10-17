/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.telemetry.apm.internal.metrics;

import io.opentelemetry.api.metrics.Meter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DoubleGaugeAdapter wraps an otel ObservableDoubleMeasurement
 */
public class DoubleGaugeAdapter extends AbstractInstrument<io.opentelemetry.api.metrics.ObservableDoubleGauge>
    implements
    org.elasticsearch.telemetry.metric.DoubleGauge {

//    private final AtomicReference<Queue<ValueWithAttributes>> valueWithAttributes = new AtomicReference<>(new ConcurrentLinkedQueue<>()/*bounds?*/);
    private final Queue<ValueWithAttributes> valueWithAttributes = new ConcurrentLinkedQueue<>();//bounds?

    public DoubleGaugeAdapter(Meter meter, String name, String description, String unit) {
        super(meter, name, description, unit);
    }

    @Override
    io.opentelemetry.api.metrics.ObservableDoubleGauge buildInstrument(Meter meter) {
        return Objects.requireNonNull(meter)
            .gaugeBuilder(getName())
            .setDescription(getDescription())
            .setUnit(getUnit())
            .buildWithCallback(measurement -> {
                //will progress and finish
                // Queue<ValueWithAttributes> andSet = valueWithAttributes.getAndSet(new ConcurrentLinkedQueue<>());

                //might be keep on looping
                while (valueWithAttributes.peek() != null) {
                    var v = valueWithAttributes.poll();
                    measurement.record(v.value(), OtelHelper.fromMap(v.attributes()));
                }

            });
    }

    @Override
    public void record(double value) {
        record(value, Collections.emptyMap());
    }

    @Override
    public void record(double value, Map<String, Object> attributes) {
        valueWithAttributes.offer(new ValueWithAttributes(value, attributes));
    }

    private record ValueWithAttributes(double value, Map<String, Object> attributes) {
    }
}
