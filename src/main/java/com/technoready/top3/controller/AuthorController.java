package com.technoready.top3.controller;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.technoready.top3.model.Author;
import com.technoready.top3.model.AuthorSearchResponse;
import com.technoready.top3.service.ApiException;
import com.technoready.top3.service.ScholarApiClient;
import com.technoready.top3.view.AuthorView;

public class AuthorController {
    private final ScholarApiClient apiClient;
    private final AuthorView view;

    public AuthorController(ScholarApiClient apiClient, AuthorView view) {
        this.apiClient = apiClient;
        this.view = view;
    }

    public void onSearchTop3(String query) {
        if (query == null || query.isBlank()) {
            view.showMessage("Please enter a non-empty query.");
            return;
        }
        try {
            AuthorSearchResponse res = apiClient.searchAuthors(query, 20);
            List<Author> authors = res.getAuthors() == null ? List.of() : res.getAuthors();

            List<Author> top3 = authors.stream()
                    .sorted(Comparator.comparing(
                            (Author a) -> a.getCitedBy() == null ? 0 : a.getCitedBy()
                    ).reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            view.showResults(top3);
        } catch (ApiException e) {
            view.showError("Failed to fetch authors (status " + e.getStatusCode() + ")", e);
        }
    }
}
