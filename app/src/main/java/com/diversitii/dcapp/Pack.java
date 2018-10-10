package com.diversitii.dcapp;

/**
 * Object model for category data.
 */
public class Pack {
    private boolean doPromote;
    private int categoryId;
    private String topicIds;


    Pack(boolean doPromote, int categoryId, String topicIds) {
        this.doPromote = doPromote;
        this.categoryId = categoryId;
        this.topicIds = topicIds;
    }

    // DO NOT CHANGE BELOW--needed for class serialization

    public boolean getDoPromote() {
        return doPromote;
    }

    public void setDoPromote(boolean doPromote) {
        this.doPromote = doPromote;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getTopicIds() {
        return topicIds;
    }

    public void setTopicIds(String topicIds) {
        this.topicIds = topicIds;
    }
}
