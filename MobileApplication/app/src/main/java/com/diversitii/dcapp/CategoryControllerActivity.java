package com.diversitii.dcapp;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.diversitii.dcapp.billing.IabBroadcastReceiver;
import com.diversitii.dcapp.billing.IabHelper;
import com.diversitii.dcapp.billing.IabResult;
import com.diversitii.dcapp.billing.Inventory;
import com.diversitii.dcapp.billing.Purchase;
import com.diversitii.dcapp.database.Category;
import com.diversitii.dcapp.database.CategoryDao;
import com.diversitii.dcapp.database.PacksDao;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

/**
 * Category control functions.
 */
public class CategoryControllerActivity extends MusicActivity {
    private static final String TAG = CategoryControllerActivity.class.getName();
    private static final int RC_REQUEST = 12103; // (arbitrary) request code for the purchase flow (permissible range is restricted)

    // In-app billing
    private String mSelectedCat;
    private int mSelectedCatId;
    //    private int mSelectedMultipack = 0;
//    private boolean mIsMultipackPurchase = false;
    private @Constants.Membership
    int mMembership = Constants.MEMBERSHIP_INVALID;
    private IabHelper mHelper;
    private IabBroadcastReceiver mBroadcastReceiver; // listen for purchase notifications while Activity is running

