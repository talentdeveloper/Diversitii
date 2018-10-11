package com.diversitii.dcapp.database;

/**
 * Object model for topic data.
 */
public class Topic {
    private int mTopicId;
    private int mCatId;
    private String mTopicName;
    private int mIsUserTopic;

    public Topic(int topicId, int catId, String topicName, int isUserTopic) {
        this.mTopicId = topicId;
        this.mCatId = catId;
        this.mTopicName = topicName;
        this.mIsUserTopic = isUserTopic;
    }

    public int getTopicId() {
        return mTopicId;
    }

    int getCatId() {
        return mCatId;
    }

    public String getTopicName() {
        return mTopicName;
    }

    public int getIsUserTopic() {
        return mIsUserTopic;
    }
}
