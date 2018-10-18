package com.diversitii.dcapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import com.diversitii.dcapp.database.Answer;
import com.diversitii.dcapp.database.AnswersDao;
import com.diversitii.dcapp.database.Question;
import com.diversitii.dcapp.database.QuestionsDao;

import java.util.Locale;

public class GameLoopActivity extends MusicActivity {
    private static final String TAG = GameLoopActivity.class.getName();

    private MediaPlayer mMediaPlayer = null; // For sound effects only
    private CountDownTimer mCountDownTimer;
    private int mButtonPresses = 0;
    private int mPlayer = 0;
    private int mTurn = 0;
    private int mQuestion = 0;
    private Question[] mQuestions;
    private int[] mAnswerIds; // Answer ID indexed by the button the answer appears on
    private int mTopicScore = 0;
    private int mTopicId = 1;
    private int mSubTopicId = Constants.DB_FALSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isPortrait = Utils.isPortrait(this);
        boolean mIsAndroidTV = Utils.isAndroidTV(this);
        if(mIsAndroidTV) {
            //Toast.makeText(this, "This is Android TV", Toast.LENGTH_LONG).show();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_game_loop);
        } else {
            if (Utils.isPortrait(this)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                //Toast.makeText(this,"This is Tablet",Toast.LENGTH_LONG).show();
            }
            setContentView(R.layout.activity_game_loop);
        }


        // Let device volume buttons control sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // If we came here after a request for a new topic, find out who's playing this round
        if (getIntent().hasExtra(MainActivity.PLAYER_TO_PLAY) &&
                getIntent().hasExtra(MainActivity.PLAYER_TURN)) {
            mPlayer = getIntent().getIntExtra(MainActivity.PLAYER_TO_PLAY, 0); // player 1 starts by default
            mTurn = getIntent().getIntExtra(MainActivity.PLAYER_TURN, 0);
        }

