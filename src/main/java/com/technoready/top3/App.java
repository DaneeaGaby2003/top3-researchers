package com.technoready.top3;

import java.util.Scanner;
import com.technoready.top3.controller.AuthorController;
import com.technoready.top3.service.ScholarApiClient;
import com.technoready.top3.view.ConsoleAuthorView;

public class App {
    public static void main(String[] args) throws Exception {
        String base = System.getenv().getOrDefault(
            "SERPAPI_BASE",
            "https://serpapi.com/search.json?engine=google_scholar_profiles"
        );
        String key  = System.getenv("SERPAPI_KEY"); // Tu API key

        var client = new ScholarApiClient(base, key);
        var view   = new ConsoleAuthorView();
        var ctrl   = new AuthorController(client, view);

        System.out.println("Top-3 Researchers – Google Scholar (MVC)");
        System.out.println(base.isBlank()
                ? "Mode: SIMULATED (/sample-authors.json)"
                : "Mode: LIVE (SerpAPI google_scholar_profiles)");
        System.out.println("Type a name to search (or 'exit'):");

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String q = sc.nextLine();
                if ("exit".equalsIgnoreCase(q)) break;
                ctrl.onSearchTop3(q);
            }
        }
        System.out.println("Bye!");
    }
}
