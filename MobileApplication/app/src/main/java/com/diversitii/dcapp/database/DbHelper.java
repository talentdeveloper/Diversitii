package com.diversitii.dcapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Manages the local database.
 */
public class DbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "diversitii.db";
    private static final int DATABASE_VERSION = 1;

    DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

//    /**
//     * Adds default data to database, if it has not already been added.
//     */
//    public static boolean initDatabase(Context context) {
//        return new TopicsDao().fillData(context)
//                && new QuestionsDao().fillData(context)
//                && new AnswersDao().fillData(context);
//    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(new TopicsDao().createTable());
        sqLiteDatabase.execSQL(new QuestionsDao().createTable());
        sqLiteDatabase.execSQL(new AnswersDao().createTable());
        sqLiteDatabase.execSQL(new PacksDao().createTable());
        sqLiteDatabase.execSQL(new CategoryDao().createTable());
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    }
}
