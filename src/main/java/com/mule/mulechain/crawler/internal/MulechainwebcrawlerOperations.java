package com.mule.mulechain.crawler.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Config;
import java.io.IOException;  
import org.jsoup.Jsoup;  
import org.jsoup.nodes.Document;  

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class MulechainwebcrawlerOperations {

  /**
   * Example of an operation that uses the configuration and a connection instance to perform some action.
   * @throws IOException 
   */
  
  @MediaType(value = ANY, strict = false)
  @Alias("Crawl-website")  
  public String crawlWebsite(String url, @Config MulechainwebcrawlerConfiguration configuration) throws IOException{

        return "";
  }

}
