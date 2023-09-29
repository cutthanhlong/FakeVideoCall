package com.vocsy.fakecall.newFakeCall;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CallHistoryHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "HISTORY_DATABASE";
    public static final String TABLE_NAME = "CALL_HISTORY";

    public static final String COL_1 = "ID";
    public static final String COL_2 = "NAME";
    public static final String COL_3 = "MOBILE_NUMBER";
    public static final String COL_4 = "DATE";
    public static final String COL_5 = "TIME";
    public static final String COL_6 = "AV";
    public static final String COL_7 = "IS_MISCALL";

    public CallHistoryHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME TEXT, MOBILE_NUMBER TEXT, DATE TEXT, TIME TEXT, AV INTEGER, IS_MISCALL INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insertData(String name, String mobile_number, String date, String time, int av, int is_miscall) {

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_2, name);
        values.put(COL_3, mobile_number);
        values.put(COL_4, date);
        values.put(COL_5, time);
        values.put(COL_6, av);
        values.put(COL_7, is_miscall);
        database.insert(TABLE_NAME, null, values);
        database.close();

    }

    public List<HistoryModels> retriveData() {
        List<HistoryModels> modelsList = new ArrayList<>();

        String selectQuery = "Select * from " + TABLE_NAME;

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                HistoryModels data = new HistoryModels();
                data.setId(Integer.parseInt(cursor.getString(0)));
                data.setName(cursor.getString(1));
                data.setMobile_number(cursor.getString(2));
                data.setDate(cursor.getString(3));
                data.setTime(cursor.getString(4));
                data.setAv(Integer.parseInt(cursor.getString(5)));
                data.setIs_Miscall(Integer.parseInt(cursor.getString(6)));
                modelsList.add(data);
            } while (cursor.moveToNext());
        }

        return modelsList;
    }

    public int deleteData(String id) {
        SQLiteDatabase database = getWritableDatabase();
        int i = database.delete(TABLE_NAME, "ID = ?", new String[]{id});
        database.close();
        return i;
    }
}
