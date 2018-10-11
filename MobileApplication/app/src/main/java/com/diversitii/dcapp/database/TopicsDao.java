package com.diversitii.dcapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.diversitii.dcapp.Constants;

/**
 * Defines the contents of the topics table. Topics are uniquely identified by COLUMN_TOPIC_ID +
 * COLUMN_IS_USER_TOPIC.
 */
public final class TopicsDao extends DbDao {
    private static final String TAG = TopicsDao.class.getName();

    private static final String TABLE_NAME = "topics";

    private static final String COLUMN_TOPIC_ID = "topic_id";
    private static final String COLUMN_CAT_ID = "cat_id";
    private static final String COLUMN_TOPIC_NAME = "topic_name";
    private static final String COLUMN_IS_DELETED = "is_deleted";
    private static final String COLUMN_IS_USER_TOPIC = "is_user_topic";


    @Override
    void setTableName() {
        mTableName = TABLE_NAME;
    }

    @Override
    String createTable() {
        return "CREATE TABLE " + TopicsDao.TABLE_NAME + " (" +
                TopicsDao._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                TopicsDao.COLUMN_TOPIC_ID + " INTEGER NOT NULL," +
                TopicsDao.COLUMN_CAT_ID + " INTEGER NOT NULL," +
                TopicsDao.COLUMN_TOPIC_NAME + " TEXT NOT NULL," +
                TopicsDao.COLUMN_IS_DELETED + " INTEGER NOT NULL," +
                TopicsDao.COLUMN_IS_USER_TOPIC + " INTEGER NOT NULL);";
    }

//    @Override
//    boolean fillData(Context context) {
//        try {
//            BufferedReader br = new BufferedReader(
//                    new InputStreamReader(context.getAssets().open(Constants.TOPICS_FILE)));
//            StringBuilder file = new StringBuilder();
//            String line;
//            while ((line = br.readLine()) != null) {
//                file.append(line).append('\n');
//            }
//            return parseJson(context, file.toString(), Constants.FREE_PACK_ID);
//        } catch (IOException e) {
//            Log.e(TAG, "Reading topics file: " + e.toString());
//        }
//        return false;
//    }

//    @Override
//    public boolean parseJson(Context context, String jsonFile, int packNumber) {
//        boolean success = false;
//        try {
//            JSONArray json = new JSONArray(jsonFile);
//            DbHelper dbHelper = new DbHelper(context);
//            final SQLiteDatabase db = dbHelper.getWritableDatabase();
//            if (db != null) {
//                db.beginTransaction();
//                try {
//                    for (int i = 0; i < json.length(); ++i) {
//                        ContentValues topic = new ContentValues();
//                        String topicId = json.getJSONObject(i).get(JSON_TOPIC_ID).toString();
//                        String topicName = json.getJSONObject(i).get(JSON_TOPIC).toString();
//                        topic.put(COLUMN_TOPIC_PACK, packNumber);
//                        topic.put(COLUMN_TOPIC_NAME, topicName);
//                        topic.put(COLUMN_IS_USER_TOPIC, Constants.DB_FALSE);
//                        if (doesTopicExist(db, Integer.valueOf(topicId))) {
//                            // Topic ID exists, update values (don't overwrite deletion settings)
//                            db.update(getTableName(), topic,
//                                    COLUMN_TOPIC_ID + " = ?",
//                                    new String[]{topicId});
//                        } else {
//                            // Topic ID is new, insert values
//                            topic.put(COLUMN_TOPIC_ID, topicId);
//                            topic.put(COLUMN_IS_DELETED, Constants.DB_FALSE);
//                            db.insert(TABLE_NAME, null, topic);
//                        }
//                    }
//                    db.setTransactionSuccessful();
//                    success = true;
//                } finally {
//                    db.endTransaction();
//                }
//            } else {
//                Log.e(TAG, "Null database");
//            }
//            dbHelper.close();
//        } catch (JSONException e) {
//            Log.e(TAG, "Error reading topics json");
//        }
//        return success;
//    }

