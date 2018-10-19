package com.diversitii.dcapp;

/**
 * Object model for subtopic data.
 */
public class Subtopic {
    private String text;

    Subtopic(String text) {
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

//