        // If multiplayer mode...
        if (getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE).getInt(Constants.SHPREFS_GAME_MODE, Constants.DEFAULT_GAME_MODE) == Constants.MULTIPLAYER) {
            // ...make sure that player has a name
            int numPlayers = Constants.getMaxPlayers(isPortrait);
            while (mPlayer < numPlayers && !Utils.hasPlayerName(this, mPlayer)) {
                mPlayer++;
            }
            if (mPlayer >= numPlayers) {
                // Somehow got here with no player to play!
                Log.e(TAG, "No player to play");
                Toast.makeText(this, getString(R.string.error_no_player), Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        // Get the topic and questions
        if (getIntent().hasExtra(MainActivity.TOPIC_ID)) {
            mTopicId = getIntent().getIntExtra(MainActivity.TOPIC_ID, 1);
            mSubTopicId = getIntent().getIntExtra(MainActivity.TOPIC_SUB_ID, Constants.DB_FALSE);
        } else {
            Log.e(TAG, "No topic selection");
        }
        mQuestions = new QuestionsDao().getTopicQuestions(this, mTopicId, mSubTopicId);
        initUi();
    }

    private void initUi() {
        loadQuestion();

        ((TextView) findViewById(R.id.questionsTextView)).setMovementMethod(new ScrollingMovementMethod());

        // Show answers in random order
        Answer[] answers = new AnswersDao().getTopicAnswers(this, mTopicId, mSubTopicId);
        mAnswerIds = new int[answers.length]; // extract the answer IDs
        int tvId = 0;
        for (Answer answer : answers) {
            if (answer != null && !answer.getAnswerText().isEmpty()) {
                TextView btn = findViewById(getResources().getIdentifier("@id/answerText" + (tvId + 1), null, getPackageName()));
                btn.setMovementMethod(new ScrollingMovementMethod()); // Scrolling buttons
                btn.setText(answer.getAnswerText());
                mAnswerIds[tvId] = answer.getAnswerId();
                ++tvId;
            } // else skip empty answer
        }

        // Set player name
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        boolean isPortrait = Utils.isPortrait(this);
        final int gameMode = shprefs.getInt(Constants.SHPREFS_GAME_MODE, Constants.DEFAULT_GAME_MODE);
        if (gameMode == Constants.SINGLE_PLAYER) {
            ((TextView) findViewById(R.id.tv_player_name)).setText(
                    (shprefs.getString(Constants.SHPREFS_NAME_SINGLE_PLAYER, getString(Constants.getPlayerDefaultNameIds(isPortrait)[0]))));
        } else if (gameMode == Constants.MULTIPLAYER) {
            ((TextView) findViewById(R.id.tv_player_name)).setText(
                    shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(Constants.getPlayerNameIds(isPortrait)[mPlayer]),
                            getString(Constants.getPlayerDefaultNameIds(isPortrait)[mPlayer])));
        } else {
            Log.e(TAG, "Unhandled game mode");
        }

        refreshScores();
    }

    /**
     * Displays next question to answer.
     */
    private void loadQuestion() {
        final SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        final int gameMode = shprefs.getInt(Constants.SHPREFS_GAME_MODE, Constants.DEFAULT_GAME_MODE);

        int qNum = mQuestion + 1;
        int mode = shprefs.getInt(Constants.SHPREFS_USER_GAME_MODE, Constants.GAME_MODE_1);
        if (mode == Constants.GAME_MODE_3) { // only 1 question
            if (mQuestion > 0) {
                finishTurn(gameMode);
                return;
            }
            qNum = 1;
            mQuestion = getIntent().getIntExtra(MainActivity.QUESTION_ID, 0);
        } else if (mode == Constants.GAME_MODE_2 && mQuestion > 1) { // only 2 questions
            finishTurn(gameMode);
            return;
        }

        // Set questions text
        if (Utils.isPortrait(this)) {
            ((TextView) findViewById(R.id.tv_questions_out_of)).setText(getString(R.string.question_format1, qNum, Constants.QUESTIONS_PER_TOPIC));
            if (mQuestions[mQuestion] != null) {
                ((TextView) findViewById(R.id.questionsTextView))
                        .setText(getString(R.string.question_format2, qNum, mQuestions[mQuestion].getQuestionText()));
            } else {
                Utils.alertUser(this, getString(R.string.error_question));
            }
        } else { // landscape layout
            if (mQuestions[mQuestion] != null) {
                ((TextView) findViewById(R.id.questionsTextView))
                        .setText(getString(R.string.question_format, qNum, Constants.QUESTIONS_PER_TOPIC, mQuestions[mQuestion].getQuestionText()));
            } else {
                Utils.alertUser(this, getString(R.string.error_question));
            }
        }

        int savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_TIMER, 0);
        final TextView tv = findViewById(R.id.timerText);
        if (savedIndex != 0) { // is timer set?
            // Start timer
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
            mCountDownTimer = new CountDownTimer(Constants.TIMER_VALUES[savedIndex] * DateUtils.SECOND_IN_MILLIS, DateUtils.SECOND_IN_MILLIS) {
                @Override
                public void onTick(long millisTillDone) {
                    int sec = (int) (millisTillDone / DateUtils.SECOND_IN_MILLIS) % 60;
                    int min = (int) ((millisTillDone / (DateUtils.SECOND_IN_MILLIS * 60)) % 60);
                    tv.setText(String.format(Locale.ENGLISH, "%d:%02d", min, sec));
                }

                @Override
                public void onFinish() {
                    if (mQuestion + 1 < Constants.QUESTIONS_PER_TOPIC) {
                        // Go to next question
                        ++mButtonPresses;
                        ++mQuestion;
                        loadQuestion();
                    } else {
                        timeOutGame(gameMode);
                    }
                }
            }.start();
        } else {
            // Hide timer
            tv.setVisibility(View.INVISIBLE);
        }
    }

    private void loseLife(int gameMode) {
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shprefs.edit();
        int lives = Utils.getDefaultLives(shprefs);
        if (gameMode == Constants.SINGLE_PLAYER) {
            editor.putInt(Constants.SHPREFS_LIVES_SINGLE_PLAYER,
                    Math.max(0, shprefs.getInt(Constants.SHPREFS_LIVES_SINGLE_PLAYER, lives) - 1));
        } else if (gameMode == Constants.MULTIPLAYER) {
            switch (mPlayer) {
                case 0:
                    editor.putInt(Constants.SHPREFS_LIVES_PLAYER1,
                            Math.max(0, shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER1, lives) - 1));
                    break;
                case 1:
                    editor.putInt(Constants.SHPREFS_LIVES_PLAYER2,
                            Math.max(0, shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER2, lives) - 1));
                    break;
                case 2:
                    editor.putInt(Constants.SHPREFS_LIVES_PLAYER3,
                            Math.max(0, shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER3, lives) - 1));
                    break;
                case 3:
                    editor.putInt(Constants.SHPREFS_LIVES_PLAYER4,
                            Math.max(0, shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER4, lives) - 1));
                    break;
                default:
                    Log.e(TAG, "Invalid player");
                    break;
            }
        } else {
            Log.e(TAG, "Unhandled game mode");
        }
        editor.apply();
    }

    /**
     * Refreshes game and topic scores.
     */
    private void refreshScores() {
        ((TextView) findViewById(R.id.topicScoreScoreText)).setText(String.valueOf(mTopicScore));
    }

    /**
     * Handles answer button press.
     *
     * @param v the view of the answer button pressed, tagged with its number (from 0)
     */
    public void onClick(View v) {
        // Select clicked button
        int btnId = Integer.valueOf(v.getTag().toString());
        if ((mAnswerIds[btnId] != 0) && mButtonPresses < Constants.QUESTIONS_PER_TOPIC) { // clicked button has an answer on it and not out of clicks
            final SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);

            // Sound effect
            float vol = shprefs.getInt(Constants.SHPREFS_SOUND_EFFECTS_VOL, 100) / 100f;
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
            }
            mMediaPlayer = MediaPlayer.create(this, R.raw.game_show_reveal_02);
            mMediaPlayer.setVolume(vol, vol);
            mMediaPlayer.start();

            // Show button as clicked
            int clickedTimes = (v.getTag(R.id.click_tag) != null) ? Integer.valueOf(v.getTag(R.id.click_tag).toString()) : 0;
            if (clickedTimes == 0) {
                v.setBackgroundResource(R.drawable.one_click_rect);
            } else if (clickedTimes == 1) {
                v.setBackgroundResource(R.drawable.two_click_rect);
            } else if (clickedTimes == 2) {
                v.setBackgroundResource(R.drawable.three_click_rect);
            }
            v.setTag(R.id.click_tag, clickedTimes + 1); // increment clicks on button

            final int gameMode = shprefs.getInt(Constants.SHPREFS_GAME_MODE, Constants.DEFAULT_GAME_MODE);
            final ResizingTextView view = (ResizingTextView) v;
            // Save answer button state
            final CharSequence prevText = view.getText();
            final int prevColor = view.getCurrentTextColor();
            final Drawable prevBG = view.getBackground();
            // Fade in button
            final Animation in2 = new AlphaAnimation(0.5f, 1.0f);
            in2.setDuration(DateUtils.SECOND_IN_MILLIS / 4);

            // Fade out correct/incorrect
            final Animation out2 = new AlphaAnimation(1.0f, 0.5f);
            out2.setDuration(DateUtils.SECOND_IN_MILLIS / 4);
            out2.setStartOffset(DateUtils.SECOND_IN_MILLIS); // pause on correct/incorrect display
            out2.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setText(prevText);
                    view.setTextColor(prevColor);
                    view.setBackground(prevBG);
                    view.startAnimation(in2); // fade in answer button
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            // Fade out answer button
            final Animation out = new AlphaAnimation(1.0f, 0.5f);
            out.setDuration(DateUtils.SECOND_IN_MILLIS / 4);

            // Fade in correct/incorrect
            final Animation in = new AlphaAnimation(0.5f, 1.0f);
            in.setDuration(DateUtils.SECOND_IN_MILLIS / 4);
            in.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.startAnimation(out2); // fade it out
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            if (isRightAnswer(btnId)) {
                // Animate button
                out.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setText(getString(R.string.answer_correct));
                        view.setTextColor(ContextCompat.getColor(GameLoopActivity.this, android.R.color.white));
                        view.setBackgroundResource(R.drawable.answer_correct);
                        view.startAnimation(in);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                view.startAnimation(out);

                in2.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // Button animation over

                        int questionPoints = mQuestions[mQuestion].getPoints() * Utils.getPointsValue(shprefs);
//                        Log.d(TAG, "Points: " + questionPoints);

                        // Increment and save game score
                        String scoreKey = (gameMode == Constants.SINGLE_PLAYER) ? Constants.SHPREFS_SCORE_SINGLE :
                                Constants.getPlayerScoreKeys(Utils.isPortrait(GameLoopActivity.this))[mPlayer];
                        int gameScore = questionPoints + shprefs.getInt(scoreKey, 0);
                        SharedPreferences.Editor editor = shprefs.edit();
                        editor.putInt(scoreKey, gameScore);
                        editor.apply();

                        mTopicScore += questionPoints; // update topic score
                        refreshScores();

                        finishTurn(gameMode);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            } else { // wrong answer
                // Animate button
                out.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setText(getString(R.string.answer_incorrect));
                        view.setTextColor(ContextCompat.getColor(GameLoopActivity.this, android.R.color.white));
                        view.setBackgroundResource(R.drawable.answer_incorrect);
                        view.startAnimation(in);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                view.startAnimation(out);

                in2.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // Animation over
                        loseLife(gameMode);
                        finishTurn(gameMode);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        } // else clicked button is blank, ignore
    }

    private void finishTurn(int gameMode) {
        // Increment question
        ++mButtonPresses;
        ++mQuestion;
        if (gameMode == Constants.SINGLE_PLAYER && mButtonPresses >= Constants.QUESTIONS_PER_TOPIC) {
            // Time for a new topic
            (new Thread(new Runnable() {
                public void run() {
                    try { // hold for a moment so player can see result of last question
                        Thread.sleep(750); // 3/4 second
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                    } finally {
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }
                }
            })).start();
        } else if (gameMode == Constants.MULTIPLAYER && mButtonPresses >= Constants.QUESTIONS_PER_TOPIC) {
            // Time for a new topic and/or player
            (new Thread(new Runnable() {
                public void run() {
                    try { // hold for a moment so player can see result of last question
                        Thread.sleep(750); // 3/4 second
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                    } finally {
                        getNextPlayer();
                    }
                }
            })).start();
        } else {
            loadQuestion(); // more questions to answer for topic
        }
    }

    /**
     * Switches players in multiplayer mode or breaks game loop.
     */
    private void getNextPlayer() {
        int numPlayers = Constants.getMaxPlayers(Utils.isPortrait(this));
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        if (mPlayer == numPlayers - 1 && mTurn >= Constants.NUM_TURNS - 1) {
            // No one else to play
            float vol = shprefs.getInt(Constants.SHPREFS_SOUND_EFFECTS_VOL, 100) / 100f;
            // Sound effect for changing player
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
            }
            mMediaPlayer = MediaPlayer.create(this, R.raw.game_show_buzz_in_10);
            mMediaPlayer.setVolume(vol, vol);
            mMediaPlayer.start();

            startActivity(new Intent(this, MainActivity.class));
        } else { // get next player or player's next turn
            // Increment turns
            if (mTurn >= Constants.NUM_TURNS - 1) { // out of turns, next player
                // Check there is a next player with a name in the scoreboard
                int nextPlayer = mPlayer + 1;
                while (nextPlayer < numPlayers && !Utils.hasPlayerName(this, nextPlayer)) {
                    nextPlayer++;
                }
                float vol = shprefs.getInt(Constants.SHPREFS_SOUND_EFFECTS_VOL, 100) / 100f;
                // Sound effect for changing player
                if (mMediaPlayer != null) {
                    mMediaPlayer.reset();
                }
                mMediaPlayer = MediaPlayer.create(this, R.raw.game_show_buzz_in_10);
                mMediaPlayer.setVolume(vol, vol);
                mMediaPlayer.start();
                if (nextPlayer >= numPlayers) {
                    // No more players with names, exit
                    startActivity(new Intent(this, MainActivity.class));
                } else { // get next player
                    Intent i = new Intent(this, MainActivity.class);

                    i.putExtra(MainActivity.PLAYER_TO_PLAY, nextPlayer);
                    i.putExtra(MainActivity.PLAYER_TURN, 0); // reset turns

                    try { // wait for sound effect to finish
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                    }
                    startActivity(i);
                }
            } else {
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra(MainActivity.PLAYER_TO_PLAY, mPlayer); // same player
                i.putExtra(MainActivity.PLAYER_TURN, mTurn + 1);
                startActivity(i);
            }
        }
        finish();
    }

    private boolean isRightAnswer(int answer) {
        return (mQuestion < mQuestions.length && answer < mAnswerIds.length && mQuestions[mQuestion].getAnswerId() == mAnswerIds[answer]);
    }

    private void timeOutGame(int gameMode) {
        Toast.makeText(this, getString(R.string.time_up), Toast.LENGTH_SHORT).show();
        if (gameMode == Constants.SINGLE_PLAYER) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        } else if (gameMode == Constants.MULTIPLAYER) {
            getNextPlayer();
        } else {
            Log.e(TAG, "Unhandled game mode");
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) { // clean up sound
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        if (mCountDownTimer != null) { // clean up timer
            mCountDownTimer.cancel();
        }
    }
}
