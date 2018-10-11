package com.diversitii.dcapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.diversitii.dcapp.billing.IabBroadcastReceiver;
import com.diversitii.dcapp.billing.IabHelper;
import com.diversitii.dcapp.billing.IabResult;
import com.diversitii.dcapp.billing.Inventory;
import com.diversitii.dcapp.billing.Purchase;
import com.diversitii.dcapp.database.Answer;
import com.diversitii.dcapp.database.AnswersDao;
import com.diversitii.dcapp.database.Category;
import com.diversitii.dcapp.database.CategoryDao;
import com.diversitii.dcapp.database.Pack;
import com.diversitii.dcapp.database.PacksDao;
import com.diversitii.dcapp.database.Question;
import com.diversitii.dcapp.database.QuestionsDao;
import com.diversitii.dcapp.database.Topic;
import com.diversitii.dcapp.database.TopicsDao;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Topic selection plus scorecard display and game mode control. Default game mode is single player.
 */
public class MainActivity extends MusicActivity {
    private static final String TAG = MainActivity.class.getName();
    static final String TOPIC_ID = "TOPIC_ID"; // To send topic ID selection to game loop
    static final String TOPIC_SUB_ID = "TOPIC_SUB_ID"; // To further identify topic in game loop
    static final String QUESTION_ID = "QUESTION_ID"; // Question ID of question to play in Game Mode 3. Determined by the number button it appears on, in the order 1, 2, 3, 3
    static final String PLAYER_TO_PLAY = "PLAYER_TO_PLAY"; // To send/receive next player to play game loop
    static final String PLAYER_TURN = "PLAYER_TURN"; // To send/receive the turn the next player is on in the game loop
    private static final int TOPIC_ENTRY_PANEL_SLOT1 = 3; // Every third panel
    private static final int TOPIC_ENTRY_PANEL_SLOT2 = 10; // Every third panel
    private static boolean mIsFirstLaunch = true;

    private int mSelectedPlayer; // ID of player name display in UI
    private int mSelectedTopic = -1;
    private MediaPlayer mMediaPlayer = null; // For sound effects only
    private int mPlayer = -1; // counts from 0
    private int mTurn = -1; // counts from 0
    private int mQuestionId = 1;
    private Topic[] mTopics;

    private boolean mIsPortrait;
    private ViewPager mViewPager;
    private int mCurrentPage = 0;
    private static Timer mTimer;
    private static Activity mActivity;
    private static TopicCategory[] mCats;
    private boolean[] mIsPackUnlocked;
    private int mTotalPacks = 0;
    private SparseIntArray mCategoriesByPacks;
    private int mAvailableCategoryPacks = 0;
    private static int mSelectedPackId;

    // In-app billing
    private static IabHelper mHelper;
    private static final int RC_REQUEST = 12103; // (arbitrary) request code for the purchase flow (permissible range is restricted)
    private IabBroadcastReceiver mBroadcastReceiver; // listen for purchase notifications while Activity is running
    // Listener called when inventory querying is done
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "onQueryInventoryFinished result: " + result);

            if (mHelper == null) return;  // Quit if helper was disposed of

            if (result.isFailure()) {
                Utils.alertUser(MainActivity.this, getString(R.string.error_inventory2, result));
                return;
            }

            // Check for purchased packs
            PacksDao packsDao = new PacksDao();
            SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = shprefs.edit();
            for (int i = 1; i <= mTotalPacks; ++i) { // packs count from 1!!
//                Purchase pack = inventory.getPurchase(Constants.getCategoryPackSku(i));
                Purchase pack = inventory.getPurchase(Constants.getPackSku(i));
                if (pack != null && Utils.verifyDeveloperPayload(pack)) {
//                    editor.putBoolean(Constants.getCategoryPackSku(i), true);
                    packsDao.setIsOwned(MainActivity.this, i);
//                } else {
//                    editor.putBoolean(Constants.getCategoryPackSku(i), false);
                }
            }
            editor.apply();

            updatePacks();
            fetchCategories();
        }
    };
    // Listener called when a purchase completes
    static IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "onIabPurchaseFinished result: " + result + "; purchase: " + purchase);

            if (mHelper == null) return; // Quit if helper was disposed of

            if (result.isFailure()) {
                if (!result.getMessage().contains("User canceled")) {
                    Utils.alertUser(mActivity, mActivity.getString(R.string.error_purchasing, result));
                }
                return;
            }
            if (!Utils.verifyDeveloperPayload(purchase)) {
                Utils.alertUser(mActivity, mActivity.getString(R.string.error_purchasing2));
                return;
            }
            // Purchase success, unlock purchased pack
            Utils.alertUser(mActivity, mActivity.getString(R.string.purchased_pack));
            new PacksDao().setIsOwned(mActivity, mSelectedPackId);
            Utils.installPack(mActivity, mSelectedPackId);

            closePanels(mActivity);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsPortrait = Utils.isPortrait(this);
        if (mIsPortrait) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        setContentView(R.layout.activity_topic_selection);

        mActivity = this;

        if (!isGooglePlayServicesAvailable()) { // App requires Play Services SDK
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
        }

        // Let device volume buttons control sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        doFirebaseAuth();

        // Get next player to play, if this activity was started from game loop
        if (getIntent().hasExtra(PLAYER_TO_PLAY) &&
                getIntent().hasExtra(PLAYER_TURN)) {
            mPlayer = getIntent().getIntExtra(PLAYER_TO_PLAY, -1);
            mTurn = getIntent().getIntExtra(PLAYER_TURN, -1);
        }

        showTopics();

        // Scrolling buttons
        ResizingTextView btn = findViewById(R.id.topicBtn1);
        btn.setMovementMethod(new ScrollingMovementMethod());
        btn = findViewById(R.id.topicBtn2);
        btn.setMovementMethod(new ScrollingMovementMethod());
        if (!mIsPortrait) {
            btn = findViewById(R.id.topicBtn3);
            btn.setMovementMethod(new ScrollingMovementMethod());
            btn = findViewById(R.id.topicBtn4);
            btn.setMovementMethod(new ScrollingMovementMethod());
        }

        // "CC" button
        findViewById(R.id.btn_cat_controller).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CategoryControllerActivity.class));
                finish();
            }
        });
    }

    public void updateTopics() {
        showTopics();
    }

    /**
     * Displays topic choices.
     */
    private void showTopics() {
        final SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        int gameMode = shprefs.getInt(Constants.SHPREFS_USER_GAME_MODE, Constants.GAME_MODE_1);
        mTopics = new TopicsDao().getRandomTopics(this, Constants.getMaxTopics(mIsPortrait));
        for (int i = 0; i < Constants.getMaxTopics(mIsPortrait); ++i) {
            if (mTopics[i] != null) {
                if (gameMode == Constants.GAME_MODE_3) {
                    // Show subtopics instead of topics
                    int questionId = Math.min(i + 1, Constants.QUESTIONS_PER_TOPIC); // 1, 2, 3, 3
                    final int subId = new QuestionsDao().getSubtopicId(this, questionId, mTopics[i].getTopicId());
                    if (subId == Constants.DEFAULT_SUBTOPIC_ID) { // default subtopic is same as topic
                        ((TextView) findViewById(getResources().getIdentifier("@id/topicBtn" + (i + 1), null, getPackageName())))
                                .setText(mTopics[i].getTopicName());
                        findViewById(getResources().getIdentifier("@id/loading" + (i + 1), null, getPackageName()))
                                .setVisibility(View.GONE);

                    } else {
                        // Get subtopic name
                        final int index = i;
                        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_SUBTOPICS).document(String.valueOf(subId))
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document != null && document.exists()) {
                                                ((TextView) findViewById(getResources().getIdentifier("@id/topicBtn" + (index + 1), null, getPackageName())))
                                                        .setText(document.get(Constants.FIELD_TEXT).toString());
                                                findViewById(getResources().getIdentifier("@id/loading" + (index + 1), null, getPackageName()))
                                                        .setVisibility(View.GONE);
                                            } else {
                                                Log.e(TAG, "Fetch failed for subtopic " + subId + " topic " + mTopics[index].getTopicName());
                                            }
                                        } else {
                                            Log.e(TAG, "Subtopic fetch error: " + task.getException());
                                        }
                                    }
                                });
                    }
                } else {
                    ((TextView) findViewById(getResources().getIdentifier("@id/topicBtn" + (i + 1), null, getPackageName())))
                            .setText(mTopics[i].getTopicName());
                    findViewById(getResources().getIdentifier("@id/loading" + (i + 1), null, getPackageName()))
                            .setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isGooglePlayServicesAvailable()) { // App requires Play Services SDK
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
        }

        initScorecard(getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE)
                .getInt(Constants.SHPREFS_GAME_MODE, Constants.DEFAULT_GAME_MODE), Utils.isPortrait(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        // Clean up billing
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
        if (mHelper != null) {
            try {
                mHelper.disposeWhenFinished();
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "This is expected if billing service was unavailable on device: " + e.toString());
            }
            mHelper = null;
        }

        if (mMediaPlayer != null) { // clean up sound
            if (mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mHelper == null) return; // Quit if helper was disposed of

        // Pass activity result to helper
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // Not expecting non-billing related activity results, so do nothing
            super.onActivityResult(requestCode, resultCode, data);
        }  // else handled by helper
    }

    private boolean isGooglePlayServicesAvailable() {
        return ConnectionResult.SUCCESS ==
                (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this));
    }

