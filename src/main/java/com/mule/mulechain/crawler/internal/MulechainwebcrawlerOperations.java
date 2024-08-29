package com.mule.mulechain.crawler.internal;

import com.mule.mulechain.crawler.internal.helpers.crawlingHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class MulechainwebcrawlerOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(MulechainwebcrawlerOperations.class);

  /**
   * Example of an operation that uses the configuration and a connection instance to perform some action.
   *
   * @throws IOException
   */

  /* JSoup limitiations / web crawl challenges
   - some sites prevent robots - use of User-Agent may be required but not always guaranteed to work
   - JavaScript generated content is not read by jsoup
   - some sites require cookies or sessions to be present
   */
  @MediaType(value = ANY, strict = false)
  @Alias("Crawl-website")
  public String crawlWebsite(@Config MulechainwebcrawlerConfiguration configuration,
                             @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url,
                             @DisplayName("Maximum Depth") @Placement(order = 2) @Example("2") int maxDepth,
                             @DisplayName("Download Images") @Placement(order = 3) @Example("Yes") boolean downloadImages,
                             @DisplayName("Save Website Text to File") @Placement(order = 4) @Example("Yes") boolean savePageContents,
                             @Optional @DisplayName("Download Location") @Placement(order = 5) @Example("/users/mulesoft/downloads") String downloadPath) throws IOException {
    LOGGER.info("Website crawl action");


    // initialise variables
    Set<String> visitedLinks = new HashSet<>();
    List<String> specificTags = configuration.getTags();
    List<Map<String, String>> results = new ArrayList<>();

    // start crawl
    List<Map<String, String>> pageContents = startCrawling(url, 0, maxDepth, visitedLinks, downloadImages, downloadPath, specificTags, results);

    String jsonResult = crawlingHelper.covertToJSON(pageContents);
    // save contents if required
    if (savePageContents) {
      saveContents(jsonResult, downloadPath);
    }

    // return content as payload
    return jsonResult;
  }

  @MediaType(value = ANY, strict = false)
  @Alias("Get-website-links")
  public Set<String> getWebsiteLinks(@Config MulechainwebcrawlerConfiguration configuration,
                             @DisplayName("Website URL") @Placement(order = 1) @Example("https://mac-project.ai/docs") String url) throws IOException {
    LOGGER.info("Website get-links action");

    // initialise variables
    Set<String> pageLinks = new HashSet<>();

    // get page as a document
    Document document = crawlingHelper.getDocument(url);

    // Select all anchor tags with href attributes
    Elements links = document.select("a[href]");

    // Iterate over the selected elements and add each link to the HashSet
    for (Element link : links) {
      pageLinks.add(link.attr("abs:href")); // get absolute URLs
    }

    return pageLinks;
  }


  private void saveContents(String pageContents, String downloadPath) throws IOException {
      LOGGER.info("Writing crawled contents to file");
      // Combine directory and filename into a single File object
      File file = new File(downloadPath, "crawl-results.txt");

      // Ensure the directory exists
      file.getParentFile().mkdirs();

      // Use try-with-resources to ensure the BufferedWriter is closed automatically
      // append to file instead of overwriting
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
        writer.write(pageContents);
        LOGGER.info("File saved successfully to " + file.getAbsolutePath());
      } catch (IOException e) {
        LOGGER.info("An error occurred while writing to the file: " + e.getMessage());
        throw e;
      }
  }




  //private String startCrawling(String url, int depth, int maxDepth, Set<String> visitedLinks, boolean downloadImages, String downloadPath, List<String> tags) {
  private List<Map<String, String>> startCrawling(String url, int depth, int maxDepth, Set<String> visitedLinks, boolean downloadImages, String downloadPath, List<String> tags, List<Map<String, String>> results) {



    // return if maxDepth reached
    if (depth > maxDepth || visitedLinks.contains(url)) {
      //return "";
      return results;
    }

    // crawl current page
    StringBuilder collectedText = new StringBuilder();

    try {
      visitedLinks.add(url);

      LOGGER.info("Fetching content for : " + url);

      // get page as a html document
      Document document = crawlingHelper.getDocument(url);


      // check if crawl should only iterate over specified tags and extract contents from these tags only
      if (tags != null && !tags.isEmpty()) {
        for (String selector : tags) {
          Elements elements = document.select(selector);
          for (Element element : elements) {
            collectedText.append(element.text());
          }
        }
      }
      else {
        // Extract the text content of the page and add it to the collected text
        String textContent = document.text();
        collectedText.append(textContent);
      }

      // check if need to download images in the current page
      if (downloadImages) {
        LOGGER.info("Downloading images for : " + url);
        // Save all images found on the page
        Elements images = document.select("img[src]");
        LOGGER.info("Number of img[src] elements found : " + images.size());
        for (Element img : images) {
          String imgUrl = img.absUrl("src");
          saveImage(imgUrl, downloadPath);
        }
      }

      // Create JSON object for the current page
      Map<String, String> pageData = new HashMap<>();
      pageData.put("path", url);
      pageData.put("content", collectedText.toString());
      results.add(pageData);


      // If not at max depth, find and crawl the links on the page
      if (depth < maxDepth) {
        // get all links on the current page
        Elements links = document.select("a[href]");
        for (Element link : links) {
          String nextUrl = link.absUrl("href");
          // start crawl of the next page (nextUrl)
          collectedText.append(startCrawling(nextUrl, depth + 1, maxDepth, visitedLinks, downloadImages, downloadPath, tags, results));
        }
      }

    } catch (Exception e) {
      LOGGER.error(e.toString());
    }
    return results;
  }

  private void saveImage(String imageUrl, String saveDirectory) throws IOException {
    LOGGER.info("Found image : " + imageUrl);
    try {
      // Check if the URL is a Data URL
      if (imageUrl.startsWith("data:image/")) {
        // Extract base64 data from the Data URL
        String base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1);

        if (base64Data.isEmpty()) {
          LOGGER.info("Base64 data is empty for URL: " + imageUrl);
          return;
        }

        // Decode the base64 data
        byte[] imageBytes;

        try {
          imageBytes = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
          LOGGER.info("Error decoding base64 data: " + e.getMessage());
          return;
        }

        if (imageBytes.length == 0) {
          LOGGER.info("Decoded image bytes are empty for URL: " + imageUrl);
          return;
        }

        // Determine the file extension from the Data URL
        String fileType = imageUrl.substring(5, imageUrl.indexOf(";"));
        String fileExtension = fileType.split("/")[1];

        // Generate a unique filename using the current timestamp
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String fileName = "image_" + timestamp + "." + fileExtension;
        File file = new File(saveDirectory, fileName);

        // Ensure the directory exists
        file.getParentFile().mkdirs();

        // Write the decoded bytes to the file
        try (FileOutputStream out = new FileOutputStream(file)) {
          out.write(imageBytes);
          LOGGER.info("DataImage saved: " + file.getAbsolutePath());
        }
      } else {
        // Handle standard image URLs
        URL url = new URL(imageUrl);

        // Extract the 'url' parameter from the query string
        String decodedUrl = crawlingHelper.extractAndDecodeUrl(imageUrl);
        // Extract the filename from the decoded URL
        String fileName = crawlingHelper.extractFileNameFromUrl(decodedUrl);

        //String fileName = decodedUrl.substring(imageUrl.lastIndexOf("/") + 1);
        File file = new File(saveDirectory, fileName);

        // Ensure the directory exists
        file.getParentFile().mkdirs();

        // Download and save the image
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(file)) {

          byte[] buffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
          }
        }
        LOGGER.info("Image saved: " + file.getAbsolutePath());

      }
    } catch (IOException e) {
      LOGGER.error("Error saving image: " + imageUrl);
      throw e;
    }
  }
}

