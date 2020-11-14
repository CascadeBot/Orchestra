package org.cascadebot.orchestra.data;

import org.cascadebot.orchestra.data.enums.SearchResultType;

public class SearchResult {

    private SearchResultType type;
    private String url;
    private String title;

    public SearchResult(SearchResultType type, String url, String title) {
        this.type = type;
        this.url = url;
        this.title = title;
    }

    public SearchResultType getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }
}
