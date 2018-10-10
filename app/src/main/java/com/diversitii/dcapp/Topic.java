package com.diversitii.dcapp;

/**
 * Object model for topic data.
 */
public class Topic {
    private String text;
    private int categoryId;

    Topic(String text, int categoryId) {
        this.text = text;
        this.categoryId = categoryId;
    }

    // DO NOT CHANGE BELOW--needed for class serialization

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
}
