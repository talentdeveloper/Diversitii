package com.diversitii.dcapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.diversitii.dcapp.Constants;

import java.util.ArrayList;

/**
 * Defines the contents of the categories table. Categories are uniquely identified by
 * COLUMN_CATEGORY_ID.
 */
public final class CategoryDao extends DbDao {
    private static final String TAG = CategoryDao.class.getName();

    static final String TABLE_NAME = "categories";

    static final String COLUMN_CATEGORY_ID = "category_id";
    private static final String COLUMN_CATEGORY_TEXT = "category_text";
    private static final String COLUMN_CATEGORY_ICON = "category_icon";
    private static final String COLUMN_IS_OWNED = "is_owned";
    static final String COLUMN_IS_ENABLED = "is_enabled";

    @Override
    void setTableName() {
        mTableName = TABLE_NAME;
    }

    @Override
    String createTable() {
        return "CREATE TABLE " + CategoryDao.TABLE_NAME + " (" +
                CategoryDao._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                CategoryDao.COLUMN_CATEGORY_ID + " INTEGER NOT NULL," +
                CategoryDao.COLUMN_CATEGORY_TEXT + " TEXT," +
                CategoryDao.COLUMN_CATEGORY_ICON + " TEXT," +
                CategoryDao.COLUMN_IS_OWNED + " INTEGER," +
                CategoryDao.COLUMN_IS_ENABLED + " INTEGER NOT NULL);";
    }

    // True if category identified by given parameters is saved in table.
    private boolean doesCategoryExist(SQLiteDatabase db, String categoryId) {
        boolean exists = false;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + COLUMN_CATEGORY_ID +
                    " FROM " + getTableName() +
                    " WHERE " + COLUMN_CATEGORY_ID + " = " + categoryId +
                    " LIMIT 1", null);
            if (cursor != null && cursor.moveToFirst()) {
                exists = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Querying categories");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return exists;
    }