    // True if non-user topic identified by given parameters is saved in table.
    private boolean doesTopicExist(SQLiteDatabase db, int topicId) {
        boolean exists = false;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + COLUMN_TOPIC_ID +
                    " FROM " + getTableName() +
                    " WHERE " + COLUMN_TOPIC_ID + " = " + topicId +
                    " AND " + COLUMN_IS_USER_TOPIC + " = " + Constants.DB_FALSE +
                    " LIMIT 1", null);
            if (cursor != null && cursor.moveToFirst()) {
                exists = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Querying topics");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return exists;
    }

    /**
     * Gets an array of Constants.NUM_TOPIC_CHOICES random non-deleted topics from the database.
     *
     * @param context   the calling context
     * @param numTopics the number of topics to return
     * @return the topic objects
     */
    public Topic[] getRandomTopics(Context context, int numTopics) {
        Topic[] topics = new Topic[numTopics];
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_TOPIC_ID + ", " + COLUMN_CAT_ID + ", " + COLUMN_TOPIC_NAME + ", " + COLUMN_IS_USER_TOPIC +
                        " FROM " + getTableName() + " this_table INNER JOIN " + CategoryDao.TABLE_NAME + " cat_table " +
                        " ON this_table." + COLUMN_CAT_ID + " = cat_table." + CategoryDao.COLUMN_CATEGORY_ID +
                        " WHERE this_table." + COLUMN_IS_DELETED + " = " + Constants.DB_FALSE +
                        " AND cat_table." + CategoryDao.COLUMN_IS_ENABLED + " = " + Constants.DB_TRUE +
                        " ORDER BY RANDOM()" +
                        " LIMIT " + numTopics, null);
                if (cursor != null && cursor.moveToFirst()) {
                    for (int i = 0; i < numTopics && !cursor.isAfterLast(); ++i) {
                        topics[i] = new Topic(
                                cursor.getInt(cursor.getColumnIndex(COLUMN_TOPIC_ID)),
                                cursor.getInt(cursor.getColumnIndex(COLUMN_CAT_ID)),
                                cursor.getString(cursor.getColumnIndex(COLUMN_TOPIC_NAME)),
                                cursor.getInt(cursor.getColumnIndex(COLUMN_IS_USER_TOPIC)));
                        cursor.moveToNext();
                    }
                } else {
                    Log.e(TAG, "No matching topics");
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Getting topics");
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
        return topics;
    }

    /**
     * Gets the total number of topics that do not have categories.
     *
     * @param context the calling context
     * @return the total number of topics without categories
     */
    public long getTotalRandomTopics(Context context) {
        return DatabaseUtils.queryNumEntries(new DbHelper(context).getReadableDatabase(),
                getTableName(), COLUMN_CAT_ID + " = ? AND " + COLUMN_IS_USER_TOPIC + " = ?",
                new String[]{String.valueOf(Constants.DEFAULT_CAT_ID), String.valueOf(Constants.DB_FALSE)});
    }

