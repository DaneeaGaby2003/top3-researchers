package com.technoready.top3.view;

import java.util.List;
import com.technoready.top3.model.Author;

public class ConsoleAuthorView implements AuthorView {
    @Override
    public void showResults(List<Author> authors) {
        if (authors == null || authors.isEmpty()) {
            System.out.println("No authors found.");
            return;
        }
        System.out.println("=== Top Results ===");
        for (Author a : authors) {
            System.out.printf("- %s | %s | citations: %s%n",
                safe(a.getName()), safe(a.getAffiliation()),
                a.getCitedBy() == null ? "0" : a.getCitedBy().toString());
            if (a.getInterests() != null && !a.getInterests().isEmpty()) {
                System.out.println("  Interests: " + String.join(", ", a.getInterests()));
            }
        }
    }
    @Override public void showMessage(String message) { System.out.println(message); }
    @Override public void showError(String message, Throwable t) {
        System.err.println("[ERROR] " + message);
        if (t != null && t.getMessage()!=null) System.err.println("Cause: " + t.getMessage());
    }
    private String safe(String s) { return s == null ? "" : s; }
}
