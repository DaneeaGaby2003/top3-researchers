package com.technoready.top3.view;

import java.util.List;
import com.technoready.top3.model.Author;

public interface AuthorView {
    void showResults(List<Author> authors);
    void showMessage(String message);
    void showError(String message, Throwable t);
}