//    /**
//     * Enables or disables the loading screen.
//     */
//    private void setLoadScreen(boolean set) {
//        findViewById(R.id.layout_main).setVisibility(set ? View.GONE : View.VISIBLE);
//        findViewById(R.id.layout_loading).setVisibility(set ? View.VISIBLE : View.GONE);
//    }

    /**
     * Signs in anonymously with Firebase.
     */
    private void doFirebaseAuth() {
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            setup();
                        } else {
                            Log.e(TAG, "Firebase authentication failed: ", task.getException());
                            Toast.makeText(MainActivity.this, getString(R.string.error_firebase),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Setup that requires Firebase authentication
    private void setup() {
        initDatabase();

        // Subscribe to notifications if notifications are enabled in settings
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        if (shprefs.getBoolean(Constants.SHPREFS_NOTIFICATIONS_OK, Constants.DEFAULT_NOTIFICATIONS_OK)) {
            FirebaseMessaging.getInstance().subscribeToTopic(Constants.NOTIFICATIONS_TOPIC);
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.NOTIFICATIONS_TOPIC);
        }

        if (mIsFirstLaunch) { // show category panels
            mIsFirstLaunch = false;
            getPacksToPromote();
        }
    }

    /**
     * Sets up the database if it has not been set up.
     */
    private void initDatabase() {
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);

        if (!shprefs.getBoolean(Constants.SHPREFS_IS_DATABASE_INIT, false)) {
            // Add free pack data to database
            initPackData();

            SharedPreferences.Editor editor = shprefs.edit();
            editor.putBoolean(Constants.SHPREFS_IS_DATABASE_INIT, true);
            editor.apply();
        }

//        // If initial pack just installed, save update times
//        String updateKey = Constants.getTopicsUpdateTime(Constants.FREE_PACK_ID);
//        long updated = shprefs.getLong(updateKey, Constants.FREE_PACK_UPDATE_TIME);
//        // Check need to reload topics
//        Utils.checkForPackUpdates(this, updated, Constants.COLLECTION_TOPICS, updateKey, new TopicsDao(), Constants.FREE_PACK_ID);

//        updateKey = Constants.getQuestionsUpdateTime(Constants.FREE_PACK_ID);
//        updated = shprefs.getLong(updateKey, Constants.FREE_PACK_UPDATE_TIME);
//        // Check need to reload questions
//        Utils.checkForPackUpdates(this, updated, Constants.COLLECTION_QUESTIONS, updateKey, new QuestionsDao(), Constants.FREE_PACK_ID);

//        updateKey = Constants.getAnswersUpdateTime(Constants.FREE_PACK_ID);
//        updated = shprefs.getLong(updateKey, Constants.FREE_PACK_UPDATE_TIME);
//        // Check need to reload answers
//        Utils.checkForPackUpdates(this, updated, Constants.COLLECTION_ANSWERS, updateKey, new AnswersDao(), Constants.FREE_PACK_ID);
    }

    /**
     * Initializes database with purchasable pack data, category data, and free pack data.
     */
    private void initPackData() {
        // Get the packs category IDs
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_PACKS)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            PacksDao packsDao = new PacksDao();
                            ArrayList<Integer> catIds = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                if (document != null && document.exists()) {
                                    try {
                                        int catId = Integer.parseInt(document.get(Constants.FIELD_CATEGORY_ID).toString());
                                        if (!catIds.contains(catId)) {
                                            catIds.add(catId);
                                        }

                                        int packId = Integer.parseInt(document.getId());
                                        packsDao.addPack(MainActivity.this,
                                                new Pack(packId, catId, (packId == Constants.FREE_PACK_ID)));
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Invalid ID! Pack " + document.getId() + " with category " + document.get(Constants.FIELD_CATEGORY_ID).toString());
                                    }
                                } else {
                                    Log.e(TAG, "Pack not found");
                                }
                            }
                            downloadCategories(catIds);
                        } else {
                            Log.w(TAG, "Error getting packs: ", task.getException());
                        }
                    }
                });

        getFreePack(Constants.SHPREFS_PACKS_UPDATE);
    }

    private void downloadCategories(ArrayList<Integer> catIds) {
        final CategoryDao categoryDao = new CategoryDao();
        for (final int catId : catIds) {
            if (catId != Constants.DEFAULT_CAT_ID) {
                // Get category text
                FirebaseFirestore.getInstance().collection(Constants.COLLECTION_CATEGORIES).document(String.valueOf(catId))
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document != null && document.exists()) {
                                        categoryDao.addCategory(MainActivity.this,
                                                new Category(catId, document.get(Constants.FIELD_TEXT).toString(), null));
                                    } else {
                                        Log.e(TAG, "Category not found: " + catId);
                                    }
                                } else {
                                    Log.w(TAG, "Error getting categories: ", task.getException());
                                }
                            }
                        });

                // Get category icon, if not already saved
                if (!categoryDao.hasIcon(MainActivity.this, catId)) {
                    Log.d(TAG, "Getting icon for cat " + catId);
                    // Save new icon
                    final StorageReference storageRef = FirebaseStorage.getInstance()
                            .getReferenceFromUrl(Constants.STORAGE_URL + Constants.DIR_ICONS);
                    StorageReference iconRef = storageRef.child(catId + Constants.ICON_FILE_EXT);
                    final String fileName = Constants.getCategoryIconFileName(catId);
                    File file = new File(MainActivity.this.getFilesDir(), fileName);
                    iconRef.getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            // Save icon file reference
                            categoryDao.addCategory(MainActivity.this, new Category(catId, null, fileName));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Category icon fetch: " + e.toString());
                        }
                    });
                } // else already saved