    // Listener called when inventory querying is done
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "onQueryInventoryFinished result: " + result);
            setLoadScreen(false);

            if (mHelper == null) return;  // Quit if helper was disposed of

            if (result.isFailure()) {
                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_inventory2, result));
                return;
            }

            // Check memberships
            Purchase membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_DIAMOND_ANNUAL));
            if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                Utils.checkUpdateMembership(CategoryControllerActivity.this, Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
            } else {
                membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_DIAMOND_BIANNUAL));
                if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                    Utils.checkUpdateMembership(CategoryControllerActivity.this, Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
                } else {
                    membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_DIAMOND_QUARTERLY));
                    if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                        Utils.checkUpdateMembership(CategoryControllerActivity.this, Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
                    }
                }
            }
            membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_RED_DIAMOND_ANNUAL));
            if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                Utils.checkUpdateMembership(CategoryControllerActivity.this, Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
            } else {
                membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_RED_DIAMOND_BIANNUAL));
                if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                    Utils.checkUpdateMembership(CategoryControllerActivity.this, Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
                } else {
                    membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_RED_DIAMOND_QUARTERLY));
                    if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                        Utils.checkUpdateMembership(CategoryControllerActivity.this, Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
                    }
                }
            }
        }
    };

    // Listener called when a purchase completes
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "onIabPurchaseFinished result: " + result + "; purchase: " + purchase);
            setLoadScreen(false);

            if (mHelper == null) return; // Quit if helper was disposed of

            if (result.isFailure()) {
                if (!result.getMessage().contains("User canceled")) {
                    Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_purchasing, result));
                }
                return;
            }
            if (!Utils.verifyDeveloperPayload(purchase)) {
                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_purchasing2));
                return;
            }
            // Purchase success
            /*if (mIsMultipackPurchase) {
                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.purchased_multipack));

                PacksDao packsDao = new PacksDao();
                SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = shprefs.edit();
                for (int i = (mSelectedMultipack - 1) * Constants.PACKS_PER_MULTIPACK + 1; i <= mSelectedMultipack * Constants.PACKS_PER_MULTIPACK; ++i) {
                    // Unlock pack
                    packsDao.setIsOwned(CategoryControllerActivity.this, i);
                    // Unlock pack's category, if any
                    int catId = packsDao.getCategoryId(CategoryControllerActivity.this, i);
                    if (catId != Constants.DEFAULT_CAT_ID) {
                        new CategoryDao().setIsOwned(CategoryControllerActivity.this, catId);
                    } else {
                        Log.d(TAG, "Bought pack had no category");
                    }
                    Utils.installPack(CategoryControllerActivity.this, i);
                    // Reset pack update time to reload previously purchased topics (if deleted)
//                    editor.putLong(Constants.getTopicsUpdateTime(i), 0);
                }
                editor.putBoolean(Constants.getMultipackKey(mSelectedMultipack), true);
                editor.apply();

                initUi();
            } else */
            if (purchase.getSku().equals(Constants.getCatPackSku(mSelectedCatId))) { // category purchase
                // Unlock all packs with purchased category, or all random packs if no category
                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.purchased_pack));

                // Unlock pack's category, if any
                PacksDao packsDao = new PacksDao();
                if (mSelectedCatId != Constants.DEFAULT_CAT_ID) {
                    new CategoryDao().setIsOwned(CategoryControllerActivity.this, mSelectedCatId);
                } else {
                    Log.d(TAG, "Bought random pack");
                }
                // Unlock packs
                packsDao.setCategoryOwned(CategoryControllerActivity.this, mSelectedCatId);
                Utils.installCategory(CategoryControllerActivity.this, mSelectedCatId);

                initUi();
            } else if (mMembership != Constants.MEMBERSHIP_INVALID) { // Membership purchase
                // Unlock all current packs
                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.purchased_membership));

                SharedPreferences.Editor editor = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE).edit();
                int lastPack = 1;
                switch (mMembership) {
                    case Constants.MEMBERSHIP_COPPER:
                        lastPack = Constants.MEMBERSHIP_COPPER_LAST_PACK;
                        editor.putBoolean(Constants.SHPREFS_BOUGHT_COPPER, true);
                        break;
                    case Constants.MEMBERSHIP_BRONZE:
                        lastPack = Constants.MEMBERSHIP_BRONZE_LAST_PACK;
                        editor.putBoolean(Constants.SHPREFS_BOUGHT_BRONZE, true);
                        break;
                    case Constants.MEMBERSHIP_SILVER:
                        lastPack = Constants.MEMBERSHIP_SILVER_LAST_PACK;
                        editor.putBoolean(Constants.SHPREFS_BOUGHT_SILVER, true);
                        break;
                    case Constants.MEMBERSHIP_GOLD:
                        lastPack = Constants.MEMBERSHIP_GOLD_LAST_PACK;
                        editor.putBoolean(Constants.SHPREFS_BOUGHT_GOLD, true);
                        break;
                    case Constants.MEMBERSHIP_DIAMOND_ANNUAL:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(CategoryControllerActivity.this,
                                Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_DIAMOND_BIANNUAL:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(CategoryControllerActivity.this,
                                Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_DIAMOND_QUARTERLY:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(CategoryControllerActivity.this,
                                Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_RED_DIAMOND_ANNUAL:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(CategoryControllerActivity.this,
                                Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_RED_DIAMOND_BIANNUAL:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(CategoryControllerActivity.this,
                                Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_RED_DIAMOND_QUARTERLY:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(CategoryControllerActivity.this,
                                Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_INVALID:
                    default:
                        Log.e("Constants", "Unhandled membership option: " + mMembership);
                        Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_membership));
                        break;
                }
                editor.apply();
                if (mMembership == Constants.MEMBERSHIP_COPPER
                        || mMembership == Constants.MEMBERSHIP_BRONZE
                        || mMembership == Constants.MEMBERSHIP_SILVER
                        || mMembership == Constants.MEMBERSHIP_GOLD) { // unlock packs
                    PacksDao packsDao = new PacksDao();
                    for (int i = 1; i <= lastPack; ++i) { // 0 is the free pack!
                        // Unlock pack
                        packsDao.setIsOwned(CategoryControllerActivity.this, i);
                        // Unlock pack's category, if any
                        int catId = packsDao.getCategoryId(CategoryControllerActivity.this, i);
                        if (catId != Constants.DEFAULT_CAT_ID) {
                            new CategoryDao().setIsOwned(CategoryControllerActivity.this, catId);
                        } else {
                            Log.d(TAG, "Bought pack had no category");
                        }
                        Utils.installPack(CategoryControllerActivity.this, i);
                    }
                }

                initUi();
            } else {
                Log.e(TAG, "Unhandled SKU");
                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_purchasing, result));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_controller);
        boolean mIsAndroidTV = Utils.isAndroidTV(this);
        if(mIsAndroidTV) {
            //Toast.makeText(this, "This is Android TV", Toast.LENGTH_LONG).show();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            initLandscape();
        } else {
            if (Utils.isPortrait(this)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                initPortrait();
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                initLandscape();
                //Toast.makeText(this,"This is Tablet",Toast.LENGTH_LONG).show();
            }
        }


        // Let device volume buttons control sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Set up in-app billing (asynchronous)
        mHelper = new IabHelper(this, Constants.BASE64_PUBLIC_KEY);
        mHelper.enableDebugLogging(false); // set true for debugging
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_billing_setup, result));
                    return;
                }

                if (mHelper == null) return; // Quit if helper was disposed of

                // Register AFTER IabHelper is setup, but before first call to getPurchases()
                mBroadcastReceiver = new IabBroadcastReceiver(new IabListener());
                registerReceiver(mBroadcastReceiver, new IntentFilter(IabBroadcastReceiver.ACTION));
                try { // Check purchase history
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_inventory));
                }
            }
        });
    }

    private void initPortrait() {
        findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(CategoryControllerActivity.this);
                Window window = dialog.getWindow();
                if (window != null) {
                    // Fill screen
                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                    lp.copyFrom(window.getAttributes());
                    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                    lp.height = WindowManager.LayoutParams.MATCH_PARENT;

                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.setCancelable(false);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.dialog_new_topics);

                    initUiCont(dialog);

                    dialog.findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                    window.setAttributes(lp);
                } else {
                    Log.e(TAG, "Null dialog window");
                }
            }
        });

        initUi();
    }

    private void initLandscape() {
        initUi();
        initUiCont(null);
    }

    private void initUi() {
        // Game modes
        final SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = shprefs.edit();
        int gameMode = shprefs.getInt(Constants.SHPREFS_USER_GAME_MODE, Constants.GAME_MODE_1);
        switch (gameMode) {
            case Constants.GAME_MODE_1:
                findViewById(R.id.btn_mode1).setBackgroundResource(R.drawable.green_rounded_rectangle);
                break;
            case Constants.GAME_MODE_2:
                findViewById(R.id.btn_mode2).setBackgroundResource(R.drawable.green_rounded_rectangle);
                break;
            case Constants.GAME_MODE_3:
                findViewById(R.id.btn_mode3).setBackgroundResource(R.drawable.green_rounded_rectangle);
                break;
            default:
                Log.e(TAG, "Unhandled game mode");
                break;
        }
        findViewById(R.id.btn_mode1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt(Constants.SHPREFS_USER_GAME_MODE, Constants.GAME_MODE_1);
                editor.apply();
                findViewById(R.id.btn_mode1).setBackgroundResource(R.drawable.green_rounded_rectangle);
                findViewById(R.id.btn_mode2).setBackgroundResource(R.drawable.darker_red_rounded_rectangle);
                findViewById(R.id.btn_mode3).setBackgroundResource(R.drawable.darker_red_rounded_rectangle);
            }
        });
        findViewById(R.id.btn_mode2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt(Constants.SHPREFS_USER_GAME_MODE, Constants.GAME_MODE_2);
                editor.apply();
                findViewById(R.id.btn_mode1).setBackgroundResource(R.drawable.darker_red_rounded_rectangle);
                findViewById(R.id.btn_mode2).setBackgroundResource(R.drawable.green_rounded_rectangle);
                findViewById(R.id.btn_mode3).setBackgroundResource(R.drawable.darker_red_rounded_rectangle);
            }
        });
        findViewById(R.id.btn_mode3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt(Constants.SHPREFS_USER_GAME_MODE, Constants.GAME_MODE_3);
                editor.apply();
                findViewById(R.id.btn_mode1).setBackgroundResource(R.drawable.darker_red_rounded_rectangle);
                findViewById(R.id.btn_mode2).setBackgroundResource(R.drawable.darker_red_rounded_rectangle);
                findViewById(R.id.btn_mode3).setBackgroundResource(R.drawable.green_rounded_rectangle);
            }
        });

        // Categories list
        ListView list = findViewById(R.id.lv_cats);
        ArrayList<Category> cats = new CategoryDao().getOwnedCategories(this);
        final String[] catsArr = new String[cats.size()];
        final boolean[] areEnabled = new boolean[cats.size()];
        for (int i = 0; i < cats.size(); ++i) {
            catsArr[i] = cats.get(i).getCategoryText();
            areEnabled[i] = cats.get(i).isEnabled();
        }
        final ListItemArrayAdapter arrayAdapter = new ListItemArrayAdapter(this, getLayoutInflater(),
                catsArr, areEnabled, false);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                long viewId = view.getId();

                if (viewId == R.id.btn_on) {
                    areEnabled[index] = true;
                    new CategoryDao().setIsEnabled(CategoryControllerActivity.this, catsArr[index], true);
                } else if (viewId == R.id.btn_off) {
                    areEnabled[index] = false;
                    new CategoryDao().setIsEnabled(CategoryControllerActivity.this, catsArr[index], false);
                }
                arrayAdapter.notifyDataSetChanged();
                refreshUi();
            }
        });
        list.setAdapter(arrayAdapter);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CategoryControllerActivity.this, MainActivity.class));
                finish();
            }
        });

        refreshUi();
    }

    /**
     * Refreshes display of available and selected categories.
     */
    private void refreshUi() {
        ((TextView) findViewById(R.id.tv_available)).setText(String.valueOf(new CategoryDao().getTotalOwnedCategories(this)));
        ((TextView) findViewById(R.id.tv_selected)).setText(String.valueOf(new CategoryDao().getTotalOwnedEnabledCategories(this)));
    }

    /**
     * Sets up UI shown on landscape page and portrait dialog.
     *
     * @param dialog dialog to display UI on or null if UI update is on activity's layout
     */
    private void initUiCont(final Dialog dialog) {
        Utils.getOffers(CategoryControllerActivity.this, dialog, R.id.tv_notifications, R.id.offer_progress);

        // New topics list
        ListView list;
        if (dialog != null) {
            list = dialog.findViewById(R.id.lv_topics);
        } else {
            list = findViewById(R.id.lv_topics);
        }
        ArrayList<String> packs = new PacksDao().getPacksForSale(this);
        packs.add(0, getString(R.string.membership_options)); // Add membership dialog link
        ListItemArrayAdapter arrayAdapter = new ListItemArrayAdapter(this, getLayoutInflater(),
                packs.toArray(new String[packs.size()]), null, false);
        list.setAdapter(arrayAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                if (((TextView) view.findViewById(R.id.tv_item)).getText().equals(getString(R.string.membership_options))) {
                    // Membership options link
                    if (Utils.isPortrait(CategoryControllerActivity.this)) {
                        startActivity(new Intent(CategoryControllerActivity.this, MembershipActivity.class));
                        finish();
                    } else {
                        initMembershipDialog();
                    }
                } else { // category row
                    mSelectedCat = ((TextView) view.findViewById(R.id.tv_item)).getText().toString();
                    mSelectedCatId = (mSelectedCat.equals(getString(R.string.random_pack))) ? Constants.DEFAULT_CAT_ID :
                            new CategoryDao().getCategoryId(CategoryControllerActivity.this, mSelectedCat);
                    promptPurchaseCategory();
                }
            }
        });
    }

    /**
     * Initiates purchase process for category.
     */
    private void promptPurchaseCategory() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_purchase_cat_pack, mSelectedCat))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setLoadScreen(true);
                        try {
//                            mIsMultipackPurchase = false;
                            mHelper.launchPurchaseFlow(CategoryControllerActivity.this, Constants.getCatPackSku(mSelectedCatId),
                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_purchase));
                            setLoadScreen(false);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void initMembershipDialog() {
        final Dialog dialog = new Dialog(CategoryControllerActivity.this);
        Window window = dialog.getWindow();
        if (window != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(false);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_membership_options);

            initUi2(dialog);

//            // Get number of paid packs available for purchase
//            setLoadScreen(true);
//
//            FirebaseDatabase.getInstance()
//                    .getReference(Constants.METADATA_KEY)
//                    .addValueEventListener(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(DataSnapshot dataSnapshot) {
//                            // Called once with initial value and again whenever data at this location updates
//                            setLoadScreen(false);
////                            Long res = dataSnapshot.getValue(Long.class);
////                            initMultipacks((res != null) ? res.intValue() : 0, dialog);
//                            initUi2(dialog);
//                        }
//
//                        @Override
//                        public void onCancelled(DatabaseError error) {
//                            Log.w(TAG, "Error reading packs data", error.toException());
//                        }
//                    });

            Utils.getOffers(this, dialog, R.id.tv_notifications, R.id.offer_progress);

            // Back button
            dialog.findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        } else {
            Log.e(TAG, "Null dialog window");
        }
    }

    private void initUi2(final Dialog dialog) {
        setLoadScreen(true);

        // Get a list of Topic IDs from all random packs
        final ArrayList<Integer> topicIds = new ArrayList<>();
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_PACKS)
                .whereEqualTo(Constants.FIELD_CATEGORY_ID, Constants.DEFAULT_CAT_ID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                if (document != null && document.exists()) {
                                    try {
                                        String[] ids = document.getString(Constants.FIELD_TOPIC_IDS).split(Constants.TOPIC_ID_LIST_SEPARATOR);
                                        for (String id : ids) {
                                            int i = Integer.parseInt(id);
                                            if (!topicIds.contains(i)) {
                                                topicIds.add(i);
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Invalid ID! Pack " + document.getId());
                                    }
                                } else {
                                    Log.e(TAG, "No random packs found");
                                }
                            }

                            setLoadScreen(false);
                            initMembership(topicIds.size(), dialog);
                        } else {
                            Log.w(TAG, "Error getting packs: ", task.getException());
                        }
                    }
                });
    }

    /**
     * Sets up membership options. Purchase is disabled for memberships without sufficient topics
     * available and for memberships already purchased.
     *
     * @param numRandomTopics the number of random (no-category) topics available
     */
    private void initMembership(final int numRandomTopics, Dialog dialog) {
        Log.d(TAG, "Random topics: " + numRandomTopics);

        // Membership options list
        ListView list = dialog.findViewById(R.id.lv_cats);
        list.setAdapter(new ListItemArrayAdapter(this, getLayoutInflater(),
                new String[]{getString(R.string.membership_copper),
                        getString(R.string.membership_bronze),
                        getString(R.string.membership_silver),
                        getString(R.string.membership_gold),
                        getString(R.string.membership_diamond_annual),
                        getString(R.string.membership_diamond_biannual),
                        getString(R.string.membership_diamond_quarterly),
                        getString(R.string.membership_red_diamond_annual),
                        getString(R.string.membership_red_diamond_biannual),
                        getString(R.string.membership_red_diamond_quarterly)},
                null, true));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);

                String membership = ((TextView) view.findViewById(R.id.tv_item)).getText().toString();
                if (membership.equals(getString(R.string.membership_copper))) {
                    if (shprefs.getBoolean(Constants.SHPREFS_BOUGHT_COPPER, false)) {
                        Utils.alertUser(CategoryControllerActivity.this, getString(R.string.membership_owned));
                    } else {
                        mMembership = Constants.MEMBERSHIP_COPPER;
                        promptPurchaseMembership(view);
                    }
                } else if (membership.equals(getString(R.string.membership_bronze))) {
                    if (shprefs.getBoolean(Constants.SHPREFS_BOUGHT_BRONZE, false)) {
                        Utils.alertUser(CategoryControllerActivity.this, getString(R.string.membership_owned));
                    } else {
                        mMembership = Constants.MEMBERSHIP_BRONZE;
                        promptPurchaseMembership(view);
                    }
                } else if (membership.equals(getString(R.string.membership_silver))) {
                    if (shprefs.getBoolean(Constants.SHPREFS_BOUGHT_SILVER, false)) {
                        Utils.alertUser(CategoryControllerActivity.this, getString(R.string.membership_owned));
                    } else {
                        mMembership = Constants.MEMBERSHIP_SILVER;
                        promptPurchaseMembership(view);
                    }
                } else if (membership.equals(getString(R.string.membership_gold))) {
                    if (shprefs.getBoolean(Constants.SHPREFS_BOUGHT_GOLD, false)) {
                        Utils.alertUser(CategoryControllerActivity.this, getString(R.string.membership_owned));
                    } else {
                        mMembership = Constants.MEMBERSHIP_GOLD;
                        promptPurchaseMembership(view);
                    }
                } else { // subscription memberships
                    if (!(shprefs.getBoolean(Constants.SHPREFS_BOUGHT_COPPER, false)
                            && shprefs.getBoolean(Constants.SHPREFS_BOUGHT_BRONZE, false)
                            && shprefs.getBoolean(Constants.SHPREFS_BOUGHT_SILVER, false)
                            && shprefs.getBoolean(Constants.SHPREFS_BOUGHT_GOLD, false))) {
                        // Diamond memberships are only available when all lower memberships have been bought
                        Utils.alertUser(CategoryControllerActivity.this, getString(R.string.membership_locked));
                    } else if (shprefs.getBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, false)) {
                        Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_already_subscribed));
                    } else {
                        if (membership.equals(getString(R.string.membership_diamond_annual))) {
                            if (!Utils.hasSupplyForDiamond(CategoryControllerActivity.this, numRandomTopics, Constants.MONTHS_ANNUAL)) {
                                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_DIAMOND_ANNUAL;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else if (membership.equals(getString(R.string.membership_diamond_biannual))) {
                            if (!Utils.hasSupplyForDiamond(CategoryControllerActivity.this, numRandomTopics, Constants.MONTHS_BIANNUAL)) {
                                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_DIAMOND_BIANNUAL;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else if (membership.equals(getString(R.string.membership_diamond_quarterly))) {
                            if (!Utils.hasSupplyForDiamond(CategoryControllerActivity.this, numRandomTopics, Constants.MONTHS_QUARTERLY)) {
                                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_DIAMOND_QUARTERLY;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else if (membership.equals(getString(R.string.membership_red_diamond_annual))) {
                            if (!Utils.hasSupplyForRedDiamond(CategoryControllerActivity.this, numRandomTopics, Constants.MONTHS_ANNUAL)) {
                                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_RED_DIAMOND_ANNUAL;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else if (membership.equals(getString(R.string.membership_red_diamond_biannual))) {
                            if (!Utils.hasSupplyForRedDiamond(CategoryControllerActivity.this, numRandomTopics, Constants.MONTHS_BIANNUAL)) {
                                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_RED_DIAMOND_BIANNUAL;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else if (membership.equals(getString(R.string.membership_red_diamond_quarterly))) {
                            if (!Utils.hasSupplyForRedDiamond(CategoryControllerActivity.this, numRandomTopics, Constants.MONTHS_QUARTERLY)) {
                                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_RED_DIAMOND_QUARTERLY;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else {
                            Log.e(TAG, "Unhandled membership name " + membership);
                            Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_purchase));
                        }
                    }
                }
            }
        });
    }

//    /**
//     * Sets up a number of multipacks based on how many packs are available.
//     *
//     * @param numPacks the number of individual packs available
//     */
//    private void initMultipacks(int numPacks, Dialog dialog) {
//        if (numPacks >= Constants.PACKS_PER_MULTIPACK) {
//            final boolean[] isUnlocked = new boolean[numPacks / Constants.PACKS_PER_MULTIPACK];
//
//            SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
//            for (int i = 1; i <= isUnlocked.length; ++i) { // multipacks count from 1!!
//                isUnlocked[i - 1] = shprefs.getBoolean(Constants.getMultipackKey(i), false);
//            }
//            // Packs grid
//            GridView grid = dialog.findViewById(R.id.gv_multipacks);
//            PacksArrayAdapter arrayAdapter = new PacksArrayAdapter(this, getLayoutInflater(), isUnlocked, false);
//            grid.setAdapter(arrayAdapter);
//            grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
//                    if (!isUnlocked[index]) {
//                        mSelectedMultipack = index + 1;
//                        promptPurchaseMultiPack();
//                    } // else multipack is already unlocked, ignore
//                }
//            });
//        } else {
//            dialog.findViewById(R.id.tv_no_multipacks).setVisibility(View.VISIBLE);
//        }
//    }

    /**
     * Initiates purchase process for membership.
     */
    public void promptPurchaseMembership(View v) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_purchase_membership_type, Constants.getMembershipName(this, mMembership)))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setLoadScreen(true);
                        try {
//                            mIsMultipackPurchase = false;
                            mHelper.launchPurchaseFlow(CategoryControllerActivity.this,
                                    Constants.getMembershipSku(mMembership),
                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_purchase));
                            setLoadScreen(false);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

//    /**
//     * Initiates purchase process for multipacks.
//     */
//    void promptPurchaseMultiPack() {
//        PacksDao packsDao = new PacksDao();
//        String msg = getString(R.string.confirm_purchase_multipack);
//        for (int i = (mSelectedMultipack - 1) * Constants.PACKS_PER_MULTIPACK + 1;
//             i < mSelectedMultipack * Constants.PACKS_PER_MULTIPACK + 1; ++i) {
//            if (packsDao.getIsOwned(this, i)) {
//                msg = getString(R.string.warn_purchase_multipack);
//                break;
//            }
//        }
//
//        new AlertDialog.Builder(this)
//                .setMessage(msg)
//                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        setLoadScreen(true);
//                        try {
//                            mIsMultipackPurchase = true;
//                            mHelper.launchPurchaseFlow(CategoryControllerActivity.this,
//                                    Constants.getMultipackSku(mSelectedMultipack),
//                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
//                        } catch (IabHelper.IabAsyncInProgressException e) {
//                            Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_purchase));
//                            setLoadScreen(false);
//                        }
//                    }
//                })
//                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .show();
//    }

    /**
     * Initiates purchase process for subscription membership.
     */
    public void promptPurchaseMembershipSubscription(View v) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_purchase_membership_type, Constants.getMembershipName(this, mMembership)))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setLoadScreen(true);
                        try {
//                            mIsMultipackPurchase = false;
                            mHelper.launchSubscriptionPurchaseFlow(CategoryControllerActivity.this,
                                    Constants.getMembershipSku(mMembership),
                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_purchase));
                            setLoadScreen(false);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
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

    /**
     * Enables or disables the loading screen.
     */
    private void setLoadScreen(boolean set) {
        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
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
            mHelper.disposeWhenFinished();
            mHelper = null;
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
                Utils.alertUser(CategoryControllerActivity.this, getString(R.string.error_inventory));
            }
        }
    }
}
