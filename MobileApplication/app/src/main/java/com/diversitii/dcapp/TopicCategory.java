package com.diversitii.dcapp;

import java.io.File;

/**
 * Topic category object.
 */
class TopicCategory {
    private int mPackId;
    private String mSku;
    private String mCategoryName = "Loading...";
    private File mIcon;

    TopicCategory(String sku, int packId) {
        this.mSku = sku;
        this.mPackId = packId;
    }

    int getPackId() {
        return mPackId;
    }

    String getSku() {
        return mSku;
    }

    String getCategoryName() {
        return mCategoryName;
    }

    void setCategoryName(String name) {
        mCategoryName = name;
    }

    File getIcon() {
        return mIcon;
    }

    void setIcon(File icon) {
        mIcon = icon;
    }
}
