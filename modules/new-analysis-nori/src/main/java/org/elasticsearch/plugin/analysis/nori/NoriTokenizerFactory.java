/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugin.analysis.nori;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ko.KoreanTokenizer;
import org.apache.lucene.analysis.ko.dict.UserDictionary;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.analysis.Analysis;
import org.elasticsearch.sp.api.analysis.TokenizerFactory;
import org.elasticsearch.sp.api.analysis.annotations.Factory;
import org.elasticsearch.sp.api.analysis.settings.Inject;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;
@Factory(name = "nori_tokenizer")

public class NoriTokenizerFactory implements TokenizerFactory {
    private static final String USER_DICT_PATH_OPTION = "user_dictionary";
    private static final String USER_DICT_RULES_OPTION = "user_dictionary_rules";

    private final UserDictionary userDictionary;
    private String name;
    private final KoreanTokenizer.DecompoundMode decompoundMode;
    private final boolean discardPunctuation;

    @Inject
    public NoriTokenizerFactory(NoriAnalysisSettings noriAnalysisSettings) {
        this.decompoundMode = getMode(noriAnalysisSettings);
        this.userDictionary = getUserDictionary(noriAnalysisSettings);
        this.discardPunctuation = noriAnalysisSettings.isDiscardPunctuation();
        var n = name();
    }

    public static UserDictionary getUserDictionary(NoriAnalysisSettings settings) {
        List<String> ruleList = settings.getUserDictionaryRules();


        StringBuilder sb = new StringBuilder();
        if (ruleList == null || ruleList.isEmpty()) {
            return null;
        }
        for (String line : ruleList) {
            sb.append(line).append(System.lineSeparator());
        }
        try (Reader rulesReader = new StringReader(sb.toString())) {
            return UserDictionary.open(rulesReader);
        } catch (IOException e) {
            throw new ElasticsearchException("failed to load nori user dictionary", e);
        }
    }

    public static KoreanTokenizer.DecompoundMode getMode(NoriAnalysisSettings settings) {
        KoreanTokenizer.DecompoundMode mode = KoreanTokenizer.DEFAULT_DECOMPOUND;
        String modeSetting = settings.getDecompoundMode();
        if (modeSetting != null) {
            mode = KoreanTokenizer.DecompoundMode.valueOf(modeSetting.toUpperCase(Locale.ENGLISH));
        }
        return mode;
    }



    @Override
    public Tokenizer create() {
        return new KoreanTokenizer(
            KoreanTokenizer.DEFAULT_TOKEN_ATTRIBUTE_FACTORY,
            userDictionary,
            decompoundMode,
            false,
            discardPunctuation
        );
    }

}
