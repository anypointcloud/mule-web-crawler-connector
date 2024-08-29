package com.mule.mulechain.crawler.internal.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class crawlingHelper {

    /*
    private static String getTitle(String url, String outputFolder) throws IOException{
        Document doc = connectUrlGetDocument(url);
        String title = doc.title();
        //System.out.println("title is: " + title);
        return title;
    }

    private static Document connectUrlGetDocument(String url) throws IOException {
        return Jsoup.connect(url).get();
    }

     */

    public static Document getDocument(String url) throws IOException {
        // use jsoup to fetch the current page elements
        Document document = Jsoup.connect(url)
                //.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                //.referrer("http://www.google.com")  // to prevent "HTTP error fetching URL. Status=403" error
                .get();

    /*
    Elements elements = document.select(selector);
    for (Element element : elements) {
      collectedText.append(element.text()).append("\n");
    }
     */

        return document;
    }


    public static String extractFileNameFromUrl(String url) {
        // Extract the filename from the URL path
        String fileName = url.substring(url.lastIndexOf("/") + 1, url.indexOf('?') > 0 ? url.indexOf('?') : url.length());

        // if no extension for image found, then use .jpg as default
        return fileName.contains(".") ? fileName : fileName + ".jpg";
    }

    /*
            "https://wp.salesforce.com/en-ap/wp-content/uploads/sites/14/2024/02/php-marquee-starter-lg-bg.jpg?w=1024",
          "https://example.com/image?url=%2F_next%2Fstatic%2Fmedia%2Fcard-1.8b03e519.png&w=3840&q=75"
 */
    public static String extractAndDecodeUrl(String fullUrl) throws UnsupportedEncodingException, MalformedURLException {

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


    public static String covertToJSON(List<Map<String, String>> pageContents) throws JsonProcessingException{
        // Convert the result to JSON
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pageContents);
    }

    public static Map<String, String> getPageMetaTags(Document document) {
        // Map to store meta tag data
        Map<String, String> metaTagData = new HashMap<>();

        // Select all meta tags
        Elements metaTags = document.select("meta");

        // Iterate through each meta tag
        for (Element metaTag : metaTags) {
            // Extract the 'name' or 'property' attribute and 'content' attribute
            String name = metaTag.attr("name");
            if (name.isEmpty()) {
                // If 'name' is not present, check for 'property' (e.g., Open Graph meta tags)
                name = metaTag.attr("property");
            }
            String content = metaTag.attr("content");

            // Only add to map if 'name' or 'property' and 'content' are present
            if (!name.isEmpty() && !content.isEmpty()) {
                metaTagData.put(name, content);
            }
        }
        return metaTagData;
    }
}
