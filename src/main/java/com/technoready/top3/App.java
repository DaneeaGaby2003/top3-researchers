package com.technoready.top3;

import java.util.Scanner;
import com.technoready.top3.controller.AuthorController;
import com.technoready.top3.service.ScholarApiClient;
import com.technoready.top3.view.ConsoleAuthorView;

public class App {
    public static void main(String[] args) {
        String base = System.getenv().getOrDefault(
            "SCHOLAR_API_BASE",
            "https://example.com/scholar/author/search");
        String key  = System.getenv().getOrDefault("SCHOLAR_API_KEY", "");

        var client = new ScholarApiClient(base, key);
        var view   = new ConsoleAuthorView();
        var ctrl   = new AuthorController(client, view);

        System.out.println("Top-3 Researchers – Google Scholar (MVC)");
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
