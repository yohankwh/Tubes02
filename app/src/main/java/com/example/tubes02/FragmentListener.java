package com.example.tubes02;

public interface FragmentListener {
    void changePage(int page);
    void closeApplication();
    void updateHighScore(int val);
    void loadHighScore();
}
