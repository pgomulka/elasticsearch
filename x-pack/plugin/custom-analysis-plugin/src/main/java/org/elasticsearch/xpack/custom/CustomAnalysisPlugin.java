package org.elasticsearch.xpack.custom;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.CharFilterFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public class CustomAnalysisPlugin extends Plugin implements AnalysisPlugin {

    private final long maxCacheSize;
    private CustomSettings analysisSettings;

    public CustomAnalysisPlugin(CustomSettings analysisSettings) {
        maxCacheSize = analysisSettings.getNumberIncrease();
        this.analysisSettings = analysisSettings;
    }

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<CharFilterFactory>> getCharFilters() {
        return Map.of("custom_replace", new AnalysisModule.AnalysisProvider<CharFilterFactory>() {
            @Override
            public CharFilterFactory get(IndexSettings indexSettings, Environment environment, String name, Settings settings) throws IOException {
                return new CharFilterFactory() {
                    @Override
                    public String name() {
                        return "custom_replace";
                    }

                    @Override
                    public Reader create(Reader reader) {
                        return new CustomAnalysisCharFilter(analysisSettings, reader);
                    }
                };
            }
        });
    }
}
