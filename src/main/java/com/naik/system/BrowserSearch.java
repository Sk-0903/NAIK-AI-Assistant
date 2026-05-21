package com.naik.system;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BrowserSearch {
    public boolean search(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return browse("https://www.google.com/search?q=" + encodedQuery);
    }

    public boolean openBestMatch(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return browse("https://www.google.com/search?btnI=1&q=" + encodedQuery);
    }

    public boolean searchYouTube(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return browse("https://www.youtube.com/results?search_query=" + encodedQuery);
    }

    public boolean searchWikipedia(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return browse("https://en.wikipedia.org/wiki/Special:Search?search=" + encodedQuery);
    }

    public boolean openWebsite(String website) {
        String trimmedWebsite = website.trim();
        if (trimmedWebsite.isBlank()) {
            return false;
        }

        if (!trimmedWebsite.startsWith("http://") && !trimmedWebsite.startsWith("https://")) {
            trimmedWebsite = "https://" + trimmedWebsite;
        }

        return browse(trimmedWebsite);
    }

    private boolean browse(String target) {
        URI uri = URI.create(target);

        if (!Desktop.isDesktopSupported()) {
            return false;
        }

        try {
            Desktop.getDesktop().browse(uri);
            return true;
        } catch (IOException error) {
            return false;
        }
    }
}
