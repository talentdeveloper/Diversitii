package com.diversitii.dcapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.diversitii.dcapp.billing.IabBroadcastReceiver;
import com.diversitii.dcapp.billing.IabHelper;
import com.diversitii.dcapp.billing.IabResult;
import com.diversitii.dcapp.billing.Inventory;
import com.diversitii.dcapp.billing.Purchase;
import com.diversitii.dcapp.database.CategoryDao;
import com.diversitii.dcapp.database.PacksDao;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

/**
 * Membership options screen.
 */
public class MembershipActivity extends MusicActivity {
    private static final String TAG = MembershipActivity.class.getName();
    private static final int RC_REQUEST = 12103; // (arbitrary) request code for the purchase flow (permissible range is restricted)

    // In-app billing
    private int mSelectedMultipack = 0;
    private boolean mIsMultipackPurchase = false;
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
                Utils.alertUser(MembershipActivity.this, getString(R.string.error_inventory2, result));
                return;
            }

            // Check memberships
            Purchase membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_DIAMOND_ANNUAL));
            if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                Utils.checkUpdateMembership(MembershipActivity.this, Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
            } else {
                membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_DIAMOND_BIANNUAL));
                if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                    Utils.checkUpdateMembership(MembershipActivity.this, Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
                } else {
                    membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_DIAMOND_QUARTERLY));
                    if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                        Utils.checkUpdateMembership(MembershipActivity.this, Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
                    }
                }
            }
            membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_RED_DIAMOND_ANNUAL));
            if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                Utils.checkUpdateMembership(MembershipActivity.this, Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
            } else {
                membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_RED_DIAMOND_BIANNUAL));
                if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                    Utils.checkUpdateMembership(MembershipActivity.this, Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
                } else {
                    membership = inventory.getPurchase(Constants.getMembershipSku(Constants.MEMBERSHIP_RED_DIAMOND_QUARTERLY));
                    if (membership != null && Utils.verifyDeveloperPayload(membership)) {
                        Utils.checkUpdateMembership(MembershipActivity.this, Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH, Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
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
                    Utils.alertUser(MembershipActivity.this, getString(R.string.error_purchasing, result));
                }
                return;
            }
            if (!Utils.verifyDeveloperPayload(purchase)) {
                Utils.alertUser(MembershipActivity.this, getString(R.string.error_purchasing2));
                return;
            }
            // Purchase success
            if (mIsMultipackPurchase) {
                Utils.alertUser(MembershipActivity.this, getString(R.string.purchased_multipack));

                PacksDao packsDao = new PacksDao();
                SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = shprefs.edit();
                for (int i = (mSelectedMultipack - 1) * Constants.PACKS_PER_MULTIPACK + 1; i <= mSelectedMultipack * Constants.PACKS_PER_MULTIPACK; ++i) {
                    // Unlock pack
                    packsDao.setIsOwned(MembershipActivity.this, i);
                    // Unlock pack's category, if any
                    int catId = packsDao.getCategoryId(MembershipActivity.this, i);
                    if (catId != Constants.DEFAULT_CAT_ID) {
                        new CategoryDao().setIsOwned(MembershipActivity.this, catId);
                    } else {
                        Log.d(TAG, "Bought pack had no category");
                    }
                    Utils.installPack(MembershipActivity.this, i);
                    // Reset pack update time to reload previously purchased topics (if deleted)
//                    editor.putLong(Constants.getTopicsUpdateTime(i), 0);
                }
                editor.putBoolean(Constants.getMultipackKey(mSelectedMultipack), true);
                editor.apply();

                initUi();
            } else if (mMembership != Constants.MEMBERSHIP_INVALID) { // Membership purchase
                // Unlock all current packs
                Utils.alertUser(MembershipActivity.this, getString(R.string.purchased_membership));

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
                        Utils.installPacks(MembershipActivity.this,
                                Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_DIAMOND_BIANNUAL:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(MembershipActivity.this,
                                Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_DIAMOND_QUARTERLY:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(MembershipActivity.this,
                                Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_RED_DIAMOND_ANNUAL:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(MembershipActivity.this,
                                Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_RED_DIAMOND_BIANNUAL:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(MembershipActivity.this,
                                Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_RED_DIAMOND_QUARTERLY:
                        editor.putBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, true);
                        Utils.installPacks(MembershipActivity.this,
                                Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH,
                                Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
                        break;
                    case Constants.MEMBERSHIP_INVALID:
                    default:
                        Log.e("Constants", "Unhandled membership option: " + mMembership);
                        Utils.alertUser(MembershipActivity.this, getString(R.string.error_membership));
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
                        packsDao.setIsOwned(MembershipActivity.this, i);
                        // Unlock pack's category, if any
                        int catId = packsDao.getCategoryId(MembershipActivity.this, i);
                        if (catId != Constants.DEFAULT_CAT_ID) {
                            new CategoryDao().setIsOwned(MembershipActivity.this, catId);
                        } else {
                            Log.d(TAG, "Bought pack had no category");
                        }
                        Utils.installPack(MembershipActivity.this, i);
                    }
                }

                initUi();
            } else {
                Log.e(TAG, "Unhandled SKU");
                Utils.alertUser(MembershipActivity.this, getString(R.string.error_purchasing, result));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membership_options);

        // Let device volume buttons control sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        initUi();

        // Set up in-app billing (asynchronous)
        mHelper = new IabHelper(this, Constants.BASE64_PUBLIC_KEY);
        mHelper.enableDebugLogging(false); // set true for debugging
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Utils.alertUser(MembershipActivity.this, getString(R.string.error_billing_setup, result));
                    return;
                }

                if (mHelper == null) return; // Quit if helper was disposed of

                // Register AFTER IabHelper is setup, but before first call to getPurchases()
                mBroadcastReceiver = new IabBroadcastReceiver(new IabListener());
                registerReceiver(mBroadcastReceiver, new IntentFilter(IabBroadcastReceiver.ACTION));
                try { // Check purchase history
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    Utils.alertUser(MembershipActivity.this, getString(R.string.error_inventory));
                }
            }
        });

        if(Utils.isAndroidTV(this)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
            if (Utils.isPortrait(this)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
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
                        Long res = dataSnapshot.getValue(Long.class);
                        initMultipacks((res != null) ? res.intValue() : 0);
                        initUi2();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.w(TAG, "Error reading packs data", error.toException());
                    }
                });

        Utils.getOffers(this, null, R.id.tv_notifications, R.id.offer_progress);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MembershipActivity.this, CategoryControllerActivity.class));
                finish();
            }
        });
    }

    private void initUi2() {
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
                            initMembership(topicIds.size());
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
    private void initMembership(final int numRandomTopics) {
        Log.d(TAG, "Random topics: " + numRandomTopics);

        // Membership options list
        ListView list = findViewById(R.id.lv_cats);
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
                        Utils.alertUser(MembershipActivity.this, getString(R.string.membership_owned));
                    } else {
                        mMembership = Constants.MEMBERSHIP_COPPER;
                        promptPurchaseMembership(view);
                    }
                } else if (membership.equals(getString(R.string.membership_bronze))) {
                    if (shprefs.getBoolean(Constants.SHPREFS_BOUGHT_BRONZE, false)) {
                        Utils.alertUser(MembershipActivity.this, getString(R.string.membership_owned));
                    } else {
                        mMembership = Constants.MEMBERSHIP_BRONZE;
                        promptPurchaseMembership(view);
                    }
                } else if (membership.equals(getString(R.string.membership_silver))) {
                    if (shprefs.getBoolean(Constants.SHPREFS_BOUGHT_SILVER, false)) {
                        Utils.alertUser(MembershipActivity.this, getString(R.string.membership_owned));
                    } else {
                        mMembership = Constants.MEMBERSHIP_SILVER;
                        promptPurchaseMembership(view);
                    }
                } else if (membership.equals(getString(R.string.membership_gold))) {
                    if (shprefs.getBoolean(Constants.SHPREFS_BOUGHT_GOLD, false)) {
                        Utils.alertUser(MembershipActivity.this, getString(R.string.membership_owned));
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
                        Utils.alertUser(MembershipActivity.this, getString(R.string.membership_locked));
                    } else if (shprefs.getBoolean(Constants.SHPREFS_SUBSCRIBED_DIAMOND, false)) {
                        Utils.alertUser(MembershipActivity.this, getString(R.string.error_already_subscribed));
                    } else {
                        if (membership.equals(getString(R.string.membership_diamond_annual))) {
                            if (!Utils.hasSupplyForDiamond(MembershipActivity.this, numRandomTopics, Constants.MONTHS_ANNUAL)) {
                                Utils.alertUser(MembershipActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_DIAMOND_ANNUAL;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else if (membership.equals(getString(R.string.membership_diamond_biannual))) {
                            if (!Utils.hasSupplyForDiamond(MembershipActivity.this, numRandomTopics, Constants.MONTHS_BIANNUAL)) {
                                Utils.alertUser(MembershipActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_DIAMOND_BIANNUAL;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else if (membership.equals(getString(R.string.membership_diamond_quarterly))) {
                            if (!Utils.hasSupplyForDiamond(MembershipActivity.this, numRandomTopics, Constants.MONTHS_QUARTERLY)) {
                                Utils.alertUser(MembershipActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_DIAMOND_QUARTERLY;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else if (membership.equals(getString(R.string.membership_red_diamond_annual))) {
                            if (!Utils.hasSupplyForRedDiamond(MembershipActivity.this, numRandomTopics, Constants.MONTHS_ANNUAL)) {
                                Utils.alertUser(MembershipActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_RED_DIAMOND_ANNUAL;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else if (membership.equals(getString(R.string.membership_red_diamond_biannual))) {
                            if (!Utils.hasSupplyForRedDiamond(MembershipActivity.this, numRandomTopics, Constants.MONTHS_BIANNUAL)) {
                                Utils.alertUser(MembershipActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_RED_DIAMOND_BIANNUAL;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else if (membership.equals(getString(R.string.membership_red_diamond_quarterly))) {
                            if (!Utils.hasSupplyForRedDiamond(MembershipActivity.this, numRandomTopics, Constants.MONTHS_QUARTERLY)) {
                                Utils.alertUser(MembershipActivity.this, getString(R.string.error_membership_supply));
                            } else {
                                mMembership = Constants.MEMBERSHIP_RED_DIAMOND_QUARTERLY;
                                promptPurchaseMembershipSubscription(view);
                            }
                        } else {
                            Log.e(TAG, "Unhandled membership name " + membership);
                            Utils.alertUser(MembershipActivity.this, getString(R.string.error_purchase));
                        }
                    }
                }
            }
        });
    }

    /**
     * Sets up a number of multipacks based on how many packs are available.
     *
     * @param numPacks the number of individual packs available
     */
    private void initMultipacks(int numPacks) {
        if (numPacks >= Constants.PACKS_PER_MULTIPACK) {
            final boolean[] isUnlocked = new boolean[numPacks / Constants.PACKS_PER_MULTIPACK];

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
    }

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
                            mIsMultipackPurchase = false;
                            mHelper.launchPurchaseFlow(MembershipActivity.this,
                                    Constants.getMembershipSku(mMembership),
                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            Utils.alertUser(MembershipActivity.this, getString(R.string.error_purchase));
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
     * Initiates purchase process for multipacks.
     */
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
                            mHelper.launchPurchaseFlow(MembershipActivity.this,
                                    Constants.getMultipackSku(mSelectedMultipack),
                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            Utils.alertUser(MembershipActivity.this, getString(R.string.error_purchase));
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
                            mIsMultipackPurchase = false;
                            mHelper.launchSubscriptionPurchaseFlow(MembershipActivity.this,
                                    Constants.getMembershipSku(mMembership),
                                    RC_REQUEST, mPurchaseFinishedListener, Utils.getPayload());
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            Utils.alertUser(MembershipActivity.this, getString(R.string.error_purchase));
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
                Utils.alertUser(MembershipActivity.this, getString(R.string.error_inventory));
            }
        }
    }
}
