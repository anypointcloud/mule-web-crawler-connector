package com.mule.mulechain.crawler.internal;

import org.jsoup.Jsoup;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
    Set<String> visitedLinks = new HashSet<>();
    String pageContents = startCrawling(url, 0, maxDepth, visitedLinks, downloadImages, downloadPath);

    if (savePageContents) {
      LOGGER.info("Writing crawled contents to file");
      // Combine directory and filename into a single File object
      File file = new File(downloadPath, "crawl-results.txt");

      // Ensure the directory exists
      file.getParentFile().mkdirs();

      // Use try-with-resources to ensure the BufferedWriter is closed automatically
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
        writer.write(pageContents);
        LOGGER.info("File saved successfully to " + file.getAbsolutePath());
      } catch (IOException e) {
        LOGGER.info("An error occurred while writing to the file: " + e.getMessage());
        throw e;
      }
    }
    return pageContents;
  }


  private String startCrawling(String url, int depth, int maxDepth, Set<String> visitedLinks, boolean downloadImages, String downloadPath) {
    if (depth > maxDepth || visitedLinks.contains(url)) {
      return "";
    }

    StringBuilder collectedText = new StringBuilder();

    try {
      visitedLinks.add(url);

      LOGGER.info("Fetching content for : " + url);
      Document document = Jsoup.connect(url)
              //.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
              //.referrer("http://www.google.com")  // to prevent "HTTP error fetching URL. Status=403" error
              .get();


      // Extract the text content of the document and add it to the collected text
      String textContent = document.text();
      collectedText.append(textContent).append("\n");


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

      // If not at max depth, find and crawl the links on the page
      if (depth < maxDepth) {
        Elements links = document.select("a[href]");
        for (Element link : links) {
          String nextUrl = link.absUrl("href");
          collectedText.append(startCrawling(nextUrl, depth + 1, maxDepth, visitedLinks, downloadImages, downloadPath));
        }
      }

    } catch (Exception e) {
      LOGGER.error(e.toString());
      return e.toString();
    }
    return collectedText.toString();
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
        String decodedUrl = extractAndDecodeUrl(imageUrl);
        // Extract the filename from the decoded URL
        String fileName = extractFileNameFromUrl(decodedUrl);

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

  /*
              "https://wp.salesforce.com/en-ap/wp-content/uploads/sites/14/2024/02/php-marquee-starter-lg-bg.jpg?w=1024",
            "https://example.com/image?url=%2F_next%2Fstatic%2Fmedia%2Fcard-1.8b03e519.png&w=3840&q=75"
   */
  private String extractAndDecodeUrl(String fullUrl) throws UnsupportedEncodingException, MalformedURLException {

    URL url = new URL(fullUrl);
    String query = url.getQuery(); // Extract the query string from the URL

    if (query != null) {
      // Extract and decode the 'url' parameter from the query string
      String[] params = query.split("&");
      for (String param : params) {
        String[] pair = param.split("=");
        if (pair.length == 2 && "url".equals(pair[0])) {
          return URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name());
        }
      }
      // If 'url' parameter not found, return the URL without changes
      return fullUrl;
    } else {
      // If there's no query string, return the URL as is
      return fullUrl;
    }
  }


  private String extractFileNameFromUrl(String url) {
    // Extract the filename from the URL path
    String fileName = url.substring(url.lastIndexOf("/") + 1, url.indexOf('?') > 0 ? url.indexOf('?') : url.length());

    // if no extension for image found, then use .jpg as default
    return fileName.contains(".") ? fileName : fileName + ".jpg";
  }
}

