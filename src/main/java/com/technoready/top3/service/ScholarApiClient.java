package com.technoready.top3.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.technoready.top3.model.AuthorSearchResponse;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import org.apache.hc.core5.http.ClassicHttpResponse;       // 👈 usa la interfaz de HttpCore5
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ScholarApiClient {
    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper mapper;

    public ScholarApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public AuthorSearchResponse searchAuthors(String query, int limit) throws ApiException {
        String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = baseUrl
                + (baseUrl.contains("?") ? "&" : "?")
                + "q=" + q
                + (apiKey != null && !apiKey.isBlank() ? "&api_key=" + apiKey : "")
                + "&limit=" + Math.max(1, limit);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept", "application/json");

            ClassicHttpResponse resp = (ClassicHttpResponse) client.execute(get); // 👈 sin CloseableHttpResponse
            int status = resp.getCode();
            String body = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity()) : "";
            EntityUtils.consumeQuietly(resp.getEntity());

            if (status < 200 || status >= 300) {
                throw new ApiException("API returned status " + status + " with body: " + body, status);
            }
            return mapper.readValue(body, AuthorSearchResponse.class);

        } catch (IOException e) {
            throw new ApiException("Network/IO error: " + e.getMessage(), 0);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Unexpected error: " + e.getMessage(), 0);
        }
    }
}
