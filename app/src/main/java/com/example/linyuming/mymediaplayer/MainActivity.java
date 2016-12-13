package com.example.linyuming.mymediaplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private Display currDisplay;
    private SurfaceView surfaceView;
    private SurfaceHolder holer;
    private MediaPlayer player;
    private int vWidth,vHeight;
    private Timer timer;
    private ImageButton rew;//快退
    private ImageButton pause;//暂停
    private ImageButton start;//开始
    private ImageButton ff;//快进
    private TextView play_time;//已播放时间
    private TextView all_time;//总播放时间
    private TextView title;//文件名称
    private SeekBar seekBar;//进度条
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //获取传过来的媒体路径
        Intent intent = getIntent();
        Uri uri = intent.getData();
        String mPath = "";
        if(uri!=null){
            mPath = uri.getPath();//外部程序调用该程序，多媒体路径
        }else{
            Bundle localBundle = getIntent().getExtras();
            if(localBundle!=null){
                String t_path = localBundle.getString("path");
                if(t_path!=null && !"".equals(t_path)){
                    mPath=t_path;
                }
            }
        }
        //加载当前布局文件控件操作
        title = (TextView) findViewById(R.id.title);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        rew = (ImageButton) findViewById(R.id.rew);
        pause = (ImageButton) findViewById(R.id.pause);
        start = (ImageButton) findViewById(R.id.start);
        ff = (ImageButton) findViewById(R.id.ff);

        play_time = (TextView) findViewById(R.id.play_time);
        all_time = (TextView) findViewById(R.id.all_time);
        seekBar = (SeekBar) findViewById(R.id.seekbar);

        //给SurfaceView添加CallBack监听
        holer = surfaceView.getHolder();
        holer.addCallback(new SurfaceHolder.Callback(){
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //当SurfaceView中的Surface被创建的时间被调用
                //在这里我们指定MediaPlayer在当前的Surface中进行播放
                player.setDisplay(holder);
                player.prepareAsync();
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        //为了可以播放视频或者使用Camera预览，我们需要指定其Buffer类型
        holer.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //实例化MediaPlayer对象
        player = new MediaPlayer();
        //设置播放完成监听
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mp) {
                //当MediaPlayer播放完后触发
                if(timer!=null){
                    timer.cancel();
                    timer = null;
                }
            }
        });
        //设置prepare完成监听器
        player.setOnCompletionListener(new  MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mp) {
                //当prepare完成后，该方法触发，在这里我们播放视频
                //首先取得viedeo的宽和高
                vHeight = player.getVideoHeight();
                vHeight = player.getVideoHeight();
                if(vWidth>currDisplay.getWidth() || vHeight>currDisplay.getWidth()){
                    //如果video的宽或高超过当前屏幕大小，则进行缩放
                    float wRatio =(float) vWidth/(float)currDisplay.getWidth();
                    float hRatio = (float)vHeight/(float)currDisplay.getHeight();
                    //选择一个大的一个进行缩放
                    float ratio = Math.max(wRatio,hRatio);
                    vWidth = (int)Math.ceil((float)vWidth/ratio);
                    vHeight = (int)Math.ceil((float)vHeight/ratio);
                    //设置surfaceView的布局参数
                    surfaceView.setLayoutParams(new LinearLayout.LayoutParams(vWidth,vHeight));
                    //开始播放视频
                    player.start();
                }else {
                    player.start();
                }
                if(timer!=null){
                    timer.cancel();
                    timer = null;
                }
                //启动时间更新及进度条更新任务每0.5秒更新一次
                timer = new Timer();
                timer.schedule(new MyTask(),50,500);
            }
        });
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            //指定需要播放的文件路径，初始化MediaPlayer
            if(!mPath.equals("")) {
                title.setText(mPath.substring(mPath.lastIndexOf("/") + 1));
                player.setDataSource(mPath);
            }else{
                AssetFileDescriptor afd = this.getResources().openRawResourceFd(R.raw.exodus);
                player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getDeclaredLength());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //暂停操作
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //按了开始操作pause可见，start设为隐藏
                start.setVisibility(View.GONE);
                pause.setVisibility(View.VISIBLE);
                //启动播放
                player.start();
                if(timer!=null){
                    timer.cancel();
                    timer = null;
                }
                //启动时间更新及播放条更新每0.5秒更新一次
                timer = new Timer();
                timer.schedule(new MyTask(),50,500);
            }
        });
        //快退操作
        rew.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //判断是否播放
                if(player.isPlaying()){
                    int currentPosition = player.getCurrentPosition();
                    if(currentPosition+10000<player.getDuration()){
                        player.seekTo(currentPosition+10000);
                    }
                }
            }
        });
        //快进操作
        ff.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //判断是否在播放
                if(player.isPlaying()){
                    int currentPosition = player.getCurrentPosition();
                    if(currentPosition+10000<player.getDuration()){
                        player.seekTo(currentPosition+10000);
                    }
                }
            }
        });
        //获取当前Display对象
        currDisplay = this.getWindowManager().getDefaultDisplay();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,1,0,"文件夹");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==1){
            Intent intent = new Intent(this,MyFileActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
    //进度条任务
    public class MyTask extends TimerTask{
        public void run(){
            Message message = new Message();
            message.what=1;
            //发生消息更新进度栏和时间显示
            handler.sendMessage(message);
        }
    }
    //处理进度栏和时间显示
    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //Time类需要impot java.sql包
                    Time progress = new Time(player.getCurrentPosition());
                    Time alltime = new Time(player.getDuration());
                    String timeStr = progress.toString();
                    String timeStr2 = progress.toString();
                    //已播放时间
                    play_time.setText(timeStr.indexOf(":")+1);
                    //总时间
                    all_time.setText(timeStr2.indexOf(":")+1);
                    int progressValue = 0;
                    if(player.getDuration()>0){
                        progressValue = seekBar.getMax()*player.getCurrentPosition()/player.getDuration();
                    }
                    //进度栏进度
                    seekBar.setProgress(progressValue);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(player!=null){
            player.stop();
        }
    }
}
