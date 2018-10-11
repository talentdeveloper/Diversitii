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
 * Defines the contents of the packs table. Packs are uniquely identified by COLUMN_PACK_ID.
 */
public class PacksDao extends DbDao {
    private static final String TAG = PacksDao.class.getName();

    private static final String TABLE_NAME = "packs";

    private static final String COLUMN_PACK_ID = "pack_id";
    private static final String COLUMN_CATEGORY_ID = "category_id";
    private static final String COLUMN_IS_OWNED = "is_owned";

    @Override
    void setTableName() {
        mTableName = TABLE_NAME;
    }

    @Override
    String createTable() {
        return "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_PACK_ID + " INTEGER NOT NULL," +
                COLUMN_CATEGORY_ID + " INTEGER," +
                COLUMN_IS_OWNED + " INTEGER NOT NULL);";
    }

    // True if pack identified by given parameters is saved in table.
    private boolean doesPackExist(SQLiteDatabase db, int packId) {
        boolean exists = false;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + COLUMN_PACK_ID +
                    " FROM " + getTableName() +
                    " WHERE " + COLUMN_PACK_ID + " = " + packId +
                    " LIMIT 1", null);
            if (cursor != null && cursor.moveToFirst()) {
                exists = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Querying packs");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return exists;
    }

    /**
     * Gets the ID of the given pack's category, or Constants.DEFAULT_CAT_ID if not found.
     *
     * @param context the calling context
     * @param packId  the pack id
     * @return category ID or Constants.DEFAULT_CAT_ID if no matching pack
     */
    public int getCategoryId(Context context, int packId) {
        int catId = Constants.DEFAULT_CAT_ID;
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_CATEGORY_ID
                        + " FROM " + getTableName()
                        + " WHERE " + COLUMN_PACK_ID + " = ?"
                        + " LIMIT 1", new String[]{String.valueOf(packId)});
                if (cursor != null && cursor.moveToFirst()) {
                    catId = cursor.getInt(cursor.getColumnIndex(COLUMN_CATEGORY_ID));
                } else {
                    Log.e(TAG, "No matching pack " + packId);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Getting category ID");
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
        return catId;
    }

    // Add packs from the database
    public void addPack(Context context, Pack pack) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                if (pack != null) {
                    ContentValues cv = new ContentValues();
                    cv.put(COLUMN_CATEGORY_ID, pack.getCategoryId());
                    cv.put(COLUMN_IS_OWNED, (pack.isOwned()) ? Constants.DB_TRUE : Constants.DB_FALSE);
                    if (doesPackExist(db, pack.getPackId())) {
                        // Pack ID exists, update values
                        Log.d(TAG, "Updating existing pack " + pack.getPackId());
                        db.update(getTableName(), cv,
                                COLUMN_PACK_ID + " = ?",
                                new String[]{pack.getPackId() + ""});
                    } else {
                        // Pack ID is new, insert values
                        cv.put(COLUMN_PACK_ID, pack.getPackId());
                        db.insert(TABLE_NAME, null, cv);
                    }
                } else {
                    Log.w(TAG, "Missing pack");
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
     * Marks given pack as owned, if it exists.
     *
     * @param context the calling context
     * @param packId  the pack ID
     */
    public void setIsOwned(Context context, int packId) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_IS_OWNED, Constants.DB_TRUE);
                if (doesPackExist(db, packId)) {
                    // Pack exists, update values
                    Log.d(TAG, "Now own existing pack " + packId);
                    db.update(getTableName(), cv, COLUMN_PACK_ID + " = ?",
                            new String[]{String.valueOf(packId)});
                } else {
                    // Pack is new, insert values
                    Log.d(TAG, "Now own new pack " + packId);
                    cv.put(COLUMN_PACK_ID, packId);
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
     * Marks all packs in the given category as owned, if any.
     *
     * @param context    the calling context
     * @param categoryId the category ID
     */
    public void setCategoryOwned(Context context, int categoryId) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_IS_OWNED, Constants.DB_TRUE);
                db.update(getTableName(), cv, COLUMN_CATEGORY_ID + " = ?",
                        new String[]{String.valueOf(categoryId)});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
    }

    public boolean getIsOwned(Context context, int packId) {
        boolean isOwned = false;
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_IS_OWNED
                        + " FROM " + getTableName()
                        + " WHERE " + COLUMN_PACK_ID + " = " + packId
                        + " LIMIT 1", null);
                if (cursor != null && cursor.moveToFirst()) {
                    isOwned = (cursor.getInt(cursor.getColumnIndex(COLUMN_IS_OWNED)) == Constants.DB_TRUE);
                } else {
                    Log.e(TAG, "No matching pack");
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Getting packs");
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
        return isOwned;
    }

    /**
     * Gets an array of category names for packs that are for sale, i.e. not already owned.
     *
     * @param context the calling context
     * @return array of category names
     */
    public ArrayList<String> getPacksForSale(Context context) {
        ArrayList<String> names = new ArrayList<>();
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_CATEGORY_ID
                                + " FROM " + getTableName()
                                + " WHERE " + COLUMN_IS_OWNED + " = " + Constants.DB_FALSE
                                + " AND " + COLUMN_CATEGORY_ID + " != " + Constants.DEFAULT_CAT_ID,
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        int catId = cursor.getInt(cursor.getColumnIndex(COLUMN_CATEGORY_ID));
                        String cat = new CategoryDao().getCategoryText(context, catId);
                        if (!names.contains(cat)) {
                            names.add(cat);
                        }
                        cursor.moveToNext();
                    }
                } else {
                    Log.d(TAG, "No packs for sale");
                }
            } catch (Exception e) {
                Log.e(TAG, "Getting packs");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
        return names;
    }

    /**
     * Gets an array of pack IDs for packs that are not already owned.
     *
     * @param context the calling context
     * @param limit   the number of packs to return
     * @return array of pack IDs
     */
    public int[] getUnboughtPacks(Context context, int limit) {
        int[] ids = new int[limit];
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_PACK_ID
                                + " FROM " + getTableName()
                                + " WHERE " + COLUMN_IS_OWNED + " = " + Constants.DB_FALSE
                                + " LIMIT " + limit,
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    for (int i = 0; i < limit && !cursor.isAfterLast(); ++i) {
                        ids[i] = cursor.getInt(cursor.getColumnIndex(COLUMN_PACK_ID));
                        cursor.moveToNext();
                    }
                } else {
                    Log.e(TAG, "No packs for sale");
                }
            } catch (Exception e) {
                Log.e(TAG, "Getting pack IDs");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
        return ids;
    }

    /**
     * Gets the total number of unbought packs with categories.
     *
     * @param context the calling context
     * @return the total number of unbought packs
     */
    public long getTotalUnboughtCatPacks(Context context) {
        return DatabaseUtils.queryNumEntries(new DbHelper(context).getReadableDatabase(),
                getTableName(), COLUMN_IS_OWNED + " = ? AND " + COLUMN_CATEGORY_ID + " != ?",
                new String[]{String.valueOf(Constants.DB_FALSE), String.valueOf(Constants.DEFAULT_CAT_ID)});
    }
}
