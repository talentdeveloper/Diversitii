package com.diversitii.dcapp.database;

import android.provider.BaseColumns;

/**
 * Base class for database classes.
 */
abstract class DbDao implements BaseColumns {
    String mTableName = "";

    abstract void setTableName();

    String getTableName() {
        if (this.mTableName.isEmpty()) {
            this.setTableName();
        }
        return this.mTableName;
    }

    abstract String createTable();

//    abstract boolean fillData(Context context);

//    public abstract boolean parseJson(Context context, String jsonFile, int packNumber);
}
