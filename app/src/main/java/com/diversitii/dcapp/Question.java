package com.diversitii.dcapp;

/**
 * Object model for question data.
 */
public class Question {
    private int questionId;
    private int topicId;
    private String text;
    private int subtopicId;
    private int points;
    private int correctAnswerId;

    Question(int questionId, int topicId, String text, int subtopicId, int points, int correctAnswerId) {
        this.questionId = questionId;
        this.topicId = topicId;
        this.text = text;
        this.subtopicId = subtopicId;
        this.points = points;
        this.correctAnswerId = correctAnswerId;
    }

    // DO NOT CHANGE BELOW--needed for class serialization

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getSubtopicId() {
        return subtopicId;
    }

    public void setSubtopicId(int subtopicId) {
        this.subtopicId = subtopicId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getCorrectAnswerId() {
        return correctAnswerId;
    }

    public void setCorrectAnswerId(int correctAnswerId) {
        this.correctAnswerId = correctAnswerId;
    }
}

//testing for tortoise git