/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.analysis.nori;

import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.analysis.ko.KoreanTokenizer;
import org.apache.lucene.analysis.ko.POS;
import org.apache.lucene.analysis.ko.dict.UserDictionary;
import org.elasticsearch.sp.api.analysis.Analyzer;
import org.elasticsearch.sp.api.analysis.settings.Inject;

import java.util.Set;

public class NoriAnalyzerProvider implements Analyzer<KoreanAnalyzer> {
    private final KoreanAnalyzer analyzer;

    @Inject
    public NoriAnalyzerProvider(NoriAnalysisSettings noriAnalysisSettings) {
        super();
        final KoreanTokenizer.DecompoundMode mode = NoriTokenizerFactory.getMode(noriAnalysisSettings);
        final UserDictionary userDictionary = NoriTokenizerFactory.getUserDictionary(noriAnalysisSettings);
        final Set<POS.Tag> stopTags = NoriPartOfSpeechStopFilterFactory.getStopTags(noriAnalysisSettings);
        analyzer = new KoreanAnalyzer(userDictionary, mode, stopTags, false);
    }

    @Override
    public KoreanAnalyzer get() {
        return analyzer;
    }
}
