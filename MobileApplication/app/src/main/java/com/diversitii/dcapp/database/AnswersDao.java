package com.diversitii.dcapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.diversitii.dcapp.Constants;

/**
 * Defines the contents of the answers table. Answers are uniquely identified by COLUMN_TOPIC_ID +
 * COLUMN_ANSWER_ID + COLUMN_IS_USER_TOPIC.
 */
public final class AnswersDao extends DbDao {
    private static final String TAG = AnswersDao.class.getName();

    private static final String TABLE_NAME = "answers";

    private static final String COLUMN_ANSWER_ID = "answer_id";
    private static final String COLUMN_TOPIC_ID = "topic_id";
    private static final String COLUMN_ANSWER_TEXT = "answer_text";
    private static final String COLUMN_IS_USER_TOPIC = "is_user_topic";

//    // Packs JSON
//    private static final String JSON_ANSWER_ID = "AnswerId";
//    private static final String JSON_TOPIC_ID = "TopicId";
//    private static final String JSON_ANSWER_TEXT = "Answers";

    @Override
    void setTableName() {
        mTableName = TABLE_NAME;
    }

    @Override
    String createTable() {
        return "CREATE TABLE " + AnswersDao.TABLE_NAME + " (" +
                AnswersDao._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                AnswersDao.COLUMN_ANSWER_ID + " INTEGER NOT NULL," +
                AnswersDao.COLUMN_TOPIC_ID + " INTEGER NOT NULL," +
                AnswersDao.COLUMN_ANSWER_TEXT + " TEXT NOT NULL," +
                AnswersDao.COLUMN_IS_USER_TOPIC + " INTEGER NOT NULL);";
    }

//    @Override
//    boolean fillData(Context context) {
//        try {
//            BufferedReader br = new BufferedReader(
//                    new InputStreamReader(context.getAssets().open(Constants.ANSWERS_FILE)));
//            StringBuilder file = new StringBuilder();
//            String line;
//            while ((line = br.readLine()) != null) {
//                file.append(line).append('\n');
//            }
//            return parseJson(context, file.toString(), Constants.FREE_PACK_ID);
//        } catch (IOException e) {
//            Log.e(TAG, "Reading answers file: " + e.toString());
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
//                        ContentValues answer = new ContentValues();
//                        String topicId = json.getJSONObject(i).get(JSON_TOPIC_ID).toString();
//                        String answerId = json.getJSONObject(i).get(JSON_ANSWER_ID).toString();
//                        String answerText = json.getJSONObject(i).get(JSON_ANSWER_TEXT).toString();
//                        answer.put(COLUMN_ANSWER_TEXT, answerText);
//                        answer.put(COLUMN_IS_USER_TOPIC, Constants.DB_FALSE);
//                        if (doesAnswerExist(db, Integer.valueOf(topicId), answerId)) {
//                            // Answer exists, update values
//                            db.update(getTableName(), answer,
//                                    COLUMN_TOPIC_ID + " = ? AND " + COLUMN_ANSWER_ID + " = ?",
//                                    new String[]{topicId, answerId});
//                        } else {
//                            // Answer is new, insert values
//                            answer.put(COLUMN_ANSWER_ID, answerId);
//                            answer.put(COLUMN_TOPIC_ID, topicId);
//                            db.insert(TABLE_NAME, null, answer);
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
//            Log.e(TAG, "Error reading answers json");
//        }
//        return success;
//    }

    // True if non-user answer identified by given parameters is saved in table.
    private boolean doesAnswerExist(SQLiteDatabase db, int topicId, String answerId) {
        boolean exists = false;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + COLUMN_TOPIC_ID +
                    " FROM " + getTableName() +
                    " WHERE " + COLUMN_TOPIC_ID + " = " + topicId +
                    " AND " + COLUMN_ANSWER_ID + " = " + answerId +
                    " AND " + COLUMN_IS_USER_TOPIC + " = " + Constants.DB_FALSE +
                    " LIMIT 1", null);
            if (cursor != null && cursor.moveToFirst()) {
                exists = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Querying answers");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return exists;
    }

    /**
     * Gets an array of at most Constants.MAX_ANSWERS answers from the database, in a random order,
     * for the given topic ID.
     *
     * @param context the calling context
     * @param topicId the topic ID
     * @return the answer objects
     */
    public Answer[] getTopicAnswers(Context context, int topicId, int isUserTopic) {
        Answer[] answers = new Answer[Constants.MAX_ANSWERS];
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_ANSWER_ID + ", " + COLUMN_ANSWER_TEXT
                        + " FROM " + getTableName()
                        + " WHERE " + COLUMN_TOPIC_ID + " = " + topicId
                        + " AND " + COLUMN_IS_USER_TOPIC + " = " + isUserTopic
                        + " ORDER BY RANDOM()"
                        + " LIMIT " + Constants.MAX_ANSWERS, null);
                if (cursor != null && cursor.moveToFirst()) {
                    for (int i = 0; i < Constants.MAX_ANSWERS; ++i) {
                        if (!cursor.isAfterLast()) {
                            answers[i] = new Answer(cursor.getInt(cursor.getColumnIndex(COLUMN_ANSWER_ID)),
                                    cursor.getString(cursor.getColumnIndex(COLUMN_ANSWER_TEXT)));
                            cursor.moveToNext();
                        } else {
                            Log.e(TAG, "Incorrectly formatted answer, topic ID " + topicId);
                            answers[i] = new Answer(0, "");
                        }
                    }
                } else {
                    Log.e(TAG, "No matching answers");
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Getting answers");
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
        return answers;
    }

    // Add packs from the database
    public void addAnswer(Context context, Answer answer) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                if (answer != null) {
                    ContentValues cv = new ContentValues();
                    cv.put(COLUMN_ANSWER_TEXT, answer.getAnswerText());
                    cv.put(COLUMN_IS_USER_TOPIC, Constants.DB_FALSE);
                    if (doesAnswerExist(db, answer.getTopicId(), String.valueOf(answer.getAnswerId()))) {
                        // Answer exists, update values
                        Log.d(TAG, "Updating existing answer " + answer.getAnswerId() + " for topic " + answer.getTopicId());
                        db.update(getTableName(), cv,
                                COLUMN_TOPIC_ID + " = ? AND " + COLUMN_ANSWER_ID + " = ? AND " + COLUMN_IS_USER_TOPIC + " = ?",
                                new String[]{answer.getTopicId() + "", String.valueOf(answer.getAnswerId()), String.valueOf(Constants.DB_FALSE)});
                    } else {
                        // Answer is new, insert values
                        cv.put(COLUMN_ANSWER_ID, answer.getAnswerId());
                        cv.put(COLUMN_TOPIC_ID, answer.getTopicId());
                        db.insert(TABLE_NAME, null, cv);
                    }
                } else {
                    Log.w(TAG, "Missing answer");
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

    public void addCustomAnswer(Context context, Answer answer, int topicId) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_ANSWER_ID, answer.getAnswerId());
                cv.put(COLUMN_TOPIC_ID, topicId);
                cv.put(COLUMN_ANSWER_TEXT, answer.getAnswerText());
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
}