    /**
     * Marks given category as enabled/disabled, if it exists.
     *
     * @param context   the calling context
     * @param isEnabled the COLUMN_IS_ENABLED value
     */
    public void setIsEnabled(Context context, String categoryName, boolean isEnabled) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_IS_ENABLED, isEnabled);
                ArrayList<Integer> catIds = getCategoryIdByName(db, categoryName);
                if (catIds != null) {
                    for (int catId : catIds) {
                        db.update(getTableName(), cv,
                                COLUMN_CATEGORY_ID + " = ?",
                                new String[]{String.valueOf(catId)});
                        db.setTransactionSuccessful();
                    }
                } else {
                    Log.e(TAG, "No category " + categoryName + " found");
                }
            } finally {
                db.endTransaction();
            }
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
    }

    /**
     * Gets the IDs of all categories with the given name.
     *
     * @param db           the database
     * @param categoryName the category name
     * @return array of category IDs or null if no matches found
     */
    private ArrayList<Integer> getCategoryIdByName(SQLiteDatabase db, String categoryName) {
        ArrayList<Integer> categoryIds = null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + COLUMN_CATEGORY_ID +
                            " FROM " + getTableName() +
                            " WHERE " + COLUMN_CATEGORY_TEXT + " = \"" + categoryName + "\"",
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                categoryIds = new ArrayList<>();
                while (!cursor.isAfterLast()) {
                    categoryIds.add(cursor.getInt(cursor.getColumnIndex(COLUMN_CATEGORY_ID)));
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Querying categories");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return categoryIds;
    }

    /**
     * Gets the ID of the first category with the given name.
     *
     * @param context      the calling context
     * @param categoryName the category name
     * @return the category ID or Constants.DEFAULT_CAT_ID if no matches found
     */
    public int getCategoryId(Context context, String categoryName) {
        int catId = Constants.DEFAULT_CAT_ID;
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            catId = getCategoryIdByName(db, categoryName).get(0);
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
        return catId;
    }

    /**
     * Marks given category as owned.
     *
     * @param context    the calling context
     * @param categoryId the category ID
     */
    public void setIsOwned(Context context, int categoryId) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_IS_OWNED, Constants.DB_TRUE);
                if (doesCategoryExist(db, String.valueOf(categoryId))) {
                    // Category exists, update values
                    Log.d(TAG, "Now own existing category " + categoryId);
                    db.update(getTableName(), cv, COLUMN_CATEGORY_ID + " = ?", new String[]{String.valueOf(categoryId)});
                } else {
                    // Category is new, insert values
                    Log.d(TAG, "Now own new category " + categoryId);
                    cv.put(COLUMN_CATEGORY_ID, categoryId);
                    cv.put(COLUMN_IS_ENABLED, Constants.DB_TRUE);
                    db.insert(TABLE_NAME, null, cv);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
    }

    /**
     * Gets the text of the given category, or null if not found.
     *
     * @param context    the calling context
     * @param categoryId the category ID
     * @return category text null if no matching category
     */
    public String getCategoryText(Context context, int categoryId) {
        String text = null;
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_CATEGORY_TEXT
                        + " FROM " + getTableName()
                        + " WHERE " + COLUMN_CATEGORY_ID + " = " + categoryId
                        + " LIMIT 1", null);
                if (cursor != null && cursor.moveToFirst()) {
                    text = cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_TEXT));
                } else {
                    Log.e(TAG, "No matching category " + categoryId);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Getting category text");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.endTransaction();
            }
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
        return text;
    }

    /**
     * Gets the total number of owned categories stored.
     *
     * @param context the calling context
     * @return the total number of categories
     */
    public long getTotalOwnedCategories(Context context) {
        return DatabaseUtils.queryNumEntries(new DbHelper(context).getReadableDatabase(),
                getTableName(), COLUMN_IS_OWNED + " = ?", new String[]{String.valueOf(Constants.DB_TRUE)});
    }

    /**
     * Gets the total number of owned enabled categories stored.
     *
     * @param context the calling context
     * @return the total number of categories
     */
    public long getTotalOwnedEnabledCategories(Context context) {
        return DatabaseUtils.queryNumEntries(new DbHelper(context).getReadableDatabase(),
                getTableName(), COLUMN_IS_OWNED + " = ? AND " + COLUMN_IS_ENABLED + " = ?",
                new String[]{String.valueOf(Constants.DB_TRUE), String.valueOf(Constants.DB_TRUE)});
    }

    /**
     * Gets all owned categories.
     *
     * @param context the calling context
     * @return the category objects
     */
    public ArrayList<Category> getOwnedCategories(Context context) {
        ArrayList<Category> categories = new ArrayList<>();
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_CATEGORY_TEXT + "," + COLUMN_IS_ENABLED
                        + " FROM " + getTableName()
                        + " WHERE " + COLUMN_IS_OWNED + " = " + Constants.DB_TRUE
                        + " ORDER BY " + COLUMN_CATEGORY_TEXT, null);
                if (cursor != null && cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        categories.add(new Category(cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_TEXT)),
                                cursor.getInt(cursor.getColumnIndex(COLUMN_IS_ENABLED))));
                        cursor.moveToNext();
                    }
                } else {
                    Log.d(TAG, "No enabled categories");
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Getting categories");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.endTransaction();
            }
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
        return categories;
    }

    /**
     * Returns true if given category has an icon.
     *
     * @param context the calling context
     * @return true if given category has an icon
     */
    public boolean hasIcon(Context context, int categoryId) {
        boolean hasIcon = false;
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_CATEGORY_ICON
                        + " FROM " + getTableName()
                        + " WHERE " + COLUMN_CATEGORY_ID + " = ?"
                        + " LIMIT 1", new String[]{String.valueOf(categoryId)});
                if (cursor != null && cursor.moveToFirst()) {
                    String icon = cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_ICON));
                    hasIcon = (icon != null) && (!icon.startsWith(Constants.ICON_FILE_PREFIX));
                } else {
                    Log.d(TAG, "No matching category");
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Checking category icon");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                db.endTransaction();
            }
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
        return hasIcon;
    }

    // Add categories from the database
    public void addCategory(Context context, Category category) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                if (category != null) {
                    ContentValues cv = new ContentValues();
                    if (category.getCategoryText() != null) {
                        cv.put(COLUMN_CATEGORY_TEXT, category.getCategoryText());
                    }
                    if (category.getIcon() != null) {
                        cv.put(COLUMN_CATEGORY_ICON, category.getIcon());
                    }
                    cv.put(COLUMN_IS_ENABLED, Constants.DB_TRUE);
                    if (doesCategoryExist(db, String.valueOf(category.getCategoryId()))) {
                        // Category exists, update values
                        Log.d(TAG, "Updating existing category " + category.getCategoryId());
                        db.update(getTableName(), cv,
                                COLUMN_CATEGORY_ID + " = ?",
                                new String[]{String.valueOf(category.getCategoryId())});
                    } else {
                        // Category is new, insert values
//                        Log.d(TAG, "Inserting category " + category.getCategoryId());
                        cv.put(COLUMN_CATEGORY_ID, category.getCategoryId());
                        cv.put(COLUMN_IS_OWNED, Constants.DB_FALSE); // DO NOT OVERWRITE!
                        db.insert(TABLE_NAME, null, cv);
                    }
                } else {
                    Log.w(TAG, "Missing category");
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
    }
}