//            } else {
//                Log.d(TAG, "Pack had no category");
            }
        }
    }

    private void getFreePack(final String updateKey) {
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_PACKS).document(Constants.FREE_PACK_ID + "")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            // Get the free pack's topic IDs
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String ids = document.get(Constants.FIELD_TOPIC_IDS).toString();
                                Utils.savePackContents(MainActivity.this, ids.split(Constants.TOPIC_ID_LIST_SEPARATOR), updateKey, true);
                            } else {
                                Log.e(TAG, "Free pack not found");
                            }
                        } else {
                            Log.w(TAG, "Error getting free pack: ", task.getException());
                        }
                    }
                });
    }

    /**
     * Gets info on category topic packs to promote in sliding panels.
     */
    private void getPacksToPromote() {
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_PACKS)
                .whereEqualTo(Constants.FIELD_DO_PROMOTE, true)
                .whereGreaterThan(Constants.FIELD_CATEGORY_ID, Constants.DEFAULT_CAT_ID)
                .limit(Constants.PACKS_TO_PROMOTE_LIMIT)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // Get the categories of the packs to promote
                            SparseIntArray catsByPack = new SparseIntArray();
                            PacksDao packsDao = new PacksDao();
                            for (DocumentSnapshot document : task.getResult()) {
                                if (document != null && document.exists()) {
                                    try {
//                                        Log.d(TAG, "PACK ID " + document.getId() + " CAT " + document.get(Constants.FIELD_CATEGORY_ID));
                                        int packId = Integer.parseInt(document.getId());
                                        if (!packsDao.getIsOwned(MainActivity.this, packId)
                                                && packId != Constants.FREE_PACK_ID) { // skip free pack and bought packs
                                            catsByPack.put(packId,
                                                    Integer.parseInt(document.get(Constants.FIELD_CATEGORY_ID).toString()));
                                        }
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Invalid ID! Pack " + document.getId() + " with category " + document.get(Constants.FIELD_CATEGORY_ID).toString());
                                    }
                                } else {
                                    Log.e(TAG, "Pack not found");
                                }
                            }
                            mCategoriesByPacks = catsByPack;
                            loadPacks();
                        } else {
                            Log.w(TAG, "Error getting packs to promote: ", task.getException());
                        }
                    }
                });
    }

    /**
     * Loads number of available packs.
     */
    private void loadPacks() {
        // Get number of packs available for purchase
        FirebaseDatabase.getInstance()
                .getReference(Constants.METADATA_KEY)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Called once with initial value and again whenever data at this location updates
                        Long res = dataSnapshot.getValue(Long.class);
                        setupBilling((res != null) ? res.intValue() : 0);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.w(TAG, "Error reading packs data", error.toException());
                    }
                });
    }

    // Set up in-app billing (asynchronous)
    private void setupBilling(int numPacks) {
        mTotalPacks = numPacks;
        if (mCategoriesByPacks.size() > 0) {
            mHelper = new IabHelper(this, Constants.BASE64_PUBLIC_KEY);
            mHelper.enableDebugLogging(false); // set true for debugging
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        Utils.alertUser(MainActivity.this, getString(R.string.error_billing_setup, result));
                        // For testing on device w/o billing service
                        updatePacks();
                        fetchCategories();

                        return;
                    }

                    if (mHelper == null) return; // Quit if helper was disposed of

                    // Register AFTER IabHelper is setup, but before first call to getPurchases()
                    mBroadcastReceiver = new IabBroadcastReceiver(new IabListener());
                    registerReceiver(mBroadcastReceiver, new IntentFilter(IabBroadcastReceiver.ACTION));
                    try { // Check purchase history
                        mHelper.queryInventoryAsync(mGotInventoryListener);
                    } catch (IabHelper.IabAsyncInProgressException e) {
                        Utils.alertUser(MainActivity.this, getString(R.string.error_inventory));
                    }
                }
            });
        } else { // no packs to showcase
            closePanels(this);
        }
    }

    // Fetches only categories that have not been purchased.
    private void fetchCategories() {
        mCats = new TopicCategory[mAvailableCategoryPacks];
        int catIndex = 0;
        for (int panelId = 0; panelId < mCategoriesByPacks.size() && catIndex < mAvailableCategoryPacks; ++panelId) {
            int packId = mCategoriesByPacks.keyAt(panelId);
            final int catId = mCategoriesByPacks.get(packId);

            if (packId > 0 && packId <= mTotalPacks
                    && !mIsPackUnlocked[packId - 1]
                    && (catIndex + 1) % TOPIC_ENTRY_PANEL_SLOT2 != 0
                    && (catIndex + 1) % TOPIC_ENTRY_PANEL_SLOT2 != TOPIC_ENTRY_PANEL_SLOT1) {

//                mCats[catIndex] = new TopicCategory(Constants.getCategoryPackSku(packId), packId);
                mCats[catIndex] = new TopicCategory(Constants.getPackSku(packId), packId);

                // Get category name by ID
                final int index = catIndex;
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection(Constants.COLLECTION_CATEGORIES).document(catId + "")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document != null && document.exists()) {
                                        mCats[index].setCategoryName(document.get(Constants.FIELD_TEXT).toString());
                                    } else {
                                        Log.e(TAG, "Category name fetch failed for " + catId);
                                    }
                                } else {
                                    Log.e(TAG, "Category name fetch error");
                                }
                            }
                        });

                // Category icon
                mCats[catIndex].setIcon(new File(this.getFilesDir(), Constants.getCategoryIconFileName(catId)));
                if (index == mCats.length - 1) {
                    showCategories();
                }
                ++catIndex;
            } else if (packId <= mTotalPacks && !mIsPackUnlocked[packId - 1]) { // if not a bought pack
                // Skip topic entry dialog slot
                Log.d(TAG, "Topic entry dialog slot");
                ++catIndex;
                --panelId;
            } // else skip bought pack
        }
        findViewById(R.id.categories_pager).setVisibility(View.VISIBLE);
    }

    private void showCategories() {
        // Create adapter to return a fragment for each page of category packs
        // If this becomes too memory intensive, switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter that will hold the section contents
        mViewPager = findViewById(R.id.categories_pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(0);

        // Page turning timer
        mTimer = new Timer();

//        int numPages = (mIsPortrait) ? mCats.length / 2 + (mCats.length % 2) :
//                (mCats.length % 4 == 0) ? mCats.length / 4 : mCats.length / 4 + 1; // double up categories for landscape (half as many pages)
        int numPages = (mIsPortrait) ? mCats.length : mCats.length / 2 + (mCats.length % 2); // double up categories for landscape (half as many pages)
        mTimer.scheduleAtFixedRate(new PageSwitchTask(numPages),
                Constants.PAGE_SWITCH_TIME, Constants.PAGE_SWITCH_TIME);
    }

    /**
     * Updates bought packs locally.
     */
    private void updatePacks() {
//        final SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        mIsPackUnlocked = new boolean[mTotalPacks];
        PacksDao packsDao = new PacksDao();
        for (int i = 1; i <= mTotalPacks; ++i) { // packs count from 1!!
//            mIsPackUnlocked[i - 1] = shprefs.getBoolean(Constants.getCategoryPackSku(i), false);
            mIsPackUnlocked[i - 1] = packsDao.getIsOwned(MainActivity.this, i);
//                String updateKey = Constants.getTopicsUpdateTime(i);
//                Utils.checkForPackUpdates(this, shprefs.getLong(updateKey, 0), Constants.getCategoryPackDirectory(i), Constants.TOPICS_FILE, updateKey, new TopicsDao(), i);
//                updateKey = Constants.getAnswersUpdateTime(i);
//                Utils.checkForPackUpdates(this, shprefs.getLong(updateKey, 0), Constants.getCategoryPackDirectory(i), Constants.ANSWERS_FILE, updateKey, new AnswersDao(), i);
//                updateKey = Constants.getQuestionsUpdateTime(i);
//                Utils.checkForPackUpdates(this, shprefs.getLong(updateKey, 0), Constants.getCategoryPackDirectory(i), Constants.QUESTIONS_FILE, updateKey, new QuestionsDao(), i);
        }
        // Add space for topic entry dialogs
        int availablePacks = mCategoriesByPacks.size();
        int topicEntryDialogs = 0;
        int total = availablePacks;
        while (total / TOPIC_ENTRY_PANEL_SLOT2 >= 1) { // brute force it assuming < 1000 topic categories
            topicEntryDialogs += 2;
            total -= TOPIC_ENTRY_PANEL_SLOT2 - 2;
        }
        if (total >= TOPIC_ENTRY_PANEL_SLOT1) {
            ++topicEntryDialogs;
            ++total;
        }
        if (total == TOPIC_ENTRY_PANEL_SLOT2) {
            ++topicEntryDialogs;
        }
        mAvailableCategoryPacks = availablePacks + topicEntryDialogs;
    }

    private static Bitmap loadImage(File f) {
        Bitmap bmp = null;
        if (f.exists()) {
            bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
        } else {
            Log.w(TAG, "No image file");
        }
        return bmp;
    }

    private static void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    private static void closePanels(Activity act) {
        act.findViewById(R.id.categories_pager).setVisibility(View.GONE);
    }

    private static void showTopicEntryDialog(final Activity activity) {
        final Dialog dialog = new Dialog(activity);

        Window window = dialog.getWindow();
        if (window != null) {
            // Set animation
            if (Utils.isPortrait(activity)) {
                window.getAttributes().windowAnimations = R.style.Animations_expand;
            } else {
                window.getAttributes().windowAnimations = R.style.Animations_expand_start;
            }

            // Fill screen
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(false);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_add_topic);

            // Check for soft keyboard
            final LinearLayout root = dialog.findViewById(R.id.layout_root);
            root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    Rect r = new Rect(); // for visible view
                    root.getWindowVisibleDisplayFrame(r);
                    int heightDiff = r.height() - root.getRootView().getHeight();
                    if (heightDiff > dpToPx(activity, 200)) { // if over 200dp, probably keyboard is open
                        dialog.findViewById(R.id.tv_top).setVisibility(View.INVISIBLE);
                        dialog.findViewById(R.id.btn_confirm).setVisibility(View.INVISIBLE);
                    } else { // keyboard is closed
                        dialog.findViewById(R.id.tv_top).setVisibility(View.VISIBLE);
                        dialog.findViewById(R.id.btn_confirm).setVisibility(View.VISIBLE);
                    }
                }
            });

            dialog.findViewById(R.id.btn_confirm).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Fetch input
                    String q1 = ((TextView) dialog.findViewById(R.id.et_question1)).getText().toString();
                    String q2 = ((TextView) dialog.findViewById(R.id.et_question2)).getText().toString();
                    String q3 = ((TextView) dialog.findViewById(R.id.et_question3)).getText().toString();
                    String a1 = ((TextView) dialog.findViewById(R.id.et_answer1)).getText().toString();
                    String a2 = ((TextView) dialog.findViewById(R.id.et_answer2)).getText().toString();
                    String a3 = ((TextView) dialog.findViewById(R.id.et_answer3)).getText().toString();
                    String a4 = ((TextView) dialog.findViewById(R.id.et_answer4)).getText().toString();
                    String a5 = ((TextView) dialog.findViewById(R.id.et_answer5)).getText().toString();
                    String a6 = ((TextView) dialog.findViewById(R.id.et_answer6)).getText().toString();
                    String topic = ((TextView) dialog.findViewById(R.id.et_topic)).getText().toString();

                    if (q1.isEmpty() || q2.isEmpty() || q3.isEmpty() ||
                            a1.isEmpty() || a2.isEmpty() || a3.isEmpty() ||
                            topic.isEmpty()) {
                        Toast.makeText(activity, activity.getString(R.string.error_topic_input), Toast.LENGTH_SHORT).show();
                    } else if (a4.isEmpty() && a5.isEmpty() && a6.isEmpty()) {
                        Toast.makeText(activity, activity.getString(R.string.error_answer_input), Toast.LENGTH_SHORT).show();
                    } else {
                        // Save new topic
                        SharedPreferences shprefs = activity.getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
                        int topicId = shprefs.getInt(Constants.SHPREFS_USER_ADDED_TOPIC_ID, 0);
                        new TopicsDao().addCustomTopic(activity, new Topic(topicId, Constants.DEFAULT_CAT_ID, topic, Constants.DB_TRUE));
                        QuestionsDao qd = new QuestionsDao();
                        qd.addCustomQuestion(activity, new Question(q1, 1, Constants.DEFAULT_QUESTION_POINTS), 1, topicId);
                        qd.addCustomQuestion(activity, new Question(q2, 2, Constants.DEFAULT_QUESTION_POINTS), 2, topicId);
                        qd.addCustomQuestion(activity, new Question(q3, 3, Constants.DEFAULT_QUESTION_POINTS), 3, topicId);
                        AnswersDao ad = new AnswersDao();
                        ad.addCustomAnswer(activity, new Answer(1, a1), topicId);
                        ad.addCustomAnswer(activity, new Answer(2, a2), topicId);
                        ad.addCustomAnswer(activity, new Answer(3, a3), topicId);
                        ad.addCustomAnswer(activity, new Answer(4, a4), topicId);
                        ad.addCustomAnswer(activity, new Answer(5, a5), topicId);
                        ad.addCustomAnswer(activity, new Answer(6, a6), topicId);

                        // Increment custom topic ID
                        SharedPreferences.Editor editor = shprefs.edit();
                        editor.putInt(Constants.SHPREFS_USER_ADDED_TOPIC_ID, (topicId + 1));
                        editor.apply();

                        // Clear fields
                        ((TextView) dialog.findViewById(R.id.et_question1)).setText("");
                        ((TextView) dialog.findViewById(R.id.et_question2)).setText("");
                        ((TextView) dialog.findViewById(R.id.et_question3)).setText("");
                        ((TextView) dialog.findViewById(R.id.et_answer1)).setText("");
                        ((TextView) dialog.findViewById(R.id.et_answer2)).setText("");
                        ((TextView) dialog.findViewById(R.id.et_answer3)).setText("");
                        ((TextView) dialog.findViewById(R.id.et_answer4)).setText("");
                        ((TextView) dialog.findViewById(R.id.et_answer5)).setText("");
                        ((TextView) dialog.findViewById(R.id.et_answer6)).setText("");
                        ((TextView) dialog.findViewById(R.id.et_topic)).setText("");
                        Toast.makeText(activity, activity.getString(R.string.msg_topic_added), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialog.findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closePanels(activity);
                    dialog.dismiss();
                }
            });

            dialog.show();
            window.setAttributes(lp);
        } else {
            Log.e(TAG, "Null dialog window");
        }
    }

    private static float dpToPx(Context context, @SuppressWarnings("SameParameterValue") float valueInDp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp,
                context.getResources().getDisplayMetrics());
    }

    private static void promptPurchasePack(final Activity act, final TopicCategory pack) {
        mSelectedPackId = pack.getPackId();

        new AlertDialog.Builder(act)
                .setMessage(act.getString(R.string.confirm_purchase_category_pack, pack.getCategoryName()))
                .setPositiveButton(act.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            mHelper.launchPurchaseFlow(act, pack.getSku(),
                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            Utils.alertUser(act, act.getString(R.string.error_purchase));
                        }
                    }
                })
                .setNegativeButton(act.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    /**
     * Displays player names and single player or multiplayer scores depending on game mode.
     *
     * @param gameMode Constants game mode value
     */
    @SuppressWarnings("ConstantConditions")
    private void initScorecard(int gameMode, boolean isPortrait) {
        int[] nameIds = Constants.getPlayerNameIds(isPortrait);
        mSelectedPlayer = (mPlayer > -1) ? nameIds[mPlayer] : nameIds[0];
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        int[] defaultNameIds = Constants.getPlayerDefaultNameIds(isPortrait);

        // "Game score" and "points value"
        int savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_SCORES, 0);
        ((TextView) findViewById(R.id.tv_game_score)).setText(String.valueOf(Constants.SCORES_VALUES[savedIndex]));
        savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_POINTS, 0);
        ((TextView) findViewById(R.id.tv_points_value)).setText(String.valueOf(Constants.POINTS_VALUES[savedIndex]));

        displayLives(gameMode, isPortrait);

        if (isPortrait) {
            // Show scores and indicate selected player
            if (gameMode == Constants.SINGLE_PLAYER) {
                // Show saved player name
                ((Button) findViewById(R.id.playerName1)).setText(shprefs.getString(Constants.SHPREFS_NAME_SINGLE_PLAYER,
                        getString(defaultNameIds[0])));
                ((Button) findViewById(R.id.playerName2)).setText("");

                // Highlight first player button
                ((ImageView) findViewById(R.id.iv_player_icon1)).setImageResource(R.drawable.player_icon_port_selected);
                ((ImageView) findViewById(R.id.iv_player_icon2)).setImageResource(0);
                findViewById(R.id.playerName1).setBackgroundResource(R.drawable.player_panel_selected);
                findViewById(R.id.playerName2).setBackgroundResource(0);

                ((TextView) findViewById(R.id.score_text1)).setText(getScore(shprefs.getInt(Constants.SHPREFS_SCORE_SINGLE, 0)));
                ((TextView) findViewById(R.id.score_text2)).setText("");

                findViewById(R.id.multiplayerBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.singlePlayerBtn).setVisibility(View.INVISIBLE);
            } else if (gameMode == Constants.MULTIPLAYER) {
                // Show saved player names
                for (int i = 0; i < Constants.getMaxPlayers(isPortrait); ++i) {
                    ((Button) findViewById(nameIds[i])).setText(
                            shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[i]),
                                    getString(defaultNameIds[i])));
                }

                selectPlayer(gameMode, isPortrait); // Highlight selected player button

                if (Utils.hasPlayerName(this, 0)) {
                    ((TextView) findViewById(R.id.score_text1)).setText(getScore(shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER1, 0)));
                } else {
                    ((TextView) findViewById(R.id.score_text1)).setText("");
                }
                if (Utils.hasPlayerName(this, 1)) {
                    ((TextView) findViewById(R.id.score_text2)).setText(getScore(shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER2, 0)));
                } else {
                    ((TextView) findViewById(R.id.score_text2)).setText("");
                }

                findViewById(R.id.singlePlayerBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.multiplayerBtn).setVisibility(View.INVISIBLE);
            } else {
                Log.e(TAG, "Unhandled game mode");
            }
        } else { // Landscape orientation
            // Show scores and indicate selected player
            if (gameMode == Constants.SINGLE_PLAYER) {
                // Show saved player name
                ((Button) findViewById(R.id.playerName1)).setText(shprefs.getString(Constants.SHPREFS_NAME_SINGLE_PLAYER,
                        getString(defaultNameIds[0])));
                ((Button) findViewById(R.id.playerName2)).setText("");
                ((Button) findViewById(R.id.playerName3)).setText("");
                ((Button) findViewById(R.id.playerName4)).setText("");

                // Highlight first player button
                ((ImageView) findViewById(R.id.iv_player_icon1)).setImageResource(R.drawable.player_icon_land);
                ((ImageView) findViewById(R.id.iv_player_icon2)).setImageResource(0);
                ((ImageView) findViewById(R.id.iv_player_icon3)).setImageResource(0);
                ((ImageView) findViewById(R.id.iv_player_icon4)).setImageResource(0);
                findViewById(R.id.playerName1).setBackgroundResource(R.drawable.player_panel_selected);
                findViewById(R.id.playerName2).setBackgroundResource(0);
                findViewById(R.id.playerName3).setBackgroundResource(0);
                findViewById(R.id.playerName4).setBackgroundResource(0);

                ((TextView) findViewById(R.id.score_text1)).setText(getScore(shprefs.getInt(Constants.SHPREFS_SCORE_SINGLE, 0)));
                ((TextView) findViewById(R.id.score_text2)).setText("");
                ((TextView) findViewById(R.id.score_text3)).setText("");
                ((TextView) findViewById(R.id.score_text4)).setText("");

                findViewById(R.id.multiplayerBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.singlePlayerBtn).setVisibility(View.INVISIBLE);
            } else if (gameMode == Constants.MULTIPLAYER) {
                // Show saved player names
                for (int i = 0; i < Constants.getMaxPlayers(isPortrait); ++i) {
                    ((Button) findViewById(nameIds[i])).setText(
                            shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[i]),
                                    getString(defaultNameIds[i])));
                }

                selectPlayer(gameMode, isPortrait); // Highlight selected player button

                if (Utils.hasPlayerName(this, 0)) {
                    ((TextView) findViewById(R.id.score_text1)).setText(getScore(shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER1, 0)));
                    ((TextView) findViewById(R.id.lives_text1)).setText(String.valueOf(shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER1, Utils.getDefaultLives(shprefs))));
                } else {
                    ((TextView) findViewById(R.id.score_text1)).setText("");
                    ((TextView) findViewById(R.id.lives_text1)).setText("");
                }
                if (Utils.hasPlayerName(this, 1)) {
                    ((TextView) findViewById(R.id.score_text2)).setText(getScore(shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER2, 0)));
                    ((TextView) findViewById(R.id.lives_text2)).setText(String.valueOf(shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER2, Utils.getDefaultLives(shprefs))));
                } else {
                    ((TextView) findViewById(R.id.score_text2)).setText("");
                    ((TextView) findViewById(R.id.lives_text2)).setText("");
                }
                if (Utils.hasPlayerName(this, 2)) {
                    ((TextView) findViewById(R.id.score_text3)).setText(getScore(shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER3, 0)));
                    ((TextView) findViewById(R.id.lives_text3)).setText(String.valueOf(shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER3, Utils.getDefaultLives(shprefs))));
                } else {
                    ((TextView) findViewById(R.id.score_text3)).setText("");
                    ((TextView) findViewById(R.id.lives_text3)).setText("");
                }
                if (Utils.hasPlayerName(this, 3)) {
                    ((TextView) findViewById(R.id.score_text4)).setText(getScore(shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER4, 0)));
                    ((TextView) findViewById(R.id.lives_text4)).setText(String.valueOf(shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER4, Utils.getDefaultLives(shprefs))));
                } else {
                    ((TextView) findViewById(R.id.score_text4)).setText("");
                    ((TextView) findViewById(R.id.lives_text4)).setText("");
                }

                findViewById(R.id.singlePlayerBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.multiplayerBtn).setVisibility(View.INVISIBLE);
            } else {
                Log.e(TAG, "Unhandled game mode");
            }
        }
    }

    /**
     * Returns string score or "no scores" string if score is 0.
     *
     * @param score a player's score
     * @return a score string
     */
    private String getScore(int score) {
        return (score > 0) ? String.valueOf(score) : getString(R.string.no_score);
    }

    private void displayLives(int gameMode, boolean isPortrait) {
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        int lives = Utils.getDefaultLives(shprefs);
        if (gameMode == Constants.SINGLE_PLAYER) {
            ((TextView) findViewById(R.id.lives_text1)).setText(String.valueOf(shprefs.getInt(Constants.SHPREFS_LIVES_SINGLE_PLAYER, lives)));
            findViewById(R.id.lives_text2).setVisibility(View.INVISIBLE);
            if (!isPortrait) {
                ((TextView) findViewById(R.id.lives_text3)).setText("");
                ((TextView) findViewById(R.id.lives_text4)).setText("");
            }
        } else if (gameMode == Constants.MULTIPLAYER) {
            ((TextView) findViewById(R.id.lives_text1)).setText(String.valueOf(shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER1, lives)));
            ((TextView) findViewById(R.id.lives_text2)).setText(String.valueOf(shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER2, lives)));
            findViewById(R.id.lives_text2).setVisibility(View.VISIBLE);
            if (!isPortrait) {
                ((TextView) findViewById(R.id.lives_text3)).setText(String.valueOf(shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER3, lives)));
                ((TextView) findViewById(R.id.lives_text4)).setText(String.valueOf(shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER4, lives)));
            }
        } else {
            Log.e(TAG, "Unhandled game mode");
        }
    }

    /**
     * Highlights button corresponding to selected player.
     *
     * @param gameMode Constants game mode value
     */
    private void selectPlayer(int gameMode, boolean isPortrait) {
        int[] iconIds = Constants.getPlayerIconIds(isPortrait);
        int[] nameIds = Constants.getPlayerNameIds(isPortrait);
        if (gameMode == Constants.MULTIPLAYER) {
            if (isPortrait) {
                for (int i = 0; i < nameIds.length; ++i) {
                    int id = nameIds[i];
                    if (mSelectedPlayer == id) {
                        if (Utils.hasPlayerName(this, i)) {
                            ((ImageView) findViewById(iconIds[i])).setImageResource(R.drawable.player_icon_port_selected);
                        } else {
                            ((ImageView) findViewById(iconIds[i])).setImageResource(R.drawable.player_icon_blank);
                        }
                        findViewById(id).setBackgroundResource(R.drawable.player_panel_selected);
                    } else {
                        if (Utils.hasPlayerName(this, i)) {
                            ((ImageView) findViewById(iconIds[i])).setImageResource(R.drawable.player_icon_port);
                            findViewById(id).setBackgroundResource(0);
                        } else {
                            ((ImageView) findViewById(iconIds[i])).setImageResource(R.drawable.player_icon_blank);
                            findViewById(id).setBackgroundResource(0);
                        }
                    }
                }
            } else { // Landscape
                for (int i = 0; i < iconIds.length; ++i) {
                    int id = nameIds[i];
                    if (mSelectedPlayer == id) {
                        if (Utils.hasPlayerName(this, i)) {
                            ((ImageView) findViewById(iconIds[i])).setImageResource(R.drawable.player_icon_land);
                        } else {
                            ((ImageView) findViewById(iconIds[i])).setImageResource(R.drawable.player_icon_blank);
                        }
                        findViewById(id).setBackgroundResource(R.drawable.player_panel_selected);
                    } else {
                        if (Utils.hasPlayerName(this, i)) {
                            ((ImageView) findViewById(iconIds[i])).setImageResource(R.drawable.player_icon_land);
                            findViewById(id).setBackgroundResource(0);
                        } else {
                            ((ImageView) findViewById(iconIds[i])).setImageResource(R.drawable.player_icon_blank);
                            findViewById(id).setBackgroundResource(0);
                        }
                    }
                }
            }
        } // else single player mode, player 1 selected by default
    }

    /**
     * Handles UI button presses.
     *
     * @param v the clicked-on view
     */
    public void onClick(View v) {
        final boolean isPortrait = Utils.isPortrait(this);
        final SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = shprefs.edit();
        final int gameMode = shprefs.getInt(Constants.SHPREFS_GAME_MODE, Constants.DEFAULT_GAME_MODE);
        int[] nameIds = Constants.getPlayerNameIds(isPortrait);
        int[] defaultNames = Constants.getPlayerDefaultNameIds(isPortrait);
        switch (v.getId()) {
            case R.id.playerName1:
                mSelectedPlayer = nameIds[0];
                selectPlayer(gameMode, isPortrait);
                break;
            case R.id.playerName2:
                mSelectedPlayer = nameIds[1];
                selectPlayer(gameMode, isPortrait);
                break;
            case R.id.playerName3:
                mSelectedPlayer = nameIds[2];
                selectPlayer(gameMode, isPortrait);
                break;
            case R.id.playerName4:
                mSelectedPlayer = nameIds[3];
                selectPlayer(gameMode, isPortrait);
                break;
            case R.id.confirmBtn:
                EditText editText = findViewById(R.id.enterNameText);
                String name = editText.getText().toString();
                if (name.length() > 0) {
                    ((Button) findViewById(mSelectedPlayer)).setText(name);
                    if (gameMode == Constants.SINGLE_PLAYER) {
                        editor.putString(Constants.SHPREFS_NAME_SINGLE_PLAYER, name); // Save name
                    } else if (gameMode == Constants.MULTIPLAYER) {
                        editor.putString(Constants.getPlayerNameStorageIds(isPortrait).get(mSelectedPlayer), name); // Save name
                    } else {
                        Log.e(TAG, "Unhandled game mode");
                    }
                    editor.apply();
                    editText.setText(""); // clear input
                    initScorecard(gameMode, isPortrait); // add empty score
                } // else invalid name
                break;
            case R.id.restartGameBtn:
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.confirm_restart))
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPlayer = -1;
                                mTurn = -1;

                                // Reset game settings
                                editor.putInt(Constants.SHPREFS_FIRST_PLAYER, Constants.NO_PLAYER);
