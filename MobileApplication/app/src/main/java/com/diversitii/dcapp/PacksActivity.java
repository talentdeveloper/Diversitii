package com.diversitii.dcapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.diversitii.dcapp.billing.IabBroadcastReceiver;
import com.diversitii.dcapp.billing.IabHelper;
import com.diversitii.dcapp.billing.IabResult;
import com.diversitii.dcapp.billing.Inventory;
import com.diversitii.dcapp.billing.Purchase;
import com.diversitii.dcapp.database.CategoryDao;
import com.diversitii.dcapp.database.PacksDao;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Allow unlocking of additional topic packs and membership.
 */
public class PacksActivity extends MusicActivity {
    private static final String TAG = PacksActivity.class.getName();

    // In-app billing
    private int mNumPacks = 0;
    private int mSelectedPack = 0;
    private int mSelectedMultipack = 0;
    private boolean mIsMultipackPurchase = false;
    private IabHelper mHelper;
    private static final int RC_REQUEST = 12103; // (arbitrary) request code for the purchase flow (permissible range is restricted)
    private IabBroadcastReceiver mBroadcastReceiver; // listen for purchase notifications while Activity is running
    // Listener called when inventory querying is done
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "onQueryInventoryFinished result: " + result);
            setLoadScreen(false);

            if (mHelper == null) return;  // Quit if helper was disposed of

            if (result.isFailure()) {
                Utils.alertUser(PacksActivity.this, getString(R.string.error_inventory2, result));
                return;
            }

            // Check for purchased packs
            PacksDao packsDao = new PacksDao();
            for (int i = 1; i <= mNumPacks; ++i) { // packs count from 1!!
                Purchase pack = inventory.getPurchase(Constants.getPackSku(i));
                boolean isUnlocked = (pack != null && Utils.verifyDeveloperPayload(pack));
                if (isUnlocked) {
                    // Unlock pack
                    packsDao.setIsOwned(PacksActivity.this, i);
                    // Unlock pack's category, if any
                    int catId = packsDao.getCategoryId(PacksActivity.this, i);
                    if (catId != Constants.DEFAULT_CAT_ID) {
                        new CategoryDao().setIsOwned(PacksActivity.this, catId);
                    } else {
                        Log.d(TAG, "Bought pack had no category");
                    }
                } else {
                    Log.e(TAG, "Invalid pack: " + i);
                }
            }

            initPacks();
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
                    Utils.alertUser(PacksActivity.this, getString(R.string.error_purchasing, result));
                }
                return;
            }
            if (!Utils.verifyDeveloperPayload(purchase)) {
                Utils.alertUser(PacksActivity.this, getString(R.string.error_purchasing2));
                return;
            }
            // Purchase success
            if (mIsMultipackPurchase) {
                Utils.alertUser(PacksActivity.this, getString(R.string.purchased_multipack));

                PacksDao packsDao = new PacksDao();
                SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = shprefs.edit();
                for (int i = (mSelectedMultipack - 1) * Constants.PACKS_PER_MULTIPACK + 1; i <= mSelectedMultipack * Constants.PACKS_PER_MULTIPACK; ++i) {
                    // Unlock pack
                    packsDao.setIsOwned(PacksActivity.this, i);
                    // Unlock pack's category, if any
                    int catId = packsDao.getCategoryId(PacksActivity.this, i);
                    if (catId != Constants.DEFAULT_CAT_ID) {
                        new CategoryDao().setIsOwned(PacksActivity.this, catId);
                    } else {
                        Log.d(TAG, "Bought pack had no category");
                    }
                    Utils.installPack(PacksActivity.this, i);
                    // Reset pack update time to reload previously purchased topics (if deleted)
//                    editor.putLong(Constants.getTopicsUpdateTime(i), 0);
                }
                editor.putBoolean(Constants.getMultipackKey(mSelectedMultipack), true);
                editor.apply();

                initUi();
            } else if (purchase.getSku().equals(Constants.getPackSku(mSelectedPack))) { // single pack
                // Unlock purchased pack
                Utils.alertUser(PacksActivity.this, getString(R.string.purchased_pack));

                PacksDao packsDao = new PacksDao();
                // Unlock pack
                packsDao.setIsOwned(PacksActivity.this, mSelectedPack);
                // Unlock pack's category, if any
                int catId = packsDao.getCategoryId(PacksActivity.this, mSelectedPack);
                if (catId != Constants.DEFAULT_CAT_ID) {
                    new CategoryDao().setIsOwned(PacksActivity.this, catId);
                } else {
                    Log.d(TAG, "Bought pack had no category");
                }
                Utils.installPack(PacksActivity.this, mSelectedPack);

                initUi();
            } else if (purchase.getSku().equals(Constants.SKU_MEMBERSHIP)) {
                // Unlock all current packs
                Utils.alertUser(PacksActivity.this, getString(R.string.purchased_membership));

                PacksDao packsDao = new PacksDao();
                for (int i = 1; i <= mNumPacks; ++i) { // 0 is the free pack!
                    // Unlock pack
                    packsDao.setIsOwned(PacksActivity.this, i);
                    // Unlock pack's category, if any
                    int catId = packsDao.getCategoryId(PacksActivity.this, i);
                    if (catId != Constants.DEFAULT_CAT_ID) {
                        new CategoryDao().setIsOwned(PacksActivity.this, catId);
                    } else {
                        Log.d(TAG, "Bought pack had no category");
                    }
                    Utils.installPack(PacksActivity.this, i);
                }

                initUi();
            } else {
                Log.e(TAG, "Unhandled SKU");
                Utils.alertUser(PacksActivity.this, getString(R.string.error_purchasing, result));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packs);

        // Let device volume buttons control sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        initUi();

        // Set up in-app billing (asynchronous)
        mHelper = new IabHelper(this, Constants.BASE64_PUBLIC_KEY);
        mHelper.enableDebugLogging(false); // set true for debugging
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Utils.alertUser(PacksActivity.this, getString(R.string.error_billing_setup, result));
                    return;
                }

                if (mHelper == null) return; // Quit if helper was disposed of

                // Register AFTER IabHelper is setup, but before first call to getPurchases()
                mBroadcastReceiver = new IabBroadcastReceiver(new IabListener());
                registerReceiver(mBroadcastReceiver, new IntentFilter(IabBroadcastReceiver.ACTION));
                try { // Check purchase history
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    Utils.alertUser(PacksActivity.this, getString(R.string.error_inventory));
                }
            }
        });

        if (Utils.isPortrait(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void initUi() {
        // Get number of paid packs available for purchase
        setLoadScreen(true);
        FirebaseDatabase.getInstance()
                .getReference(Constants.METADATA_KEY)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Called once with initial value and again whenever data at this location updates
                        setLoadScreen(false);
                        setLoadScreen(true);
                        Long res = dataSnapshot.getValue(Long.class);
                        mNumPacks = (res != null) ? res.intValue() : 0;
                        initPacks();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.w(TAG, "Error reading packs data", error.toException());
                    }
                });
    }

    /**
     * Refreshes pack GridView.
     */
    private void initPacks() {
        if (mNumPacks <= 0) { // no packs
            findViewById(R.id.gv_packs).setVisibility(View.INVISIBLE);
            findViewById(R.id.tv_no_packs).setVisibility(View.VISIBLE);
            if (!Utils.isPortrait(this)) {
                findViewById(R.id.gv_multipacks).setVisibility(View.INVISIBLE);
                findViewById(R.id.tv_no_multipacks).setVisibility(View.VISIBLE);
            }
        } else { // show packs
            findViewById(R.id.gv_packs).setVisibility(View.VISIBLE);
            findViewById(R.id.tv_no_packs).setVisibility(View.GONE);
            if (!Utils.isPortrait(this)) {
                findViewById(R.id.gv_multipacks).setVisibility(View.VISIBLE);
                findViewById(R.id.tv_no_multipacks).setVisibility(View.GONE);
            }

            PacksDao packsDao = new PacksDao();
            final boolean[] arePacksUnlocked = new boolean[mNumPacks];
            for (int i = 1; i <= mNumPacks; ++i) { // packs count from 1!!
                arePacksUnlocked[i - 1] = packsDao.getIsOwned(this, i);
//                if (arePacksUnlocked[i - 1]) {
//                    String updateKey = Constants.getTopicsUpdateTime(i);
//                    Utils.checkForPackUpdates(this, shprefs.getLong(updateKey, 0), Constants.getPackDirectory(i), Constants.TOPICS_FILE, updateKey, new TopicsDao(), i);
//                    updateKey = Constants.getAnswersUpdateTime(i);
//                    Utils.checkForPackUpdates(this, shprefs.getLong(updateKey, 0), Constants.getPackDirectory(i), Constants.ANSWERS_FILE, updateKey, new AnswersDao(), i);
//                    updateKey = Constants.getQuestionsUpdateTime(i);
//                    Utils.checkForPackUpdates(this, shprefs.getLong(updateKey, 0), Constants.getPackDirectory(i), Constants.QUESTIONS_FILE, updateKey, new QuestionsDao(), i);
//                }
            }

            // Packs grid
            GridView grid = findViewById(R.id.gv_packs);
            PacksArrayAdapter arrayAdapter = new PacksArrayAdapter(this, getLayoutInflater(), arePacksUnlocked, true);
            grid.setAdapter(arrayAdapter);
            grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
                    if (!arePacksUnlocked[index]) {
                        mSelectedPack = index + 1;
                        promptPurchasePack();
                    } // else pack is already unlocked, ignore
                }
            });
        }
        initMultipacks();
        setLoadScreen(false);
    }

    private void initMultipacks() {
        if (!Utils.isPortrait(this)) {
            if (mNumPacks >= Constants.PACKS_PER_MULTIPACK) {
                final boolean[] isUnlocked = new boolean[mNumPacks / Constants.PACKS_PER_MULTIPACK];

                SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
                for (int i = 1; i <= isUnlocked.length; ++i) { // multipacks count from 1!!
                    isUnlocked[i - 1] = shprefs.getBoolean(Constants.getMultipackKey(i), false);
                }
                // Packs grid
                GridView grid = findViewById(R.id.gv_multipacks);
                PacksArrayAdapter arrayAdapter = new PacksArrayAdapter(this, getLayoutInflater(), isUnlocked, false);
                grid.setAdapter(arrayAdapter);
                grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
                        if (!isUnlocked[index]) {
                            mSelectedMultipack = index + 1;
                            promptPurchaseMultiPack();
                        } // else multipack is already unlocked, ignore
                    }
                });
            } else {
                findViewById(R.id.tv_no_multipacks).setVisibility(View.VISIBLE);
            }
        } // else no multipack option
    }

    /**
     * Initiates purchase process for membership.
     */
    public void promptPurchaseMembership(View v) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_purchase_membership))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setLoadScreen(true);
                        try {
                            mIsMultipackPurchase = false;
                            mHelper.launchPurchaseFlow(PacksActivity.this,
                                    Constants.SKU_MEMBERSHIP,
                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            Utils.alertUser(PacksActivity.this, getString(R.string.error_purchase));
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

    void promptPurchaseMultiPack() {
        PacksDao packsDao = new PacksDao();
        String msg = getString(R.string.confirm_purchase_multipack);
        for (int i = (mSelectedMultipack - 1) * Constants.PACKS_PER_MULTIPACK + 1;
             i < mSelectedMultipack * Constants.PACKS_PER_MULTIPACK + 1; ++i) {
            if (packsDao.getIsOwned(this, i)) {
                msg = getString(R.string.warn_purchase_multipack);
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setLoadScreen(true);
                        try {
                            mIsMultipackPurchase = true;
                            mHelper.launchPurchaseFlow(PacksActivity.this,
                                    Constants.getMultipackSku(mSelectedMultipack),
                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            Utils.alertUser(PacksActivity.this, getString(R.string.error_purchase));
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

    /**
     * Initiates purchase process for pack.
     */
    private void promptPurchasePack() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_purchase_pack, mSelectedPack))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setLoadScreen(true);
                        try {
                            mIsMultipackPurchase = false;
                            mHelper.launchPurchaseFlow(PacksActivity.this,
                                    Constants.getPackSku(mSelectedPack),
                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            Utils.alertUser(PacksActivity.this, getString(R.string.error_purchase));
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

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.movieReel:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                finish();
                break;
            default:
                Log.e(TAG, "Unhandled button press");
                break;
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
                Utils.alertUser(PacksActivity.this, getString(R.string.error_inventory));
            }
        }
    }
}
