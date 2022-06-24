/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.custom;

import org.apache.lucene.analysis.charfilter.BaseCharFilter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class CustomAnalysisCharFilter extends BaseCharFilter {
    private CustomAnalysisSettings analysisSettings;
    private final CustomClusterSettings customClusterSettings;
    private Reader transformedInput;

    public CustomAnalysisCharFilter(CustomAnalysisSettings analysisSettings, CustomClusterSettings customClusterSettings, Reader reader) {
        super(reader);
        this.analysisSettings = analysisSettings;
        this.customClusterSettings = customClusterSettings;
    }


    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        // Buffer all input on the first call.
        if (transformedInput == null) {
            fill();
        }

        return transformedInput.read(cbuf, off, len);
    }

    private void fill() throws IOException {
        StringBuilder buffered = new StringBuilder();
        char[] temp = new char[1024];
        for (int cnt = input.read(temp); cnt > 0; cnt = input.read(temp)) {
            buffered.append(temp, 0, cnt);
        }
        transformedInput = new StringReader(process(buffered).toString());
    }

    private StringBuilder process(StringBuilder input) {
        long increase = analysisSettings.getNumberIncrease();
        for (int index = 0; index < input.length(); ++index) {
            char c = input.charAt(index);
            if (c >= '0' && c <= '9') {
                int v = c -'0';
                int nv = (int)(v+increase) % 9;
                input.setCharAt(index, (char) ('0'+nv));
            }
        }
        input.append(customClusterSettings.getSuffix());
        return input;
    }

}
