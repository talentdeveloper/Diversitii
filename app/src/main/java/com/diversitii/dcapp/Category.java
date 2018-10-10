package com.diversitii.dcapp;

/**
 * Object model for category data.
 */
public class Category {
    private String text;

    Category(String text) {
        this.text = text;
    }

    // DO NOT CHANGE BELOW--needed for class serialization

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
