/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.aggregations.metrics;

import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.util.DoubleArray;
import org.elasticsearch.common.util.GeoBigArray;
import org.elasticsearch.core.Releasables;
import org.elasticsearch.index.fielddata.MultiGeoPointValues;
import org.elasticsearch.search.aggregations.AggregationExecutionContext;
import org.elasticsearch.search.aggregations.Aggregator;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.LeafBucketCollector;
import org.elasticsearch.search.aggregations.LeafBucketCollectorBase;
import org.elasticsearch.search.aggregations.support.AggregationContext;
import org.elasticsearch.search.aggregations.support.ValuesSource;
import org.elasticsearch.search.aggregations.support.ValuesSourceConfig;
import org.elasticsearch.xcontent.ParseField;

import java.io.IOException;
import java.util.Map;

final class GeoBoundsAggregator extends MetricsAggregator {

    static final ParseField WRAP_LONGITUDE_FIELD = new ParseField("wrap_longitude");

    private final ValuesSource.GeoPoint valuesSource;
    private final boolean wrapLongitude;
    DoubleArray tops;
//    DoubleArray bottoms;
//    DoubleArray posLefts;
//    DoubleArray posRights;
//    DoubleArray negLefts;
//    DoubleArray negRights;

    GeoBigArray geoBigArray;

    // TODO: update with a DoubleDoubleDoubleDoubleDoubleDoubleArray

    GeoBoundsAggregator(
        String name,
        AggregationContext context,
        Aggregator parent,
        ValuesSourceConfig valuesSourceConfig,
        boolean wrapLongitude,
        Map<String, Object> metadata
    ) throws IOException {
        super(name, context, parent, metadata);
        // TODO: stop expecting nulls here
        this.valuesSource = valuesSourceConfig.hasValues() ? (ValuesSource.GeoPoint) valuesSourceConfig.getValuesSource() : null;
        this.wrapLongitude = wrapLongitude;
        if (valuesSource != null) {
            geoBigArray = bigArrays().newGeoBigArray(1, false);
            geoBigArray.fill_tops(0,geoBigArray.size()/6,Double.NEGATIVE_INFINITY);
            geoBigArray.fill_bottoms(0,geoBigArray.size()/6,Double.POSITIVE_INFINITY);
            geoBigArray.fill_posLefts(0,geoBigArray.size()/6,Double.POSITIVE_INFINITY);
            geoBigArray.fill_posRights(0,geoBigArray.size()/6,Double.NEGATIVE_INFINITY);
            geoBigArray.fill_negLefts(0,geoBigArray.size()/6,Double.POSITIVE_INFINITY);
            geoBigArray.fill_negRights(0,geoBigArray.size()/6,Double.NEGATIVE_INFINITY);
//            tops = bigArrays().newDoubleArray(1, false);
//            geoBigArray.fill(0, tops.size(), Double.NEGATIVE_INFINITY);
//            bottoms = bigArrays().newDoubleArray(1, false);
//            bottoms.fill(0, bottoms.size(), Double.POSITIVE_INFINITY);
//            posLefts = bigArrays().newDoubleArray(1, false);
//            posLefts.fill(0, posLefts.size(), Double.POSITIVE_INFINITY);
//            posRights = bigArrays().newDoubleArray(1, false);
//            posRights.fill(0, posRights.size(), Double.NEGATIVE_INFINITY);
//            negLefts = bigArrays().newDoubleArray(1, false);
//            negLefts.fill(0, negLefts.size(), Double.POSITIVE_INFINITY);
//            negRights = bigArrays().newDoubleArray(1, false);
//            negRights.fill(0, negRights.size(), Double.NEGATIVE_INFINITY);
        }
    }

    @Override
    public LeafBucketCollector getLeafCollector(AggregationExecutionContext aggCtx, LeafBucketCollector sub) {
        if (valuesSource == null) {
            return LeafBucketCollector.NO_OP_COLLECTOR;
        }
        final MultiGeoPointValues values = valuesSource.geoPointValues(aggCtx.getLeafReaderContext());
        return new LeafBucketCollectorBase(sub, values) {
            @Override
            public void collect(int doc, long bucket) throws IOException {
                if (bucket >= geoBigArray.size()) {
                    long from = geoBigArray.size();
                    geoBigArray = bigArrays().grow(geoBigArray, bucket + 1);
                    geoBigArray.fill_tops(from,geoBigArray.size()/6,Double.NEGATIVE_INFINITY);
                    geoBigArray.fill_bottoms(from,geoBigArray.size()/6,Double.POSITIVE_INFINITY);
                    geoBigArray.fill_posLefts(from,geoBigArray.size()/6,Double.POSITIVE_INFINITY);
                    geoBigArray.fill_posRights(from,geoBigArray.size()/6,Double.NEGATIVE_INFINITY);
                    geoBigArray.fill_negLefts(from,geoBigArray.size()/6,Double.POSITIVE_INFINITY);
                    geoBigArray.fill_negRights(from,geoBigArray.size()/6,Double.NEGATIVE_INFINITY);
                }

                if (values.advanceExact(doc)) {
                    final int valuesCount = values.docValueCount();

                    for (int i = 0; i < valuesCount; ++i) {
                        GeoPoint value = values.nextValue();
                        double top = geoBigArray.tops_get(bucket);
                        if (value.lat() > top) {
                            top = value.lat();
                        }
                        double bottom = geoBigArray.bottoms_get(bucket);
                        if (value.lat() < bottom) {
                            bottom = value.lat();
                        }
                        double posLeft = geoBigArray.posLefts_get(bucket);
                        if (value.lon() >= 0 && value.lon() < posLeft) {
                            posLeft = value.lon();
                        }
                        double posRight = geoBigArray.posRights_get(bucket);
                        if (value.lon() >= 0 && value.lon() > posRight) {
                            posRight = value.lon();
                        }
                        double negLeft = geoBigArray.negLefts_get(bucket);
                        if (value.lon() < 0 && value.lon() < negLeft) {
                            negLeft = value.lon();
                        }
                        double negRight = geoBigArray.negRights_get(bucket);
                        if (value.lon() < 0 && value.lon() > negRight) {
                            negRight = value.lon();
                        }
                        geoBigArray.tops_set(bucket, top);
                        geoBigArray.bottoms_set(bucket, bottom);
                        geoBigArray.posLefts_set(bucket, posLeft);
                        geoBigArray.posRights_set(bucket, posRight);
                        geoBigArray.negLefts_set(bucket, negLeft);
                        geoBigArray.negRights_set(bucket, negRight);
                    }
                }
            }
        };
    }

    @Override
    public InternalAggregation buildAggregation(long owningBucketOrdinal) {
        if (valuesSource == null) {
            return buildEmptyAggregation();
        }
        double top = geoBigArray.tops_get(owningBucketOrdinal);
        double bottom = geoBigArray.bottoms_get(owningBucketOrdinal);
        double posLeft = geoBigArray.posLefts_get(owningBucketOrdinal);
        double posRight = geoBigArray.posRights_get(owningBucketOrdinal);
        double negLeft = geoBigArray.negLefts_get(owningBucketOrdinal);
        double negRight = geoBigArray.negRights_get(owningBucketOrdinal);
        return new InternalGeoBounds(name, top, bottom, posLeft, posRight, negLeft, negRight, wrapLongitude, metadata());
    }

    @Override
    public InternalAggregation buildEmptyAggregation() {
        return new InternalGeoBounds(
            name,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            wrapLongitude,
            metadata()
        );
    }

    @Override
    public void doClose() {
        Releasables.close(geoBigArray);
    }
}
