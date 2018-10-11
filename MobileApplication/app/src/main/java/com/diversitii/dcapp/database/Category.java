package com.diversitii.dcapp.database;

import com.diversitii.dcapp.Constants;

/**
 * Object model for category data.
 */
public class Category {
    private int mCategoryId;
    private String mCategoryText;
    private String mIcon;
    private boolean mIsEnabled;

    public Category(int categoryId, String categoryText, String icon) {
        this.mCategoryId = categoryId;
        this.mCategoryText = categoryText;
        this.mIcon = icon;
    }

    Category(String categoryText, int isEnabled) {
        this.mCategoryText = categoryText;
        this.mIsEnabled = (isEnabled == Constants.DB_TRUE);
    }

    int getCategoryId() {
        return mCategoryId;
    }

    public String getCategoryText() {
        return mCategoryText;
    }

    public String getIcon() {
        return mIcon;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }
}
