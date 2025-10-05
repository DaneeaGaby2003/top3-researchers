package com.technoready.top3.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.technoready.top3.model.Author;
import com.technoready.top3.model.AuthorSearchResponse;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Client with two modes:
 *  - Simulated: if baseUrl is blank, loads /sample-authors.json from resources.
 *  - Real: if baseUrl is provided, performs HTTP GET (SerpAPI) and adapts JSON to AuthorSearchResponse.
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
                    AuthorSearchResponse full = mapper.readValue(is, AuthorSearchResponse.class);
                    trimToLimit(full, limit);
                    return full;
                }
            }

            // ---- REAL MODE: call external API (SerpAPI u OpenAlex) ----
            String safeBase = (baseUrl == null ? "" : baseUrl.trim());
            String safeKey  = (apiKey  == null ? "" : apiKey.trim());

            URI uri;
            if (safeBase.contains("openalex.org")) {
                // OpenAlex: /authors?search=...&per-page=...
                uri = new URIBuilder(safeBase)
                        .addParameter("search", query == null ? "" : query)
                        .addParameter("per-page", String.valueOf(Math.max(1, limit)))
                        .build();
            } else {
                // SerpAPI (por si más adelante usas otro engine válido)
                uri = new URIBuilder(safeBase)
                        .addParameter("q", query == null ? "" : query)
                        .addParameter("num", String.valueOf(Math.max(1, limit)))
                        .addParameter("api_key", safeKey)
                        .build();
            }

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet get = new HttpGet(uri);
                get.addHeader("Accept", "application/json");

                ClassicHttpResponse resp = (ClassicHttpResponse) client.execute(get);
                int status = resp.getCode();
                String body = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity()) : "";
                EntityUtils.consumeQuietly(resp.getEntity());

                if (status < 200 || status >= 300) {
                    // Fallback automático si el proveedor está caído o descontinuado
                    if (status == 400 && body != null && body.toLowerCase().contains("discontinued")) {
                        try (InputStream is = ScholarApiClient.class.getResourceAsStream("/sample-authors.json")) {
                            if (is == null) throw new ApiException("sample-authors.json not found in resources.", 0);
                            AuthorSearchResponse full = mapper.readValue(is, AuthorSearchResponse.class);
                            trimToLimit(full, limit);
                            return full;
                        }
                    }
                    throw new ApiException("API returned status " + status + " with body: " + body, status);
                }

                // Detecta y adapta OpenAlex
                if (safeBase.contains("openalex.org") || body.contains("\"results\"")) {
                    AuthorSearchResponse adapted = adaptOpenAlexAuthors(body);
                    trimToLimit(adapted, limit);
                    return adapted;
                }

                // Detecta SerpAPI (profiles[])
                if (body.contains("\"profiles\"")) {
                    AuthorSearchResponse adapted = adaptSerpApiProfiles(body);
                    trimToLimit(adapted, limit);
                    return adapted;
                }

                // Fallback: intenta tu formato directo (authors[])
                AuthorSearchResponse direct = mapper.readValue(body, AuthorSearchResponse.class);
                trimToLimit(direct, limit);
                return direct;
            }

        } catch (IOException e) {
            throw new ApiException("Network/IO error: " + e.getMessage(), 0);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Unexpected error: " + e.getMessage(), 0);
        }
    }

    /** Recorta la lista a 'limit' si viene más larga (comporta igual en sim y real). */
    private static void trimToLimit(AuthorSearchResponse resp, int limit) {
        if (resp == null || resp.getAuthors() == null) return;
        int n = Math.max(1, limit);
        if (resp.getAuthors().size() > n) {
            resp.setAuthors(resp.getAuthors().subList(0, n));
        }
    }

    /** Adaptador: SerpAPI (profiles[]) -> AuthorSearchResponse (authors[]). */
    private AuthorSearchResponse adaptSerpApiProfiles(String body) throws Exception {
        var root = mapper.readTree(body);
        var out = new AuthorSearchResponse();
        List<Author> authors = new ArrayList<>();

        if (root.has("profiles") && root.get("profiles").isArray()) {
            for (var node : root.get("profiles")) {
                Author a = new Author();

                // SerpAPI no trae un author_id estable; usa el link como id derivado.
                String link = node.hasNonNull("link") ? node.get("link").asText() : null;
                a.setAuthorId(link);

                a.setName(node.hasNonNull("name") ? node.get("name").asText() : null);
                a.setAffiliation(node.hasNonNull("affiliations") ? node.get("affiliations").asText() : null);

                int cited = 0;
                if (node.has("cited_by")) {
                    var cb = node.get("cited_by");
                    if (cb.has("total")) cited = cb.get("total").asInt(0);
                    else if (cb.isInt()) cited = cb.asInt(0);
                }
                a.setCitedBy(cited);

                List<String> interests = new ArrayList<>();
                if (node.has("interests") && node.get("interests").isArray()) {
                    for (var i : node.get("interests")) interests.add(i.asText());
                }
                a.setInterests(interests);

                authors.add(a);
            }
        }

        out.setAuthors(authors);
        return out;
    }

    /** Adaptador: OpenAlex (results[]) -> AuthorSearchResponse (authors[]). */
    private AuthorSearchResponse adaptOpenAlexAuthors(String body) throws Exception {
        var root = mapper.readTree(body);
        var out = new AuthorSearchResponse();
        List<Author> authors = new ArrayList<>();

        if (root.has("results") && root.get("results").isArray()) {
            for (var node : root.get("results")) {
                Author a = new Author();
                a.setAuthorId(node.hasNonNull("id") ? node.get("id").asText() : null);
                a.setName(node.hasNonNull("display_name") ? node.get("display_name").asText() : null);

                // afiliación (si disponible)
                String aff = null;
                if (node.has("last_known_institution") && node.get("last_known_institution").hasNonNull("display_name")) {
                    aff = node.get("last_known_institution").get("display_name").asText();
                }
                a.setAffiliation(aff);

                // conteo de citas
                int cited = node.hasNonNull("cited_by_count") ? node.get("cited_by_count").asInt(0) : 0;
                a.setCitedBy(cited);

                // intereses desde x_concepts (top 5)
                List<String> interests = new ArrayList<>();
                if (node.has("x_concepts") && node.get("x_concepts").isArray()) {
                    int i = 0;
                    for (var c : node.get("x_concepts")) {
                        if (c.hasNonNull("display_name")) interests.add(c.get("display_name").asText());
                        if (++i >= 5) break;
                    }
                }
                a.setInterests(interests);

                authors.add(a);
            }
        }

        out.setAuthors(authors);
        return out;
    }
}
