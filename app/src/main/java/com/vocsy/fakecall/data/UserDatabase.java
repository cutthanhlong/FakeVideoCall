package com.vocsy.fakecall.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;


import com.vocsy.fakecall.model.UserModel;

import java.util.ArrayList;
import java.util.List;

public class UserDatabase extends SQLiteOpenHelper {


    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "USER_DATABASE";
    public static final String USER_TABLE = "USER_TABLE";

    public static final String USER_COL_1 = "ID";
    public static final String USER_COL_2 = "USER_NAME";
    public static final String USER_COL_3 = "USER_PHONE_NUMBER";
    public static final String USER_COL_4 = "USER_PHOTO";
    public static final String USER_COL_5 = "USER_VIDEO";
    public static final String USER_COL_6 = "USER_TYPE";
    public static final String USER_COL_7 = "USER_EMAIL";
    public static final String USER_COL_8 = "USER_AUDIO";
    public static final String USER_COL_9 = "USER_FAVOURITE";
    public static final String USER_COL_10 = "USER_AVB";

    public UserDatabase(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + USER_TABLE + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, USER_NAME TEXT, USER_PHONE_NUMBER TEXT, USER_PHOTO TEXT, USER_VIDEO TEXT, USER_TYPE TEXT, USER_EMAIL TEXT,USER_AUDIO TEXT,USER_FAVOURITE TEXT,USER_AVB TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insertUSER(String name, String phone_Number, String photo_path, String video, String type, String email, String audio,String favourite,String avb) {
        Log.e("TAG", "insertUSER: insert data" );
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(USER_COL_2, name);
        values.put(USER_COL_3, phone_Number);
        values.put(USER_COL_4, photo_path);
        values.put(USER_COL_5, video);
        values.put(USER_COL_6, type);
        values.put(USER_COL_7, email);
        values.put(USER_COL_8, audio);
        values.put(USER_COL_9, favourite);
        values.put(USER_COL_10, avb);
        database.insert(USER_TABLE, null, values);
        database.close();
    }

    public List<UserModel> retriveData() {
        Log.e("TAG", "insertUSER: retrive data" );
        List<UserModel> modelsList = new ArrayList<>();

        String selectQuery = "Select * from " + USER_TABLE;

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                UserModel data = new UserModel();
                data.setId(Integer.parseInt(cursor.getString(0)));
                data.setName(cursor.getString(1));
                data.setPhonenumber(cursor.getString(2));
                data.setPhoto(cursor.getString(3));
                data.setVideo(cursor.getString(4));
                data.setType(cursor.getString(5));
                data.setEmail(cursor.getString(6));
                data.setAudio(cursor.getString(7));
                data.setFavourite(cursor.getString(8));
                data.setAvb(cursor.getString(9));
                modelsList.add(data);
            } while (cursor.moveToNext());
        }

        return modelsList;
    }

    public void updateFavorite(String id, String favorite) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(USER_COL_9, favorite);
        database.update(USER_TABLE, values, "ID =?", new String[]{id});
        database.close();
    }
    public void updateUserDetails(String id, String name, String phone_Number, String photo_path, String video, String type, String email, String audio,String avb) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(USER_COL_2, name);
        values.put(USER_COL_3, phone_Number);
        values.put(USER_COL_4, photo_path);
        values.put(USER_COL_5, video);
        values.put(USER_COL_6, type);
        values.put(USER_COL_7, email);
        values.put(USER_COL_8, audio);
        values.put(USER_COL_10, avb);
        database.update(USER_TABLE, values, "ID =?", new String[]{id});
    }

    public int deleteData(String id) {
        SQLiteDatabase database = getWritableDatabase();
        int i = database.delete(USER_TABLE, "ID = ?", new String[]{id});
        database.close();
        return i;
    }
}
