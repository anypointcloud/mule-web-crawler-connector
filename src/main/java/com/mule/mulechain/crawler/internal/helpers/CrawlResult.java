package com.mule.mulechain.crawler.internal.helpers;

public class CrawlResult extends SiteMapNode {
    private String fileName;

    public CrawlResult(String url, String fileName) {
        super(url);
        this.fileName = fileName;

    }

    public String getFileName() {
        return fileName;
    }
}