//                                editor.putInt(Constants.SHPREFS_SETTINGS_LIVES, 0);
//                                editor.putInt(Constants.SHPREFS_SETTINGS_SCORES, 0);
//                                editor.putInt(Constants.SHPREFS_SETTINGS_POINTS, 0);

                                int lives = Utils.getDefaultLives(shprefs);
                                if (gameMode == Constants.SINGLE_PLAYER) {
                                    editor.putInt(Constants.SHPREFS_SCORE_SINGLE, 0);
                                    editor.putInt(Constants.SHPREFS_LIVES_SINGLE_PLAYER, lives);
                                } else {
                                    editor.putInt(Constants.SHPREFS_SCORE_PLAYER1, 0);
                                    editor.putInt(Constants.SHPREFS_SCORE_PLAYER2, 0);
                                    editor.putInt(Constants.SHPREFS_LIVES_PLAYER1, lives);
                                    editor.putInt(Constants.SHPREFS_LIVES_PLAYER2, lives);
                                    if (!isPortrait) {
                                        editor.putInt(Constants.SHPREFS_SCORE_PLAYER3, 0);
                                        editor.putInt(Constants.SHPREFS_SCORE_PLAYER4, 0);
                                        editor.putInt(Constants.SHPREFS_LIVES_PLAYER3, lives);
                                        editor.putInt(Constants.SHPREFS_LIVES_PLAYER4, lives);
                                    }
                                }
                                editor.apply();
                                initScorecard(gameMode, Utils.isPortrait(MainActivity.this));

                                getPacksToPromote();
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
            case R.id.playBtn:
                if (mSelectedTopic > -1) {
                    if (gameMode == Constants.SINGLE_PLAYER || Utils.arePlayersSet(this)) {
                        ArrayList<String> winners = getWinners(gameMode);
                        int firstPlayer = shprefs.getInt(Constants.SHPREFS_FIRST_PLAYER, Constants.NO_PLAYER);
                        if ((mPlayer == -1 || mPlayer == firstPlayer) && mTurn < 1 && winners != null) {
                            showGameResult(winners);
                        } else {
                            int defaultLives = Utils.getDefaultLives(shprefs);
                            if (gameMode == Constants.SINGLE_PLAYER && shprefs.getInt(Constants.SHPREFS_LIVES_SINGLE_PLAYER, defaultLives) < 1) {
                                // No more lives, game over
                                Toast.makeText(this, getString(R.string.game_over), Toast.LENGTH_SHORT).show();
                            } else if (gameMode == Constants.MULTIPLAYER &&
                                    ((mPlayer > -1 && Utils.getMultiplayerLives(shprefs, mPlayer) < 1) ||
                                            (mPlayer == -1 && Utils.getMultiplayerLives(shprefs, 0) < 1))) {
                                // No more lives, next player's turn
                                // Sound effect for changing player
                                float vol = shprefs.getInt(Constants.SHPREFS_SOUND_EFFECTS_VOL, 100) / 100f;
                                if (mMediaPlayer != null) {
                                    mMediaPlayer.reset();
                                }
                                mMediaPlayer = MediaPlayer.create(this, R.raw.game_show_buzz_in_10);
                                mMediaPlayer.setVolume(vol, vol);
                                mMediaPlayer.start();

                                Toast.makeText(this, getString(R.string.no_lives), Toast.LENGTH_SHORT).show();
                                mPlayer = (mPlayer != -1) ? mPlayer : 0;
                                mPlayer = (mPlayer == Constants.getMaxPlayers(Utils.isPortrait(this)) - 1) ? 0 : mPlayer + 1;
                                while (!Utils.hasPlayerName(this, mPlayer)) {
                                    mPlayer = (mPlayer == Constants.getMaxPlayers(Utils.isPortrait(this)) - 1) ? 0 : mPlayer + 1;
                                }
                                mSelectedPlayer = nameIds[mPlayer];
                                mTurn = 0;
                                selectPlayer(gameMode, Utils.isPortrait(this));
                            } else {
                                // Sound effect
                                float vol = shprefs.getInt(Constants.SHPREFS_SOUND_EFFECTS_VOL, 100) / 100f;
                                if (mMediaPlayer != null) {
                                    mMediaPlayer.reset();
                                }
                                mMediaPlayer = MediaPlayer.create(this, R.raw.general_whoosh01);
                                mMediaPlayer.setVolume(vol, vol);
                                mMediaPlayer.start();

                                // No more clicking topics
                                ResizingTextView btn = findViewById(R.id.topicBtn1);
                                btn.setEnabled(false);
                                btn = findViewById(R.id.topicBtn2);
                                btn.setEnabled(false);
                                if (!isPortrait) {
                                    btn = findViewById(R.id.topicBtn3);
                                    btn.setEnabled(false);
                                    btn = findViewById(R.id.topicBtn4);
                                    btn.setEnabled(false);
                                }

                                try { // wait for sound effect to finish
                                    Thread.sleep(750);
                                } catch (InterruptedException e) {
                                    Log.e(TAG, e.toString());
                                }
                                if (firstPlayer == Constants.NO_PLAYER) { // set starting player
                                    int p = (mPlayer != -1) ? mPlayer : 0;
                                    editor.putInt(Constants.SHPREFS_FIRST_PLAYER, p);
                                    editor.apply();
                                }
                                // Start game
                                Intent i = new Intent(getApplicationContext(), GameLoopActivity.class);
                                i.putExtra(TOPIC_ID, mTopics[mSelectedTopic].getTopicId());
                                i.putExtra(TOPIC_SUB_ID, mTopics[mSelectedTopic].getIsUserTopic());
                                i.putExtra(QUESTION_ID, mQuestionId);
                                if (mPlayer > -1) { // pass next player to play back, if any
                                    i.putExtra(PLAYER_TO_PLAY, mPlayer);
                                    i.putExtra(PLAYER_TURN, mTurn);
                                } // else not started from game loop, start new game
                                startActivity(i);
                                finish();
                            }
                        }
                    } else if (gameMode == Constants.MULTIPLAYER) { // multiplayer mode but player names are not set
                        Toast.makeText(this, getString(R.string.error_player_names), Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Unhandled game mode");
                    }
                } else {
                    Toast.makeText(this, getString(R.string.error_select_topic), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.previousBtn:
                startActivity(new Intent(getApplicationContext(), RulesOffersActivity.class));
                break;
            case R.id.nextBtn:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                break;
            case R.id.multiplayerBtn: // change game mode
                editor.putInt(Constants.SHPREFS_GAME_MODE, Constants.MULTIPLAYER);
                // Clear single player names and scores and lives
                editor.putString(Constants.SHPREFS_NAME_SINGLE_PLAYER, getString(defaultNames[0]));
                editor.putInt(Constants.SHPREFS_SCORE_SINGLE, 0);
                editor.putInt(Constants.SHPREFS_LIVES_SINGLE_PLAYER, Utils.getDefaultLives(shprefs));
                editor.putInt(Constants.SHPREFS_FIRST_PLAYER, Constants.NO_PLAYER);
                editor.apply();

                initScorecard(Constants.MULTIPLAYER, isPortrait);
                break;
            case R.id.singlePlayerBtn: // change game mode
                editor.putInt(Constants.SHPREFS_GAME_MODE, Constants.SINGLE_PLAYER);
                // Clear multiplayer names and scores and lives
                for (int i = 0; i < Constants.getMaxPlayers(isPortrait); ++i) {
                    editor.putString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[i]), getString(defaultNames[i]));
                    editor.putInt(Constants.getPlayerScoreKeys(isPortrait)[i], 0);
                }
                int lives = Utils.getDefaultLives(shprefs);
                editor.putInt(Constants.SHPREFS_LIVES_PLAYER1, lives);
                editor.putInt(Constants.SHPREFS_LIVES_PLAYER2, lives);
                editor.putInt(Constants.SHPREFS_LIVES_PLAYER3, lives);
                editor.putInt(Constants.SHPREFS_LIVES_PLAYER4, lives);
                editor.putInt(Constants.SHPREFS_FIRST_PLAYER, Constants.NO_PLAYER);
                editor.apply();

                initScorecard(Constants.SINGLE_PLAYER, isPortrait);
                break;
            case R.id.topicBtn1:
                selectTopic(0, R.id.topicBtn1);
                break;
            case R.id.topicBtn2:
                selectTopic(1, R.id.topicBtn2);
                break;
            case R.id.topicBtn3:
                selectTopic(2, R.id.topicBtn3);
                break;
            case R.id.topicBtn4:
                selectTopic(3, R.id.topicBtn4);
                break;
            default:
                Log.e(TAG, "Unhandled button press");
                break;
        }
    }

    private void selectTopic(int topicId, int btnId) {
        mQuestionId = Math.min(topicId + 1, Constants.QUESTIONS_PER_TOPIC) - 1; // 0, 1, 2, 2

        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        // Sound effect
        float vol = shprefs.getInt(Constants.SHPREFS_SOUND_EFFECTS_VOL, 100) / 100f;
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
        mMediaPlayer = MediaPlayer.create(this, R.raw.general_whoosh01);
        mMediaPlayer.setVolume(vol, vol);
        mMediaPlayer.start();

        mSelectedTopic = topicId;
        if (Utils.isPortrait(this)) {
            findViewById(R.id.topicBtn1).setBackgroundResource(R.drawable.yellow_rounded_rectangle);
            findViewById(R.id.topicBtn2).setBackgroundResource(R.drawable.yellow_rounded_rectangle);
            findViewById(btnId).setBackgroundResource(R.drawable.topic_selected_port);
        } else {
            findViewById(R.id.topicBtn1).setBackgroundResource(R.drawable.blue_rectangle);
            findViewById(R.id.topicBtn2).setBackgroundResource(R.drawable.blue_rectangle);
            findViewById(R.id.topicBtn3).setBackgroundResource(R.drawable.blue_rectangle);
            findViewById(R.id.topicBtn4).setBackgroundResource(R.drawable.blue_rectangle);
            findViewById(btnId).setBackgroundResource(R.drawable.topic_selected);
        }
    }

    /**
     * Gets array of winners or null if game is not over yet.
     */
    @SuppressWarnings("ConstantConditions")
    private ArrayList<String> getWinners(int gameMode) {
        ArrayList<String> winners = null;
        final SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        int gameScore = Utils.getGameScore(shprefs);
        if (gameMode == Constants.SINGLE_PLAYER && shprefs.getInt(Constants.SHPREFS_SCORE_SINGLE, 0) >= gameScore) {
            // Single player won
            winners = new ArrayList<>();
            winners.add(shprefs.getString(Constants.SHPREFS_NAME_SINGLE_PLAYER,
                    getString(Constants.getPlayerDefaultNameIds(Utils.isPortrait(this))[0])));
        } else if (gameMode == Constants.MULTIPLAYER) {
            boolean isMet; // is game over?
            if (Utils.isPortrait(this)) {
                isMet = (shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER1, 0) >= gameScore) ||
                        (shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER2, 0) >= gameScore);
            } else {
                isMet = (shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER1, 0) >= gameScore) ||
                        (shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER2, 0) >= gameScore) ||
                        (shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER3, 0) >= gameScore) ||
                        (shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER4, 0) >= gameScore);
            }
            if (isMet) {
                // Game over
                boolean isPortrait = Utils.isPortrait(this);
                int[] defaultNameIds = Constants.getPlayerDefaultNameIds(isPortrait);
                int[] nameIds = Constants.getPlayerNameIds(isPortrait);
                winners = new ArrayList<>();
                int score1 = shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER1, 0);
                int score2 = shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER2, 0);
                // This could be better...
                if (score1 >= gameScore && score1 > score2) { // {1}
                    winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[0]), getString(defaultNameIds[0])));
                } else if (score2 >= gameScore && score2 > score1) { // {2}
                    winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[1]), getString(defaultNameIds[1])));
                } else if (score1 >= gameScore && score1 == score2) { // {1, 2}
                    winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[0]), getString(defaultNameIds[0])));
                    winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[1]), getString(defaultNameIds[1])));
                }
                if (!isPortrait) {
                    int score3 = shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER3, 0);
                    int score4 = shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER4, 0);
                    if (score3 >= gameScore && score3 > score4 && score3 > score1 && score3 > score2) { // {3}
                        winners.clear();
                        winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[2]), getString(defaultNameIds[2])));
                    } else if (score4 >= gameScore && score4 > score3 && score4 > score1 && score4 > score2) { // {4}
                        winners.clear();
                        winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[3]), getString(defaultNameIds[3])));
                    } else if (score3 >= gameScore && score3 == score4 && (score3 > score1 && score3 > score2)) { // {3, 4}
                        winners.clear();
                        winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[2]), getString(defaultNameIds[2])));
                        winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[3]), getString(defaultNameIds[3])));
                    } else if ((score3 >= gameScore && score3 > score4 && score3 == score1 && score1 > score2) || // {1, 3}, {2, 3}, {1, 2, 3}
                            (score3 >= gameScore && score3 > score4 && score3 == score2 && score2 > score1) ||
                            (score3 >= gameScore && score3 > score4 && score3 == score2 && score2 == score1)) {
                        winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[2]), getString(defaultNameIds[2])));
                    } else if ((score4 >= gameScore && score4 > score3 && score4 == score1 && score1 > score2) || // {1, 4}, {2, 4}, {1, 2, 4}
                            (score4 >= gameScore && score4 > score3 && score4 == score2 && score2 > score1) ||
                            (score4 >= gameScore && score4 > score3 && score4 == score2 && score2 == score1)) {
                        winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[3]), getString(defaultNameIds[3])));
                    } else if (score3 >= gameScore && score3 == score4 &&
                            ((score3 == score1 && score1 > score2) || (score3 == score2 && score2 > score1))) { // {1, 3, 4}, {2, 3, 4}
                        winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[2]), getString(defaultNameIds[2])));
                        winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[3]), getString(defaultNameIds[3])));
                    } else if (score3 == score4 && score3 == score1 && score3 == score2) { // {1, 2, 3, 4}
                        winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[2]), getString(defaultNameIds[2])));
                        winners.add(shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(nameIds[3]), getString(defaultNameIds[3])));
                    }
                }
            }
        } else if (gameMode != Constants.SINGLE_PLAYER) {
            Log.e(TAG, "Unhandled game mode");
        }
        return winners;
    }

    private void showGameResult(ArrayList<String> winners) {
        final Dialog dialog = new Dialog(this);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(true);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            if (winners.size() > 1) {
                // Draw
                dialog.setContentView(R.layout.dialog_draw);
                ((TextView) dialog.findViewById(R.id.tv_draw1)).setText(winners.get(0));
                ((TextView) dialog.findViewById(R.id.tv_draw2)).setText(winners.get(1));
                if (winners.size() > 2) {
                    ((TextView) dialog.findViewById(R.id.tv_draw3)).setText(winners.get(2));
                }
                if (winners.size() > 3) {
                    ((TextView) dialog.findViewById(R.id.tv_draw4)).setText(winners.get(3));
                }
            } else {
                dialog.setContentView(R.layout.dialog_winner);
                ((TextView) dialog.findViewById(R.id.tv_winner)).setText(winners.get(0));
            }

            dialog.show();
        } else {
            Log.e(TAG, "Null dialog window");
        }
    }


    /**
     * Allows user to post Player 1's most recent score to their Facebook account.
     */
    /*private void postScore() { // TODO use me. NOTE: if you use this function you do not need Facebook login on the main page. This will have the user log in separately
        final SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        Button postScoresBtn = findViewById(R.id.postScoresBtn); // TODO make button
        final ShareDialog shareDialog = new ShareDialog(this);
        postScoresBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ShareDialog.canShow(ShareLinkContent.class)) {
                    ShareLinkContent content = new ShareLinkContent.Builder()
                            .setContentUrl(Uri.parse(getString(R.string.app_website))) // comment this out to remove website URL from post
                            .setQuote("Check out my score! " + getScore(shprefs.getInt(Constants.SHPREFS_SCORE_PLAYER1, 0))) // FIXME this text appears with website link
                            .build();
                    shareDialog.show(content);
                }
            }
        });
    }*/


    class PageSwitchTask extends TimerTask {
        private int mNumPages = 0;

        PageSwitchTask(int numPages) {
            mNumPages = numPages;
        }

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                public void run() { // infinite page loop
                    if (mCurrentPage >= mNumPages) {
                        mCurrentPage = 0;
                    } else {
                        mCurrentPage += 1;
                    }
                    mViewPager.setCurrentItem(mCurrentPage);
                }
            });
        }
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment holding 1 or 2 topic categories.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // Called to instantiate the fragment for the given section
            return CategoryTopicsFragment.newInstance(position, mIsPortrait);
        }

        @Override
        public int getCount() {
            return (mIsPortrait) ? mCats.length : mCats.length / 2 + (mCats.length % 2); // max pages, half as many on landscape
        }
    }


    /**
     * A fragment containing topic categories.
     */
    public static class CategoryTopicsFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this fragment.
         */
        private static final String ARG_SECTION_NUMBER = "SECTION_NUMBER";

        private static boolean mIsPortrait = true;

        public CategoryTopicsFragment() {
            // Required empty public constructor
        }

        /**
         * Returns a new instance of this fragment for the given section.
         */
        public static CategoryTopicsFragment newInstance(int sectionNumber, boolean isPortrait) {
            mIsPortrait = isPortrait;

            CategoryTopicsFragment fragment = new CategoryTopicsFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_topic_categories, container, false);
            Bundle bundle = getArguments();
            if (bundle != null) {
                int pageNum = bundle.getInt(ARG_SECTION_NUMBER);
                final int index = (mIsPortrait) ? pageNum : pageNum * 2;

                if (((index + 1) % TOPIC_ENTRY_PANEL_SLOT2 == 0) || (index + 1) % TOPIC_ENTRY_PANEL_SLOT2 == TOPIC_ENTRY_PANEL_SLOT1) {
                    // Display topic input dialog every third and tenth panel
                    RelativeLayout layout;
                    Button openBtn;
                    ImageView cancelBtn;
                    if (mIsPortrait) {
                        layout = rootView.findViewById(R.id.layout_topic_entry);
                        openBtn = rootView.findViewById(R.id.btn_open);
                        cancelBtn = rootView.findViewById(R.id.tv_cancel2);
                    } else { // landscape
                        layout = rootView.findViewById(R.id.layout_topic_entry1);
                        openBtn = rootView.findViewById(R.id.btn_open1);
                        cancelBtn = rootView.findViewById(R.id.tv_cancel1);
                    }

                    layout.setVisibility(View.VISIBLE);

                    // Expand the dialog
                    openBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showTopicEntryDialog(getActivity());
                            cancelTimer();
                        }
                    });

                    cancelBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            closePanels(getActivity());
                        }
                    });

                    if (!mIsPortrait) {
                        // Display additional category, if any
                        if (index < mCats.length - 1) {
                            final TopicCategory cat3 = mCats[index + 1];
                            ((TextView) rootView.findViewById(R.id.tv_title_cat3)).setText(cat3.getCategoryName());

                            ImageView iv3 = rootView.findViewById(R.id.iv_logo_cat3);
                            iv3.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    cancelTimer();
                                    promptPurchasePack(getActivity(), cat3);
                                }
                            });
                            Bitmap bmp = loadImage(cat3.getIcon());
                            if (bmp != null) {
                                iv3.setImageBitmap(bmp);
                                rootView.findViewById(R.id.loader3).setVisibility(View.INVISIBLE);
                            } else {
                                rootView.findViewById(R.id.loader3).setVisibility(View.VISIBLE);
                            }
                        }
                    }
                } else {
                    // Display topic categories
                    final TopicCategory cat1 = mCats[index];
                    ((TextView) rootView.findViewById(R.id.tv_title_cat1)).setText(cat1.getCategoryName());
                    ImageView iv1 = rootView.findViewById(R.id.iv_logo_cat1);
                    iv1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            cancelTimer();
                            promptPurchasePack(getActivity(), cat1);
                        }
                    });
                    Bitmap bmp = loadImage(cat1.getIcon());
                    if (bmp != null) {
                        iv1.setImageBitmap(bmp);
                        rootView.findViewById(R.id.loader1).setVisibility(View.INVISIBLE);
                    } else {
                        rootView.findViewById(R.id.loader1).setVisibility(View.VISIBLE);
                    }

                    if (!mIsPortrait) {
                        // Display additional category, if any
                        if (index < mCats.length - 1) {
                            if ((index + 2) % TOPIC_ENTRY_PANEL_SLOT2 == 0) { // topic entry
                                rootView.findViewById(R.id.layout_topic_entry2).setVisibility(View.VISIBLE);

                                // Expand the dialog
                                rootView.findViewById(R.id.btn_open2).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        showTopicEntryDialog(getActivity());
                                        cancelTimer();
                                    }
                                });

                                rootView.findViewById(R.id.tv_cancel2).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        closePanels(getActivity());
                                    }
                                });
                            } else {
                                final TopicCategory cat3 = mCats[index + 1];
                                ((TextView) rootView.findViewById(R.id.tv_title_cat3)).setText(cat3.getCategoryName());

                                ImageView iv3 = rootView.findViewById(R.id.iv_logo_cat3);
                                iv3.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        cancelTimer();
                                        promptPurchasePack(getActivity(), cat3);
                                    }
                                });
                                bmp = loadImage(cat3.getIcon());
                                if (bmp != null) {
                                    iv3.setImageBitmap(bmp);
                                    rootView.findViewById(R.id.loader3).setVisibility(View.INVISIBLE);
                                } else {
                                    rootView.findViewById(R.id.loader3).setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            rootView.findViewById(R.id.layout_cat2).setVisibility(View.INVISIBLE);
                        }
                    }

                    if (mIsPortrait && pageNum % 2 != 0) { // allow canceling only on every other page
                        rootView.findViewById(R.id.tv_cancel).setVisibility(View.INVISIBLE);
                    } else {
                        rootView.findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                closePanels(getActivity());
                            }
                        });
                    }
                }
            } else {
                Log.e(TAG, "Null arguments");
            }

            return rootView;
        }
    }


    private class IabListener implements IabBroadcastReceiver.IabBroadcastListener {
        @Override
        public void receivedBroadcast() {
            // Called when a broadcast notification of a change in inventory items is received
            Log.d(TAG, "receivedBroadcast");
            try {
                mHelper.queryInventoryAsync(mGotInventoryListener);
            } catch (IabHelper.IabAsyncInProgressException e) {
                Utils.alertUser(MainActivity.this, getString(R.string.error_inventory));
            }
        }
    }
}
