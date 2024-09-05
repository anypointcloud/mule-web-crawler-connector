package com.mule.mulechain.crawler.internal;

import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.util.List;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Operations(MulechainwebcrawlerOperations.class)
public class MulechainwebcrawlerConfiguration {

  @Parameter
  @Optional
  @DisplayName("Tag List")
  private List<String> tags;

  // Getters and Setters
  public List<String> getTags() {
    return this.tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }
}
