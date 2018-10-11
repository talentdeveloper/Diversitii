package com.diversitii.dcapp.database;

/**
 * Object model for question data.
 */
public class Question {
    private int mQuestionId;
    private int mTopicId;
    private String mQuestionText;
    private int mAnswerId;
    private int mPoints;
    private int mSubtopicId;

    public Question(int questionId, int topicId, String questionText, int answerId, int points, int subtopicId) {
        this.mQuestionId = questionId;
        this.mTopicId = topicId;
        this.mQuestionText = questionText;
        this.mAnswerId = answerId;
        this.mPoints = points;
        this.mSubtopicId = subtopicId;
    }

    public Question(String questionText, int answerId, int points) {
        this.mQuestionText = questionText;
        this.mAnswerId = answerId;
        this.mPoints = points;
    }

    int getQuestionId() {
        return mQuestionId;
    }

    int getTopicId() {
        return mTopicId;
    }

    public String getQuestionText() {
        return mQuestionText;
    }

    public int getAnswerId() {
        return mAnswerId;
    }

    public int getPoints() {
        return mPoints;
    }

    int getSubtopicId() {
        return mSubtopicId;
    }
}
