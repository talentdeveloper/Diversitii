package com.diversitii.dcapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.diversitii.dcapp.Constants;

/**
 * Defines the contents of the questions table. Questions are uniquely identified by COLUMN_TOPIC_ID
 * + COLUMN_QUESTION_ID + COLUMN_IS_USER_TOPIC.
 */
public final class QuestionsDao extends DbDao {
    private static final String TAG = QuestionsDao.class.getName();

    private static final String TABLE_NAME = "questions";

    private static final String COLUMN_QUESTION_ID = "question_id";
    private static final String COLUMN_TOPIC_ID = "topic_id";
    private static final String COLUMN_ANSWER_ID = "answer_id";
    private static final String COLUMN_QUESTIONS_TEXT = "questions_text";
    private static final String COLUMN_QUESTIONS_POINTS = "questions_points";
    private static final String COLUMN_SUBTOPIC_ID = "subtopic_id";
    private static final String COLUMN_IS_USER_TOPIC = "is_user_topic";

//    // Packs JSON
//    private static final String JSON_QUESTION_ID = "QuestionId";
//    private static final String JSON_TOPIC_ID = "TopicId";
//    private static final String JSON_ANSWER_ID = "AnswerId";
//    private static final String JSON_QUESTIONS = "Questions";
//    private static final String JSON_POINTS = "PointsId";

    @Override
    void setTableName() {
        mTableName = TABLE_NAME;
    }

    @Override
    String createTable() {
        return "CREATE TABLE " + QuestionsDao.TABLE_NAME + " (" +
                QuestionsDao._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                QuestionsDao.COLUMN_QUESTION_ID + " INTEGER NOT NULL," +
                QuestionsDao.COLUMN_TOPIC_ID + " INTEGER NOT NULL," +
                QuestionsDao.COLUMN_ANSWER_ID + " INTEGER NOT NULL," +
                QuestionsDao.COLUMN_QUESTIONS_TEXT + " TEXT NOT NULL," +
                QuestionsDao.COLUMN_QUESTIONS_POINTS + " INTEGER NOT NULL," +
                QuestionsDao.COLUMN_SUBTOPIC_ID + " INTEGER NOT NULL," +
                QuestionsDao.COLUMN_IS_USER_TOPIC + " INTEGER NOT NULL);";
    }

//    @Override
//    boolean fillData(Context context) {
//        try {
//            BufferedReader br = new BufferedReader(
//                    new InputStreamReader(context.getAssets().open(Constants.QUESTIONS_FILE)));
//            StringBuilder file = new StringBuilder();
//            String line;
//            while ((line = br.readLine()) != null) {
//                file.append(line).append('\n');
//            }
//            return parseJson(context, file.toString(), Constants.FREE_PACK_ID);
//        } catch (IOException e) {
//            Log.e(TAG, "Reading questions file: " + e.toString());
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
//                        ContentValues question = new ContentValues();
//                        JSONObject jsonObj = json.getJSONObject(i);
//                        String topicId = jsonObj.get(JSON_TOPIC_ID).toString();
//                        String questionId = jsonObj.get(JSON_QUESTION_ID).toString();
//                        question.put(COLUMN_QUESTIONS_TEXT, jsonObj.get(JSON_QUESTIONS).toString());
//                        String points = (jsonObj.has(JSON_POINTS)) ? jsonObj.get(JSON_POINTS).toString() : String.valueOf(Constants.DEFAULT_QUESTION_POINTS);
//                        question.put(COLUMN_QUESTIONS_POINTS, points);
//                        question.put(COLUMN_ANSWER_ID, jsonObj.get(JSON_ANSWER_ID).toString());
//                        question.put(COLUMN_IS_USER_TOPIC, Constants.DB_FALSE);
//                        if (doesQuestionExist(db, Integer.valueOf(topicId), questionId)) {
//                            // Question exists, update values
//                            db.update(getTableName(), question,
//                                    COLUMN_TOPIC_ID + " = ? AND " + COLUMN_QUESTION_ID + " = ?",
//                                    new String[]{topicId, questionId});
//                        } else {
//                            // Question is new, insert values
//                            question.put(COLUMN_QUESTION_ID, questionId);
//                            question.put(COLUMN_TOPIC_ID, topicId);
//                            db.insert(TABLE_NAME, null, question);
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
//            Log.e(TAG, "Error reading questions json");
//        }
//        return success;
//    }

    // True if non-user question identified by given parameters is saved in table.
    private boolean doesQuestionExist(SQLiteDatabase db, int topicId, String questionId) {
        boolean exists = false;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + COLUMN_TOPIC_ID +
                    " FROM " + getTableName() +
                    " WHERE " + COLUMN_TOPIC_ID + " = " + topicId +
                    " AND " + COLUMN_QUESTION_ID + " = " + questionId +
                    " AND " + COLUMN_IS_USER_TOPIC + " = " + Constants.DB_FALSE +
                    " LIMIT 1", null);
            if (cursor != null && cursor.moveToFirst()) {
                exists = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Querying questions");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return exists;
    }

