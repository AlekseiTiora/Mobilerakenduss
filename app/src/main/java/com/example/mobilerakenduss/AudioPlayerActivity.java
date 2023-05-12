package com.example.mobilerakenduss;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeRequest;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognizeRequest;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1p1beta1.SpeechSettings;
import com.google.protobuf.ByteString;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;


public class AudioPlayerActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1001;
    TextView titleTv,currentTimeTv,totalTimeTv,lyricsText;
    SeekBar seekBar;
    ProgressBar loadingSpinner;
    ImageView pausePlay,nextBtn,previousBtn,musicIcon,lyricsButton,saveLyrics;
    ArrayList<AudioModel> songsList;
    AudioModel currentSong;
    Context mainContext;
    AudioEncoder audioEncoder;
    MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
    int x=0;

    public AudioPlayerActivity() throws UnsupportedEncodingException {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) { //создаем  titleTv,pausePlay,seekBar и т.д для дальнейшего использования
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);
        mainContext=this;
        titleTv = findViewById(R.id.song_title);
        currentTimeTv = findViewById(R.id.current_time);
        totalTimeTv = findViewById(R.id.total_time);
        seekBar = findViewById(R.id.seek_bar);
        pausePlay = findViewById(R.id.pause_play);
        nextBtn = findViewById(R.id.next);
        previousBtn = findViewById(R.id.previous);
        musicIcon = findViewById(R.id.music_icon_big);
        //Объявление кнопки текста
        lyricsText=findViewById(R.id.lyricsText);
        lyricsButton = findViewById(R.id.lyrics);
        saveLyrics = findViewById(R.id.save_lyrics);
        titleTv.setSelected(true);
        loadingSpinner = findViewById(R.id.loading_spinner);
        songsList = (ArrayList<AudioModel>) getIntent().getSerializableExtra("LIST");
        setResourcesWithMusic();


        lyricsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertAudioToFlac(0,(Activity) mainContext,mainContext,currentSong.title);
            }
        });
        saveLyrics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("CurrentSongTitle---", currentSong.title);
                convertAudioToFlac(1,(Activity) mainContext,mainContext,currentSong.title);


            }
        });
        AudioPlayerActivity.this.runOnUiThread(new Runnable() {
            @Override // для того что чтобы запустить либо остановить музыку
            public void run() {
                if(mediaPlayer!=null){
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    currentTimeTv.setText(convertToMMSS(mediaPlayer.getCurrentPosition()+""));

                    if(mediaPlayer.isPlaying()){
                        pausePlay.setImageResource(R.drawable.baseline_pause_24);
                        musicIcon.setRotation(x++);
                    }else{
                        pausePlay.setImageResource(R.drawable.baseline_play_arrow_24);
                        musicIcon.setRotation(0);
                    }

                }
                new Handler().postDelayed(this,100);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mediaPlayer!=null && fromUser){
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    void setResourcesWithMusic(){ // запускает музыку
        currentSong = songsList.get(MyMediaPlayer.currentIndex);
        titleTv.setText(currentSong.getTitle());
        totalTimeTv.setText(convertToMMSS(currentSong.getDuration()));
        pausePlay.setOnClickListener(v-> pausePlay());
        nextBtn.setOnClickListener(v-> playNextSong());
        previousBtn.setOnClickListener(v-> playPreviousSong());
        playMusic();


    }


    private void playMusic(){ // играет музыку

        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(currentSong.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            seekBar.setProgress(0);
            seekBar.setMax(mediaPlayer.getDuration());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void playNextSong(){ // переносит на следующую музыку

        if(MyMediaPlayer.currentIndex== songsList.size()-1)
            return;
        MyMediaPlayer.currentIndex +=1;
        mediaPlayer.reset();
        setResourcesWithMusic();

    }

    private void playPreviousSong(){ // переносит обратно на музыку
        if(MyMediaPlayer.currentIndex== 0)
            return;
        MyMediaPlayer.currentIndex -=1;
        mediaPlayer.reset();
        setResourcesWithMusic();
    }

    private void pausePlay(){ // ставит паузу в музыке
        if(mediaPlayer.isPlaying())
            mediaPlayer.pause();
        else
            mediaPlayer.start();
    }


    public static String convertToMMSS(String duration){ // показывает снизу seekbara сколько прошло секунд от музыки и сколько осталось
        Long millis = Long.parseLong(duration);
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 4) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение предоставлено, сохраните файл

                convertAudioToFlac(0,(Activity) mainContext,mainContext,currentSong.title);


            } else {
                // Разрешение отклонено, показать сообщение пользователю
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void convertAudioToFlac(int flag,Activity activity,Context context,String fileName)
    {

        if (FFmpeg.getInstance(this).isSupported()) {
            Log.e("Supported---", "convertAudioToFlac: " );
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Разрешение не предоставлено, запросить разрешение
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
            } else {
                loadingSpinner.setVisibility(View.VISIBLE);
                musicIcon.setVisibility(View.INVISIBLE);
                lyricsText.setText("");
                String[] cmd = {"-i",currentSong.path, "-ss", "00:00:00", "-t","60","-ar", "16000", "-ac", "2", "-y",Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/test.flac"};

                FFmpeg fFmpeg = FFmpeg.getInstance(mainContext);
                fFmpeg.execute(cmd,new ExecuteBinaryResponseHandler(){
                    @Override
                    public void onStart() {}

                    @Override
                    public void onProgress(String message) {
                        Log.d("FFmpeg progress ---", message );

                    }

                    @Override
                    public void onFailure(String message) {
                        Log.e("FFmpeg failure ---", message );

                    }

                    @Override
                    public void onSuccess(String message) {
                        Log.d("FFmpeg success ---", message );
                        new SpeechToTextTask(flag,activity,context,fileName).execute();
                    }

                    @Override
                    public void onFinish() {

                    }

                });
            }


        } else {
            // ffmpeg не поддерживается
            Log.e("Not Supported---", "convertAudioToFlac: " );
        }
    }

    @SuppressLint("SetTextI18n")
    private void convertSpeechToText() throws IOException {

    }

    private class SpeechToTextTask extends AsyncTask<Void, Void, String> {
        private int flag = 0;
        private Activity activity;
        private Context context;
        private String filename;

        public SpeechToTextTask(int flag,Activity activity,Context context,String fileName) {
            this.flag=flag;
            this.activity=activity;
            this.context=context;
            this.filename=fileName;
        }

        @Override
        protected String doInBackground(Void... voids) {
            // Отправить речь в текстовый запрос здесь
            StringBuilder transcript = new StringBuilder(); // сохранить расшифрованный текст здесь

            try {

                GoogleCredentials credentials = GoogleCredentials.fromStream(context.getAssets().open("service-account.json"))
                        .createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
                SpeechSettings speechSettings =
                        SpeechSettings.newBuilder()
                                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                                .build();

                try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {


                    Path path = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        path = Paths.get(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/test.flac");
                    }
                    byte[] data = new byte[0];
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        data = Files.readAllBytes(path);
                    }
                    ByteString audioBytes = ByteString.copyFrom(data);


                    RecognitionConfig config = RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.FLAC)
                            .setLanguageCode("en-US")
                            .setSampleRateHertz(16000)
                            .setAudioChannelCount(2)
                            .build();

                    RecognitionAudio recognitionAudio = RecognitionAudio.newBuilder()
                            .setContent(audioBytes)
                            .build();
                    // Использовать неблокирующий вызов для получения транскрипции файла
                    OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
                            speechClient.longRunningRecognizeAsync(config, recognitionAudio);

                            while (!response.isDone()) {
                                System.out.println("Waiting for response...");
                                Thread.sleep(10000);
                            }
                    List<SpeechRecognitionResult> results = response.get().getResultsList();

                    for (SpeechRecognitionResult result : results) {
                        // Для данного фрагмента речи может быть несколько альтернативных расшифровок.
                        SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                        System.out.printf("Transcription: %s%n", alternative.getTranscript());
                        transcript.append(alternative.getTranscript());
                    }

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return transcript.toString();
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String transcript) {
            if(flag==0)
            {
                // Обновить пользовательский интерфейс с расшифрованным текстом
                loadingSpinner.setVisibility(View.GONE);
                lyricsText.setText(lyricsText.getText()+""+ transcript);
            }
            else
            {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // Разрешение не предоставлено, запросить разрешение
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                } else {
                    File file = new File(Environment.getExternalStorageDirectory(), filename+"_speech_text.txt");
                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(transcript.getBytes());
                        fos.close();
                        Toast.makeText(context, "Lyrics have been saved!", Toast.LENGTH_SHORT).show();
                        loadingSpinner.setVisibility(View.GONE);
                        if(lyricsText.getText()=="")
                        {
                            lyricsText.setText(lyricsText.getText()+""+ transcript);

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}