    // Add packs from the database
    public void addTopic(Context context, Topic topic) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                if (topic != null) {
                    ContentValues cv = new ContentValues();
                    cv.put(COLUMN_TOPIC_NAME, topic.getTopicName());
                    cv.put(COLUMN_IS_USER_TOPIC, topic.getIsUserTopic());
                    cv.put(COLUMN_CAT_ID, topic.getCatId());
                    if (doesTopicExist(db, topic.getTopicId())) {
                        // Topic ID exists, update values (don't overwrite deletion settings)
                        Log.d(TAG, "Updating existing topic " + topic.getTopicId());
                        db.update(getTableName(), cv,
                                COLUMN_TOPIC_ID + " = ? AND " + COLUMN_IS_USER_TOPIC + " = ?",
                                new String[]{String.valueOf(topic.getTopicId()), String.valueOf(Constants.DB_FALSE)});
                    } else {
                        // Topic ID is new, insert values
                        cv.put(COLUMN_TOPIC_ID, topic.getTopicId());
                        cv.put(COLUMN_IS_DELETED, Constants.DB_FALSE);
                        db.insert(TABLE_NAME, null, cv);
                    }
                } else {
                    Log.w(TAG, "Missing topic");
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

    public void addCustomTopic(Context context, Topic topic) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_TOPIC_ID, topic.getTopicId());
                cv.put(COLUMN_CAT_ID, topic.getCatId());
                cv.put(COLUMN_TOPIC_NAME, topic.getTopicName());
                cv.put(COLUMN_IS_DELETED, Constants.DB_FALSE);
                cv.put(COLUMN_IS_USER_TOPIC, Constants.DB_TRUE);
                db.insert(TABLE_NAME, null, cv);

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
     * Marks given topic as deleted, if it exists.
     *
     * @param context   the calling context
     * @param topicName the COLUMN_TOPIC_NAME value
     * @return true if topic was marked as deleted
     */
    public boolean deleteTopic(Context context, String topicName) {
        boolean success = false;
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                ContentValues topic = new ContentValues();
                topic.put(COLUMN_IS_DELETED, Constants.DB_TRUE);
                int topicId = getTopicByName(db, topicName);
                if (topicId > 0) {
                    db.update(getTableName(), topic,
                            COLUMN_TOPIC_ID + " = ?",
                            new String[]{String.valueOf(topicId)});
                    db.setTransactionSuccessful();
                    success = true;
                } // else no topic found
            } finally {
                db.endTransaction();
            }
        } else {
            Log.e(TAG, "Null database");
        }
        dbHelper.close();
        return success;
    }

    /**
     * Gets at most Constants.TOPIC_DROP_DOWN_SZ non-deleted topics matching the given topic name
     * prompt.
     *
     * @param searchText the first letters of the topic names to match (all topics match null)
     * @return first Constants.TOPIC_DROP_DOWN_SZ matches
     */
    public String[] getMatches(Context context, String searchText) {
        String[] matches = null;
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            String queryString = "SELECT " + COLUMN_TOPIC_NAME + " FROM " + TABLE_NAME +
                    " WHERE " + COLUMN_IS_DELETED + " = " + Constants.DB_FALSE;
            String params[] = null;
            if (searchText != null) {
                queryString += " AND " + COLUMN_TOPIC_NAME + " LIKE ?";
                params = new String[]{searchText.trim() + "%"};
            }
            queryString += " LIMIT " + Constants.TOPIC_DROP_DOWN_SZ;
            try {
                cursor = db.rawQuery(queryString, params);
                if (cursor != null && cursor.moveToFirst()) {
                    matches = new String[cursor.getCount()];
                    for (int i = 0; i < matches.length && !cursor.isAfterLast(); ++i) {
                        matches[i] = cursor.getString(cursor.getColumnIndex(COLUMN_TOPIC_NAME));
                        cursor.moveToNext();
                    }
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Getting matching topics");
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
        return matches;
    }

    /**
     * Gets the ID of at most one non-deleted topic with the given name.
     *
     * @param db        the database
     * @param topicName the topic name
     * @return the topic ID or -1 if no matches found
     */
    private int getTopicByName(SQLiteDatabase db, String topicName) {
        int topicId = -1;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + COLUMN_TOPIC_ID +
                    " FROM " + getTableName() +
                    " WHERE " + COLUMN_TOPIC_NAME + " = \"" + topicName + "\" AND " + COLUMN_IS_DELETED + " = " + Constants.DB_FALSE +
                    " LIMIT 1", null);
            if (cursor != null && cursor.moveToFirst()) {
                topicId = cursor.getInt(cursor.getColumnIndex(COLUMN_TOPIC_ID));
            }
        } catch (Exception e) {
            Log.e(TAG, "Querying topics");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return topicId;
    }

    /**
     * Returns true if given non-user-topic is found.
     */
    public boolean topicExists(Context context, int topicId) {
        boolean exists = false;
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_TOPIC_ID + " FROM " + getTableName() +
                        " WHERE " + COLUMN_TOPIC_ID + " = " + topicId +
                        " AND " + COLUMN_IS_USER_TOPIC + " = " + Constants.DB_FALSE +
                        " LIMIT 1", null);
                if (cursor != null && cursor.moveToFirst()) {
                    exists = true;
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Getting matching topics");
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
        return exists;
    }
}
