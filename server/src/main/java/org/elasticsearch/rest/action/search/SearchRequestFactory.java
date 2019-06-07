package org.elasticsearch.rest.action.search;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.rest.RestRequest;

import java.io.IOException;
import java.util.List;

interface  SearchRequestFactory {
    static SearchRequestFactory forRequestVersion(RestRequest request) {
        List<String> allHeaderValues = request.getAllHeaderValues("Content-Type");
        if(allHeaderValues.contains("application/vnd.elasticsearch.v8+json")){
            return new SearchRequestFactoryV8();
        }else{
            return new SearchRequestFactoryV7();
        }
    }

    SearchRequest build(RestRequest request) throws IOException;
}
