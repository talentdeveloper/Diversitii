package com.diversitii.dcapp.database;

/**
 * Object model for pack data.
 */
public class Pack {
    private int mPackId;
    private int mCategoryId;
    private boolean mIsOwned;

    public Pack(int packId, int categoryId, boolean isOwned) {
        this.mPackId = packId;
        this.mCategoryId = categoryId;
        this.mIsOwned = isOwned;
    }

    int getPackId() {
        return mPackId;
    }

    int getCategoryId() {
        return mCategoryId;
    }

    boolean isOwned() {
        return mIsOwned;
    }
}
