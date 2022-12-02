/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.util;

import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.elasticsearch.common.util.BigLongArray.writePages;
import static org.elasticsearch.common.util.PageCacheRecycler.DOUBLE_PAGE_SIZE;

public class GeoBigArray extends AbstractBigArray {
    private static final GeoBigArray ESTIMATOR = new GeoBigArray(0, BigArrays.NON_RECYCLING_INSTANCE, false);

    static final VarHandle VH_PLATFORM_NATIVE_DOUBLE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.nativeOrder());
    /** Page size in bytes: 16KB */
    public static final int PAGE_SIZE_IN_BYTES = 1 << 14;
    public static final int ELEMENT_SHIFT = 6;
    public static final int ELEMENT_SIZE_IN_BYTES = 1 << ELEMENT_SHIFT;   // 32 (6x8 + 16pad = 64)
    public static final int ELEMENTS_PER_PAGE = PAGE_SIZE_IN_BYTES / ELEMENT_SIZE_IN_BYTES;

    private byte[][] pages;

    /** Constructor. */
    GeoBigArray(long size, BigArrays bigArrays, boolean clearOnResize) {
        super(DOUBLE_PAGE_SIZE, bigArrays, clearOnResize);
        this.size = size;
        pages = new byte[numPages(size)][];
        for (int i = 0; i < pages.length; ++i) {
            pages[i] = newBytePage(i);
        }
    }

    public void fill_tops(long fromIndex, long toIndex, double value) {fill(fromIndex, toIndex, value, 0);}
    public void fill_bottoms(long fromIndex, long toIndex, double value) {fill(fromIndex, toIndex, value, 1);}
    public void fill_posLefts(long fromIndex, long toIndex, double value) {fill(fromIndex, toIndex, value, 2);}
    public void fill_posRights(long fromIndex, long toIndex, double value) {fill(fromIndex, toIndex, value, 3);}
    public void fill_negLefts(long fromIndex, long toIndex, double value) {fill(fromIndex, toIndex, value, 4);}
    public void fill_negRights(long fromIndex, long toIndex, double value) {fill(fromIndex, toIndex, value, 5);}


   public double tops_set(long index, double value){ return set(index,0,value);}
   public double bottoms_set(long index, double value){ return set(index,1,value);}
   public double posLefts_set(long index, double value){ return set(index,2,value);}
   public double posRights_set(long index, double value){ return set(index,3,value);}
   public double negLefts_set(long index, double value){ return set(index,4,value);}
   public double negRights_set(long index, double value){ return set(index,5,value);}

    public double tops_get(long index){return get(index,0);}
    public double bottoms_get(long index){return get(index,1);}
    public double posLefts_get(long index){return get(index,2);}
    public double posRights_get(long index){return get(index,3);}
    public double negLefts_get(long index){return get(index,4);}
    public double negRights_get(long index){return get(index,5);}

    private double get(long index, int el) {
        final int pageIndex = pageIndex(index);
        final int indexInPage = indexInPage(index);
        return (long) VH_PLATFORM_NATIVE_DOUBLE.get(pages[pageIndex],(indexInPage << ELEMENT_SHIFT)+Double.BYTES*el);
    }

    private double set(long index, int el, double value) {
        final int pageIndex = pageIndex(index);
        final int indexInPage = indexInPage(index);
        final byte[] page = pages[pageIndex];
        final double ret = (double) VH_PLATFORM_NATIVE_DOUBLE.get(page, (indexInPage << ELEMENT_SHIFT)+Double.BYTES*el);
        return ret;
    }

    @Override
    protected int numBytesPerElement() {
        return Integer.BYTES;//?
    }

    /** Change the size of this array. Content between indexes <code>0</code> and <code>min(size(), newSize)</code> will be preserved. */
    @Override
    public void resize(long newSize) {
        final int numPages = numPages(newSize);
        if (numPages > pages.length) {
            pages = Arrays.copyOf(pages, ArrayUtil.oversize(numPages, RamUsageEstimator.NUM_BYTES_OBJECT_REF));
        }
        for (int i = numPages - 1; i >= 0 && pages[i] == null; --i) {
            pages[i] = newBytePage(i);
        }
        for (int i = numPages; i < pages.length && pages[i] != null; ++i) {
            pages[i] = null;
            releasePage(i);
        }
        this.size = newSize;
    }
//    @Override
    public void fill(long fromIndex, long toIndex, double value, int el) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        final int fromPage = pageIndex(fromIndex);
        final int toPage = pageIndex(toIndex - 1);
        if (fromPage == toPage) {
            fill(pages[fromPage], indexInPage(fromIndex), indexInPage(toIndex - 1) + 1, value, el);
        } else {
            fill(pages[fromPage], indexInPage(fromIndex), pageSize(), value, el);
            for (int i = fromPage + 1; i < toPage; ++i) {
                fill(pages[i], 0, pageSize(), value, el);
            }
            fill(pages[toPage], 0, indexInPage(toIndex - 1) + 1, value, el);
        }
    }

    public static void fill(byte[] page, int from, int to, double value, int el) {
        if (from < to) {
            VH_PLATFORM_NATIVE_DOUBLE.set(page, from << ELEMENT_SHIFT, value);
            fillBySelfCopy(page, from << ELEMENT_SHIFT + Double.BYTES*el, to << ELEMENT_SHIFT + Double.BYTES*el, Double.BYTES);
        }
    }

    /** Estimates the number of bytes that would be consumed by an array of the given size. */
    public static long estimateRamBytes(final long size) {
        return ESTIMATOR.ramBytesEstimated(size);
    }

//    @Override
//    public void set(long index, byte[] buf, int offset, int len) {
//        set(index, buf, offset, len, pages, 3);
//    }

//    @Override
    public void writeTo(StreamOutput out) throws IOException {
        writePages(out, Math.toIntExact(size), pages, Double.BYTES, DOUBLE_PAGE_SIZE);
    }
}
