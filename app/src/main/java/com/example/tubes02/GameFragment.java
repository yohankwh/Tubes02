package com.example.tubes02;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import java.util.LinkedList;

public class GameFragment extends Fragment implements View.OnClickListener, View.OnTouchListener {
    protected UIThreadHandler uiThreadHandler;
    private FragmentListener fragmentListener;
    protected Canvas mCanvas;
    protected ImageView ivCanvas;
    protected Bitmap mBitmap;

    private TextView startBtn, scoreHolder, highScoreHolder;
    public boolean allowPass;
    public boolean allowScore;
    private int tileWidth;
    private int heightLimit;

    private boolean isLose;
    private LinkedList<Tile> tileList;

    private MoveThread threadObj;


    private int curX;
    private int curY;

    private int highScore;
    private int score;

    public void setUIThreadHandler(UIThreadHandler ui){
        this.uiThreadHandler = ui;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game, container, false);
        this.ivCanvas = view.findViewById(R.id.iv_main);
        this.scoreHolder = view.findViewById(R.id.score_tv);
        this.highScoreHolder = view.findViewById(R.id.hi_score_tv);
        this.startBtn = view.findViewById(R.id.tv_game_start);
        this.startBtn.setOnClickListener(this);
        this.allowPass = true;
        this.score = 0;
        this.fragmentListener.loadHighScore();

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof FragmentListener){
            this.fragmentListener = (FragmentListener) context;
        } else{
            throw new ClassCastException(context.toString()
                    + "must implement FragmentListener");
        }
    }

    public void initiateCanvas(){
        this.uiThreadHandler.setFlagTrue();
        // 1. Create Bitmap
        this.mBitmap = Bitmap.createBitmap(this.ivCanvas.getWidth(),this.ivCanvas.getHeight(), Bitmap.Config.ARGB_8888);
        // 2. Associate the bitmap to the ImageView.
        this.ivCanvas.setImageBitmap(mBitmap);
        // 3. Create a Canvas with the bitmap.
        this.mCanvas = new Canvas(this.mBitmap);
        //resetCanvas
        this.resetCanvas();
        this.ivCanvas.setOnTouchListener(this);
        this.tileWidth = this.ivCanvas.getWidth()/3;
        this.heightLimit = this.ivCanvas.getHeight();

        int[] initPos = {10, 50};
        if(threadObj == null){
            this.threadObj = new MoveThread(this.uiThreadHandler, initPos, this.ivCanvas.getHeight(), this.tileWidth);
        }else{
            this.threadObj.onStop();
            this.threadObj.thread.interrupt();
            this.threadObj = new MoveThread(this.uiThreadHandler, initPos, this.ivCanvas.getHeight(), this.tileWidth);
        }

        threadObj.moveObject();

//        DrawerAsyncTask dat = new DrawerAsyncTask();
//        dat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, initPos[0], initPos[1]);

    }

    public void resetCanvas(){
        // 4. Draw canvas background
        this.mCanvas.drawColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
        // 5. force draw
        this.ivCanvas.invalidate();
    }

    public void drawTile(){

    }

    @Override
    public void onClick(View v) {
        if(v==this.startBtn){
            this.uiThreadHandler.setFlagTrue();
            initiateCanvas();
            this.uiThreadHandler.setGameStart();
            this.startBtn.setVisibility(View.GONE);
        }
    }

    public void move(int newPosX, int newPosY){

        this.ivCanvas.invalidate();

        while(this.allowPass == true){
            this.curX = newPosX;
            this.curY = newPosY;
            this.resetCanvas();
            Paint paint = new Paint();
            int mColorTest = ResourcesCompat.getColor(getResources(),R.color.teal_200, null);
            paint.setColor(mColorTest);
            mCanvas.drawRect(newPosX, newPosY, newPosX+this.tileWidth,newPosY+200, paint);

        }


    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int x = (int)motionEvent.getX();
        int y = (int)motionEvent.getY();

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                this.checkAction(x, y);
                break;
        }
        return false;
    }

    public void checkAction(int x, int y){
        //if x, y is in range within the rectangle, add score, else game over?
        if(x>=curX && x<=curX+tileWidth && y>curY && y<=curY+200){
            this.allowPass = true;
            this.addScore(20);
        }else{
            this.threadObj.onStop();
            this.allowPass = false;
            this.uiThreadHandler.setFlagFalse();
            this.uiThreadHandler.setGameStop();

            if(this.highScore == this.score){
                updateHighScore(this.score);
            }
        }
    }

    public void resetAllowPass(){
        this.allowPass = false;
    }

    public int getScore(){return this.score;}

    public void addScore(int score){
        if(this.uiThreadHandler.getGameState()){
            this.score+=score;
            if(this.score>this.highScore){
                this.highScore = this.score;
                this.highScoreHolder.setText(this.score+"");
                updateHighScore(this.score);
            }
            this.scoreHolder.setText(this.score+"");

        }
    }

    public void setHighScore(int score){
        this.highScore = score;
        this.highScoreHolder.setText(score+"");
    }

    public void updateHighScore(int newHighScore){
        this.fragmentListener.updateHighScore(newHighScore);
    }

    public void addBonusScore(){
        addScore(5);
    }


    public int getHeightCanvas(){
        return this.ivCanvas.getMaxHeight();
    }



    private class DrawerAsyncTask extends AsyncTask<Integer, Integer, String>{
        private int countNumber;
        protected void onPreExecute(){
//            this.countNumber = 0;
        }

        protected void onProgressUpdate(Integer... progress){
            int count = progress[0];

            scoreHolder.setText(Integer.toString(count));
        }

        protected void onPostExecute(String result) {
//            btn.setText("START");
//            counterView.setText("0");
//            countNumber = 0;
//            Toast toast = Toast.makeText(counterView.getContext(),"Counter "+(position+1)+" finished", Toast.LENGTH_SHORT);
//            toast.show();
//            //Challenge 7.
//            renewJob();
        }

        protected void publishProgress(int count){
            this.onProgressUpdate(count);
        }


        @Override
        protected String doInBackground(Integer... integers) {

            int posX = integers[0];
            int posY = integers[1];

            Log.d("x is: ",posX+"");
            Log.d("y is: ",posY+"");

            int incrY = 20;

            int arrPos[] = {posX, posY};

            while(arrPos[1]<heightLimit+20){
                try{
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                move(arrPos[0],arrPos[1]);

                arrPos[1] = arrPos[1]+incrY;
            }
            Log.d("finish", "true");
            return null;
        }
    }

    public void stopThread(){
        this.threadObj.onStop();
    }

}





