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

    private CustomNodeSettings nodeSettings;

    public CustomAnalysisPlugin(CustomNodeSettings nodeSettings) {
//        maxCacheSize = analysisSettings.getNumberIncrease();
        this.nodeSettings = nodeSettings;
        System.out.println(nodeSettings.getMlSetting());
    }

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<CharFilterFactory>> getCharFilters() {
        return Map.of("custom_replace", new MyCustomAnalysisProvider<CharFilterFactory>());
    }
}
