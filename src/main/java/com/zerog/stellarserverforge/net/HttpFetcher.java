package com.zerog.stellarserverforge.net;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;

/** Thin wrapper over {@link HttpClient} used by every service that fetches remote metadata/files. */
public class HttpFetcher {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String getString(String url) throws IOException, InterruptedException {
        return getString(url, Map.of());
    }

    public String getString(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET();
        headers.forEach(builder::header);
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + " fetching " + url);
        }
        return response.body();
    }

    public void downloadToFile(String url, Path destination) throws IOException, InterruptedException {
        downloadToFile(url, destination, Map.of());
    }

    /** Downloads to a sibling temp file first and only replaces {@code destination} once the
     * response is confirmed 2xx — a failed/short response (network blip, an error page from a
     * proxy, an expired link) can never corrupt a file that was already there. */
    public void downloadToFile(String url, Path destination, Map<String, String> headers) throws IOException, InterruptedException {
        Path tempFile = destination.resolveSibling(destination.getFileName() + "." + System.nanoTime() + ".part");
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(5))
                    .GET();
            headers.forEach(builder::header);
            HttpResponse<Path> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofFile(tempFile));
            if (response.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + response.statusCode() + " downloading " + url);
            }
            Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
