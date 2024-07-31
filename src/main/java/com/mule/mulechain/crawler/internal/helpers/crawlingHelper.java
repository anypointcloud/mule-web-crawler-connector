package com.mule.mulechain.crawler.internal.helpers;
import java.io.IOException;

import org.jsoup.Jsoup;  
import org.jsoup.nodes.Document;  

public class crawlingHelper {
    
    private static String getTitle(String url, String outputFolder) throws IOException{
        Document doc = connectUrlGetDocument(url);  
        String title = doc.title();  
        //System.out.println("title is: " + title);     
        return title;
    }

    private static Document connectUrlGetDocument(String url) throws IOException {
        return Jsoup.connect(url).get();
    }
}
