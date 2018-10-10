package com.diversitii.dcapp;

/**
 * Object model for answer data.
 */
public class Answer {
    private int answerId;
    private String text;
    private int topicId;

    Answer(int answerId, String answerText, int topicId) {
        this.answerId = answerId;
        this.text = answerText;
        this.topicId = topicId;
    }

    // DO NOT CHANGE BELOW--needed for class serialization

    public int getAnswerId() {
        return answerId;
    }

    public void setAnswerId(int answerId) {
        this.answerId = answerId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }
}
