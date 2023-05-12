package com.example.mobilerakenduss;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView; //отоброжает список на Android
    TextView noMusicTextView; // отоброжает текст
    ArrayList<AudioModel> songsList = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        noMusicTextView = findViewById(R.id.no_songs_text);

        if(checkPermission() == false){
            requestPermission();
            return;
        }
        // с 40 по 60 строку это для того чтобы получить данные аудиофайлов на телефон
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC +" != 0";

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projection,selection,null,null); //ContentResolver это взаимодействие с провайдером контента
        while(cursor.moveToNext()){
            AudioModel songData = new AudioModel(cursor.getString(2),cursor.getString(0),cursor.getString(1),cursor.getString(3));
            if(new File(songData.getPath()).exists())
                songsList.add(songData);
        }

        if(songsList.size()==0){
            noMusicTextView.setVisibility(View.VISIBLE);
        }else{
            //recyclerview
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new AudioListAdapter(songsList,getApplicationContext()));
        }

    }

    boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(result == PackageManager.PERMISSION_GRANTED){
            return true;
        }else{
            return false;
        }
    }

    void requestPermission(){//Возвращает или задает, как проверяются разрешения в издателе перед передачей изменений
        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            Toast.makeText(MainActivity.this,"LUGEMISEKS VAJALIK LUBA, LUBA SEADESTUSEST",Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},123);

        }else
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},123);
    }

    @Override
    protected void onResume() { //запускает воспроизведение анимации, аудио
        super.onResume();
        if(recyclerView!=null){
            recyclerView.setAdapter(new AudioListAdapter(songsList,getApplicationContext()));
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, save the file

                // с 40 по 60 строку это для того чтобы получить данные аудиофайлов на телефон
                String[] projection = {
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DURATION
                };

                String selection = MediaStore.Audio.Media.IS_MUSIC +" != 0";

                Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projection,selection,null,null); //ContentResolver это взаимодействие с провайдером контента
                while(cursor.moveToNext()){
                    AudioModel songData = new AudioModel(cursor.getString(2),cursor.getString(0),cursor.getString(1),cursor.getString(3));
                    if(new File(songData.getPath()).exists())
                        songsList.add(songData);
                }

                if(songsList.size()==0){
                    noMusicTextView.setVisibility(View.VISIBLE);
                }else{
                    //recyclerview
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(new AudioListAdapter(songsList,getApplicationContext()));
                }

            } else {
                // Разрешение отклонено, показать сообщение пользователю
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}