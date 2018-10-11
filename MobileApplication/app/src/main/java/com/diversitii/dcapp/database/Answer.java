package com.diversitii.dcapp.database;

/**
 * Object model for answer data.
 */
public class Answer {
    private int mAnswerId;
    private int mTopicId;
    private String mAnswerText;

    public Answer(int answerId, int topicId, String answerText) {
        this.mAnswerId = answerId;
        this.mTopicId = topicId;
        this.mAnswerText = answerText;
    }

    public Answer(int answerId, String answerText) {
        this.mAnswerId = answerId;
        this.mAnswerText = answerText;
    }

    public int getAnswerId() {
        return mAnswerId;
    }

    int getTopicId() {
        return mTopicId;
    }

    public String getAnswerText() {
        return mAnswerText;
    }
}
