package com.technoready.top3.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.technoready.top3.model.AuthorSearchResponse;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Client with two modes:
 *  - Simulated: if baseUrl is blank, loads /sample-authors.json from resources.
 *  - Real: if baseUrl is provided, performs HTTP GET and parses JSON.
 */
public class ScholarApiClient {

    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper mapper;

    public ScholarApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        // Map snake_case JSON (author_id, cited_by) -> camelCase fields (authorId, citedBy)
        this.mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public AuthorSearchResponse searchAuthors(String query, int limit) throws ApiException {
        try {
            // ---- SIMULATION MODE: read local JSON if no base URL is configured ----
            if (baseUrl == null || baseUrl.isBlank()) {
                try (InputStream is = ScholarApiClient.class.getResourceAsStream("/sample-authors.json")) {
                    if (is == null) {
                        throw new ApiException("sample-authors.json not found in resources.", 0);
                    }
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    return mapper.readValue(body, AuthorSearchResponse.class);
                }
            }

            // ---- REAL MODE: call external API (e.g., SerpAPI) ----
            String q = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
            String url = baseUrl
                    + (baseUrl.contains("?") ? "&" : "?")
                    + "q=" + q
                    + "&limit=" + Math.max(1, limit)
                    + (apiKey != null && !apiKey.isBlank() ? "&api_key=" + apiKey : "");

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet get = new HttpGet(url);
                get.addHeader("Accept", "application/json");

                ClassicHttpResponse resp = (ClassicHttpResponse) client.execute(get);
                int status = resp.getCode();
                String body = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity()) : "";
                EntityUtils.consumeQuietly(resp.getEntity());

                if (status < 200 || status >= 300) {
                    throw new ApiException("API returned status " + status + " with body: " + body, status);
                }

                // NOTE: Si usas SerpAPI, su JSON de perfiles es distinto (profiles[]).
                // Para Sprint 2 puedes quedarte con el modo simulado.
                // En Sprint 3 puedes agregar un mapper específico para ese proveedor.
                return mapper.readValue(body, AuthorSearchResponse.class);
            }

        } catch (IOException e) {
            throw new ApiException("Network/IO error: " + e.getMessage(), 0);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Unexpected error: " + e.getMessage(), 0);
        }
    }
}