    /**
     * Gets an array of Constants.QUESTIONS_PER_TOPIC questions from the database, for the given
     * topic ID.
     *
     * @param context the calling context
     * @param topicId the topic ID
     * @return the question objects
     */
    public Question[] getTopicQuestions(Context context, int topicId, int isUserTopic) {
        Question[] questions = new Question[Constants.QUESTIONS_PER_TOPIC];
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_QUESTIONS_TEXT + ", " + COLUMN_ANSWER_ID + ", " + COLUMN_QUESTIONS_POINTS
                        + " FROM " + getTableName()
                        + " WHERE " + COLUMN_TOPIC_ID + " = " + topicId
                        + " AND " + COLUMN_IS_USER_TOPIC + " = " + isUserTopic
                        + " ORDER BY RANDOM() LIMIT " + Constants.QUESTIONS_PER_TOPIC, null);
                if (cursor != null && cursor.moveToFirst()) {
                    for (int i = 0; i < Constants.QUESTIONS_PER_TOPIC && !cursor.isAfterLast(); ++i) {
                        questions[i] = new Question(
                                cursor.getString(cursor.getColumnIndex(COLUMN_QUESTIONS_TEXT)),
                                cursor.getInt(cursor.getColumnIndex(COLUMN_ANSWER_ID)),
                                cursor.getInt(cursor.getColumnIndex(COLUMN_QUESTIONS_POINTS)));
                        cursor.moveToNext();
                    }
                } else {
                    Log.e(TAG, "No matching questions");
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Getting questions");
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
        return questions;
    }

    /**
     * Gets the subtopic ID of the given question, or Constants.DEFAULT_SUBTOPIC_ID if not found.
     *
     * @param context    the calling context
     * @param questionId the question ID
     * @param topicId    the topic ID
     * @return the subtopic ID
     */
    public int getSubtopicId(Context context, int questionId, int topicId) {
        int subtopicId = Constants.DEFAULT_SUBTOPIC_ID;
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db != null) {
            db.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT " + COLUMN_SUBTOPIC_ID
                        + " FROM " + getTableName()
                        + " WHERE " + COLUMN_TOPIC_ID + " = " + topicId
                        + " AND " + COLUMN_QUESTION_ID + " = " + questionId
                        + " AND " + COLUMN_IS_USER_TOPIC + " = " + Constants.DB_FALSE
                        + " LIMIT 1", null);
                if (cursor != null && cursor.moveToFirst()) {
                    subtopicId = cursor.getInt(cursor.getColumnIndex(COLUMN_SUBTOPIC_ID));
                } else {
                    Log.e(TAG, "No matching question");
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Getting question");
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
        return subtopicId;
    }

    // Add packs from the database
    public void addQuestion(Context context, Question question) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                if (question != null) {
                    ContentValues cv = new ContentValues();
                    cv.put(COLUMN_QUESTIONS_TEXT, question.getQuestionText());
                    cv.put(COLUMN_ANSWER_ID, question.getAnswerId());
                    cv.put(COLUMN_QUESTIONS_POINTS, question.getPoints());
                    cv.put(COLUMN_IS_USER_TOPIC, Constants.DB_FALSE);
                    cv.put(COLUMN_SUBTOPIC_ID, question.getSubtopicId());
                    if (doesQuestionExist(db, question.getTopicId(), question.getQuestionId() + "")) {
                        // Question exists, update values
                        Log.d(TAG, "Updating existing question " + question.getQuestionId() + " for topic " + question.getTopicId());
                        db.update(getTableName(), cv,
                                COLUMN_TOPIC_ID + " = ? AND " + COLUMN_QUESTION_ID + " = ? AND " + COLUMN_IS_USER_TOPIC + " = ?",
                                new String[]{question.getTopicId() + "", question.getQuestionId() + "", String.valueOf(Constants.DB_FALSE)});
                    } else {
                        // Question is new, insert values
                        cv.put(COLUMN_QUESTION_ID, question.getQuestionId());
                        cv.put(COLUMN_TOPIC_ID, question.getTopicId());
                        db.insert(TABLE_NAME, null, cv);
                    }
                } else {
                    Log.w(TAG, "Missing question");
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

    public void addCustomQuestion(Context context, Question question, int questionId, int topicId) {
        DbHelper dbHelper = new DbHelper(context);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db != null) {
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_QUESTION_ID, questionId);
                cv.put(COLUMN_TOPIC_ID, topicId);
                cv.put(COLUMN_ANSWER_ID, question.getAnswerId());
                cv.put(COLUMN_QUESTIONS_TEXT, question.getQuestionText());
                cv.put(COLUMN_QUESTIONS_POINTS, question.getPoints());
                cv.put(COLUMN_SUBTOPIC_ID, question.getSubtopicId());
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
