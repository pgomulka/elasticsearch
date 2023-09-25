/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.telemetry.apm.internal.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

import org.apache.lucene.util.SetOnce;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.telemetry.metric.DoubleCounter;
import org.elasticsearch.telemetry.metric.DoubleGauge;
import org.elasticsearch.telemetry.metric.DoubleHistogram;
import org.elasticsearch.telemetry.metric.DoubleUpDownCounter;
import org.elasticsearch.telemetry.metric.LongCounter;
import org.elasticsearch.telemetry.metric.LongGauge;
import org.elasticsearch.telemetry.metric.LongHistogram;
import org.elasticsearch.telemetry.metric.LongUpDownCounter;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Supplier;

import static org.elasticsearch.telemetry.apm.internal.APMAgentSettings.APM_ENABLED_SETTING;

public class APMMeter extends AbstractLifecycleComponent implements org.elasticsearch.telemetry.metric.Meter {
    private final Instruments instruments;

    private final Supplier<Meter> otelMeterSupplier;
    private final Supplier<Meter> noopMeterSupplier;

    private volatile boolean enabled;

    public APMMeter(Settings settings) {
        this(settings, APMMeter::otelMeter, APMMeter::noopMeter);
    }

    public APMMeter(Settings settings, Supplier<Meter> otelMeterSupplier, Supplier<Meter> noopMeterSupplier) {
        this.enabled = APM_ENABLED_SETTING.get(settings);
        this.otelMeterSupplier = otelMeterSupplier;
        this.noopMeterSupplier = noopMeterSupplier;
        this.instruments = new Instruments(enabled ? createApmServices() : noopMeterSupplier.get());
        setEnabled(enabled);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setupApmServices(this.enabled);
    }

    private void setupApmServices(boolean enabled) {
        if (enabled) {
            instruments.setProvider(createApmServices());
        } else {
            destroyApmServices();
        }
    }

    @Override
    protected void doStart() {
        if (enabled) {
            createApmServices();
        }
    }

    @Override
    protected void doStop() {
        destroyApmServices();
    }

    @Override
    protected void doClose() {}

    @Override
    public DoubleCounter registerDoubleCounter(String name, String description, String unit) {
        return instruments.registerDoubleCounter(name, description, unit);
    }

    @Override
    public DoubleCounter getDoubleCounter(String name) {
        return instruments.getDoubleCounter(name);
    }

    @Override
    public DoubleUpDownCounter registerDoubleUpDownCounter(String name, String description, String unit) {
        return instruments.registerDoubleUpDownCounter(name, description, unit);
    }

    @Override
    public DoubleUpDownCounter getDoubleUpDownCounter(String name) {
        return instruments.getDoubleUpDownCounter(name);
    }

    @Override
    public DoubleGauge registerDoubleGauge(String name, String description, String unit) {
        return instruments.registerDoubleGauge(name, description, unit);
    }

    @Override
    public DoubleGauge getDoubleGauge(String name) {
        return instruments.getDoubleGauge(name);
    }

    @Override
    public DoubleHistogram registerDoubleHistogram(String name, String description, String unit) {
        return instruments.registerDoubleHistogram(name, description, unit);
    }

    @Override
    public DoubleHistogram getDoubleHistogram(String name) {
        return instruments.getDoubleHistogram(name);
    }

    @Override
    public LongCounter registerLongCounter(String name, String description, String unit) {
        return instruments.registerLongCounter(name, description, unit);
    }

    @Override
    public LongCounter getLongCounter(String name) {
        return instruments.getLongCounter(name);
    }

    @Override
    public LongUpDownCounter registerLongUpDownCounter(String name, String description, String unit) {
        return instruments.registerLongUpDownCounter(name, description, unit);
    }

    @Override
    public LongUpDownCounter getLongUpDownCounter(String name) {
        return instruments.getLongUpDownCounter(name);
    }

    @Override
    public LongGauge registerLongGauge(String name, String description, String unit) {
        return instruments.registerLongGauge(name, description, unit);
    }

    @Override
    public LongGauge getLongGauge(String name) {
        return instruments.getLongGauge(name);
    }

    @Override
    public LongHistogram registerLongHistogram(String name, String description, String unit) {
        return instruments.registerLongHistogram(name, description, unit);
    }

    @Override
    public LongHistogram getLongHistogram(String name) {
        return instruments.getLongHistogram(name);
    }

    Meter createApmServices() {
        assert this.enabled;

        SetOnce<Meter> provider = new SetOnce<>();
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            provider.set(otelMeterSupplier.get());
            return null;
        });
        return provider.get();
    }

    private void destroyApmServices() {
        instruments.setProvider(noopMeterSupplier.get());
    }

    private static Meter noopMeter() {
        return OpenTelemetry.noop().getMeter("noop");
    }

    private static Meter otelMeter() {
        var openTelemetry = GlobalOpenTelemetry.get();
        var meter = openTelemetry.getMeter("elasticsearch");
        return meter;
    }

    // scope for testing
    Instruments getInstruments() {
        return instruments;
    }
}
