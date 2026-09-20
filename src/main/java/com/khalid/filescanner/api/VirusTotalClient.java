package com.khalid.filescanner.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.khalid.filescanner.util.AppException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Week 7: calls a real REST API (VirusTotal v3) and parses the JSON response.
 * Only the SHA-256 hash of the file is sent, never the file itself.
 */
public final class VirusTotalClient {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private VirusTotalClient() {
    }

    public static String lookupHash(String sha256, String apiKey) throws AppException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException("Enter your VirusTotal API key first (or set VT_API_KEY).");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.virustotal.com/api/v3/files/" + sha256))
                .header("x-apikey", apiKey.trim())
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 200) {
                return parseStats(response.body());
            }
            if (status == 404) {
                return "Not found on VirusTotal (this exact file was never submitted).";
            }
            if (status == 401) {
                throw new AppException("Invalid API key.");
            }
            if (status == 429) {
                throw new AppException("Rate limit reached (free keys allow about 4 lookups per minute).");
            }
            throw new AppException("VirusTotal returned HTTP " + status);
        } catch (IOException e) {
            throw new AppException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Request interrupted.", e);
        }
    }

    private static String parseStats(String body) throws AppException {
        try {
            JsonObject stats = JsonParser.parseString(body).getAsJsonObject()
                    .getAsJsonObject("data")
                    .getAsJsonObject("attributes")
                    .getAsJsonObject("last_analysis_stats");
            return String.format("Malicious: %d | Suspicious: %d | Harmless: %d | Undetected: %d",
                    stats.get("malicious").getAsInt(), stats.get("suspicious").getAsInt(),
                    stats.get("harmless").getAsInt(), stats.get("undetected").getAsInt());
        } catch (RuntimeException e) {
            throw new AppException("Unexpected response format from VirusTotal.", e);
        }
    }
}
