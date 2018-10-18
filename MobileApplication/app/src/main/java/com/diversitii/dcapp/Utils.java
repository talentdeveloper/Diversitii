package com.diversitii.dcapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.UiModeManager;
import android.content.res.Configuration;
import android.content.Context;


import com.diversitii.dcapp.billing.Purchase;
import com.diversitii.dcapp.database.Answer;
import com.diversitii.dcapp.database.AnswersDao;
import com.diversitii.dcapp.database.CategoryDao;
import com.diversitii.dcapp.database.Pack;
import com.diversitii.dcapp.database.PacksDao;
import com.diversitii.dcapp.database.Question;
import com.diversitii.dcapp.database.QuestionsDao;
import com.diversitii.dcapp.database.Topic;
import com.diversitii.dcapp.database.TopicsDao;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;



import static android.content.Context.UI_MODE_SERVICE;

/**
 * Utility methods.
 */
class Utils {
    private static final String TAG = Utils.class.getName();


    /*
     * TODO: for added security, generate a developer payload here for verification. See the
     * TODO in Utils.verifyDeveloperPayload() for more info.
     */
    static String getPayload() {
        return "";
    }

    /**
     * Whether or not layout orientation is portrait.
     *
     * @param context the calling Context
     * @return true if screen size indicates device is a phone, else false
     */
    static boolean isPortrait(Context context) {
        return context.getResources().getBoolean(R.bool.is_portrait);
    }
    static boolean isAndroidTV(Context context){
        boolean isAndroidTV;
        int uiMode = context.getResources().getConfiguration().uiMode;
        if ((uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION) {
            isAndroidTV = true;
        }else isAndroidTV = false;
        return isAndroidTV;
    }
//    /**
//     * Returns true if no network (wifi or data) is available.
//     *
//     * @param context the calling Context
//     * @return true if no network
//     */
//    static boolean noNetwork(Context context) {
//        ConnectivityManager cm =
//                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (cm != null) {
//            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
//            return (activeNetwork == null || !activeNetwork.isConnectedOrConnecting());
//        } else {
//            return true;
//        }
//    }

    static void alertUser(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setNeutralButton(context.getString(R.string.ok), null);
        builder.create().show();
    }

    /**
     * Verifies the developer payload returned by the purchase confirmation.
     *
     * @param p the purchase
     * @return true if verification passed, else false
     */
    static boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */
        return true;
    }

    /**
     * Saves given topics, along with their questions and answers, in the database.
     *
     * @param activity  the calling Activity
     * @param topicIds  the IDs of the topics to save
     * @param updateKey the SharedPreferences key of the update time for the pack data type
     */
    private static void savePackContents(final Activity activity, final String[] topicIds, final String updateKey) {
        savePackContents(activity, topicIds, updateKey, false);
    }

    /**
     * Saves given topics, along with their questions and answers, in the database.
     *
     * @param activity       the calling Activity
     * @param topicIds       the IDs of the topics to save
     * @param updateKey      the SharedPreferences key of the update time for the pack data type
     * @param doUpdateTopics whether or not to update the topics displayed on the MainActivity topic selection buttons
     */
    static void savePackContents(final Activity activity, final String[] topicIds, final String updateKey, final boolean doUpdateTopics) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (int i = 0; i < topicIds.length; ++i) {
            try {
                final int id = Integer.parseInt(topicIds[i]);
                final int index = i;

                // TOPICS
                db.collection(Constants.COLLECTION_TOPICS).document(topicIds[index])
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document != null && document.exists()) {
                                        // Mark topic's category as owned
                                        new CategoryDao().setIsOwned(activity, Integer.parseInt(document.get(Constants.FIELD_CATEGORY_ID).toString()));

                                        new TopicsDao().addTopic(activity, new Topic(Integer.parseInt(document.getId()),
                                                Integer.parseInt(document.get(Constants.FIELD_CATEGORY_ID).toString()),
                                                document.get(Constants.FIELD_TEXT).toString(),
                                                Constants.DB_FALSE));
                                    } else {
                                        Log.e(TAG, "Fetch failed for topic " + topicIds[index]);
                                    }

                                    if (doUpdateTopics) {
                                        ((MainActivity) activity).updateTopics();
                                    }
                                } else {
                                    Log.e(TAG, "Topic fetch task error: " + task.getException());
                                }
                            }
                        });

                // QUESTIONS
                for (int j = 0; j < Constants.QUESTIONS_PER_TOPIC; ++j) {
                    final int jIndex = j;
                    db.collection(Constants.COLLECTION_QUESTIONS).document(topicIds[i] + Constants.COLLECTION_ID_SEPARATOR + (jIndex + 1))
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document != null && document.exists()) {
                                            new QuestionsDao().addQuestion(activity, new Question((jIndex + 1),
                                                    id,
                                                    document.get(Constants.FIELD_TEXT).toString(),
                                                    Integer.parseInt(document.get(Constants.FIELD_CORRECT_ANSWER_ID).toString()),
                                                    Integer.parseInt(document.get(Constants.FIELD_POINTS).toString()),
                                                    Integer.parseInt(document.get(Constants.FIELD_SUBTOPIC_ID).toString())));
                                        } else {
                                            Log.e(TAG, "Fetch failed for question " + (jIndex + 1) + " topic " + topicIds[index]);
                                        }
                                    } else {
                                        Log.e(TAG, "Question fetch task error: " + task.getException());
                                    }
                                }
                            });
                }

                // ANSWERS
                for (int j = 0; j < Constants.MAX_ANSWERS; ++j) {
                    final int jIndex = j;
                    db.collection(Constants.COLLECTION_ANSWERS).document(topicIds[i] + Constants.COLLECTION_ID_SEPARATOR + (jIndex + 1))
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document != null && document.exists()) {
                                            new AnswersDao().addAnswer(activity, new Answer((jIndex + 1),
                                                    id,
                                                    document.get(Constants.FIELD_TEXT).toString()));
                                        } else {
                                            Log.e(TAG, "Fetch failed for answer " + (jIndex + 1) + " topic " + topicIds[index]);
                                        }
                                    } else {
                                        Log.e(TAG, "Answer fetch task error: " + task.getException());
                                    }
                                }
                            });
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "NumberFormatException for topic ID " + topicIds[i]);
            }
        }

        // Save back the update time
        SharedPreferences shprefs = activity.getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shprefs.edit();
        editor.putLong(updateKey, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * Whether or not player names are set for multiplayer mode.
     *
     * @param context the calling context
     * @return true if at least two names are set
     */
    static boolean arePlayersSet(Context context) {
        boolean isPortrait = Utils.isPortrait(context);
        int[] nameIds = Constants.getPlayerNameIds(isPortrait);
        Map<Integer, String> nameStorage = Constants.getPlayerNameStorageIds(isPortrait);
        SharedPreferences shprefs = context.getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        String player1Default = context.getString(R.string.player_1);
        String player1 = shprefs.getString(nameStorage.get(nameIds[0]), player1Default);
        String player2Default = context.getString(R.string.player_2);
        String player2 = shprefs.getString(nameStorage.get(nameIds[1]), player2Default);
        if (!isPortrait) {
            String player3Default = context.getString(R.string.player_3);
            String player3 = shprefs.getString(nameStorage.get(nameIds[2]), player3Default);
            String player4Default = context.getString(R.string.player_4);
            String player4 = shprefs.getString(nameStorage.get(nameIds[3]), player4Default);
            return (!player1.equals(player1Default) &&
                    (!player2.equals(player2Default)
                            || !player3.equals(player3Default)
                            || !player4.equals(player4Default)))
                    ||
                    (!player2.equals(player2Default) &&
                            (!player3.equals(player3Default)
                                    || !player4.equals(player4Default)))
                    ||
                    (!player3.equals(player3Default) &&
                            !player4.equals(player4Default));
        } else {
            return (!player1.equals(player1Default) &&
                    !player2.equals(player2Default));
        }
    }

    /**
     * Whether the given player has a name saved on the scoreboard. Multiplayer only.
     *
     * @param context      the calling context
     * @param playerNumber the player number, FROM 0
     * @return true if the player has a name
     */
    static boolean hasPlayerName(Context context, int playerNumber) {
        boolean isPortrait = Utils.isPortrait(context);
        SharedPreferences shprefs = context.getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        String defaultName = context.getString(Constants.getPlayerDefaultNameIds(isPortrait)[playerNumber]);
        return !shprefs.getString(Constants.getPlayerNameStorageIds(isPortrait).get(Constants.getPlayerNameIds(isPortrait)[playerNumber]),
                defaultName).equals(defaultName);
    }

    /**
     * Collects report form input, if any, and opens system email dialog for user to send report.
     */
    static void sendReport(Activity act) {
        StringBuilder reportTitle = new StringBuilder(); // make a comma-separated list of reported problems
        if (((CheckBox) act.findViewById(R.id.spellingBox)).isChecked()) {
            reportTitle.append(act.getString(R.string.list_item, act.getString(R.string.spelling_error)));
        }
        if (((CheckBox) act.findViewById(R.id.wrongAnswerBox)).isChecked()) {
            reportTitle.append(act.getString(R.string.list_item, act.getString(R.string.wrong_answer)));
        }
        if (((CheckBox) act.findViewById(R.id.wrongTopicBox)).isChecked()) {
            reportTitle.append(act.getString(R.string.list_item, act.getString(R.string.wrong_topic)));
        }
        if (((CheckBox) act.findViewById(R.id.otherBox)).isChecked()) {
            reportTitle.append(act.getString(R.string.list_item, act.getString(R.string.other_error)));
        }
        if (reportTitle.length() > 0) { // check box(es) were checked
            String details = ((EditText) act.findViewById(R.id.correctionsCorrectionsText)).getText().toString();
            if (details.length() > 0) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setData(Uri.parse("mailto:"));
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{act.getString(R.string.contact_email)});
                i.putExtra(Intent.EXTRA_SUBJECT, act.getString(R.string.email_title));
                i.putExtra(Intent.EXTRA_TEXT, act.getString(R.string.email_report,
                        reportTitle.substring(2), // remove leading comma and space
                        details));
                Intent chooser = Intent.createChooser(i, act.getString(R.string.prompt_email));
                if (i.resolveActivity(act.getPackageManager()) != null) {
                    act.startActivity(chooser);
                } else {
                    Toast.makeText(act, act.getString(R.string.error_no_email), Toast.LENGTH_SHORT).show();
                }
            } else { // no details given
                Toast.makeText(act, act.getString(R.string.error_no_details), Toast.LENGTH_SHORT).show();
            }
        } else { // no check boxes checked
            Toast.makeText(act, act.getString(R.string.error_no_selection), Toast.LENGTH_SHORT).show();
        }
    }

    static boolean isInGame(SharedPreferences shprefs) {
        return shprefs.getInt(Constants.SHPREFS_FIRST_PLAYER, Constants.NO_PLAYER) != Constants.NO_PLAYER;
    }

    static int getDefaultLives(SharedPreferences shprefs) {
        return Constants.LIVES_VALUES[shprefs.getInt(Constants.SHPREFS_SETTINGS_LIVES, 0)];
    }

    static int getPointsValue(SharedPreferences shprefs) {
        return Constants.POINTS_VALUES[shprefs.getInt(Constants.SHPREFS_SETTINGS_POINTS, 0)];
    }

    static int getGameScore(SharedPreferences shprefs) {
        return Constants.SCORES_VALUES[shprefs.getInt(Constants.SHPREFS_SETTINGS_SCORES, 0)];
    }

    static int getMultiplayerLives(SharedPreferences shprefs, int player) {
        int lives = Utils.getDefaultLives(shprefs);
        switch (player) {
            case 0:
                lives = shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER1, lives);
                break;
            case 1:
                lives = shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER2, lives);
                break;
            case 2:
                lives = shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER3, lives);
                break;
            case 3:
                lives = shprefs.getInt(Constants.SHPREFS_LIVES_PLAYER4, lives);
                break;
            default:
                Log.e(TAG, "Invalid player");
                break;
        }
        return lives;
    }

    /**
     * Gets offer text file from cloud storage, if it is newer than the stored file. User must be
     * logged in with Firebase.
     */
    static void getOffers(final Activity activity, final Dialog dialog, final int offersId, final int offersProgress) {
        // Check age of offers file
        SharedPreferences shprefs = activity.getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = shprefs.edit();
        final long savedUpdateTime = shprefs.getLong(Constants.SHPREFS_OFFER_UPDATE_MILLIS, 0);
        final StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(Constants.STORAGE_URL + Constants.OFFERS_FILE);
        storageRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                final long updateTime = storageMetadata.getUpdatedTimeMillis();
                if (updateTime > savedUpdateTime) {
                    // Get new file
                    storageRef.getBytes(Constants.MAX_OFFERS_SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            String offers = null;
                            try {
                                offers = new String(bytes, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                Log.e(TAG, "Error getting offers: " + e.toString());
                                // Offers error text displays by default
                            }
                            editor.putString(Constants.SHPREFS_OFFER, offers);
                            editor.putLong(Constants.SHPREFS_OFFER_UPDATE_MILLIS, updateTime);
                            editor.apply();
                            updateOffersUi(activity, dialog, offers, offersId, offersProgress);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error getting offers: " + e.toString());
                            // There was an error, use the saved text if any
                            updateOffersUi(activity, dialog, null, offersId, offersProgress);
                        }
                    });
                } else {
                    // Use stored file
                    updateOffersUi(activity, dialog, null, offersId, offersProgress);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error checking offers: " + e.toString());
                // There was an error, use the saved text if any
                updateOffersUi(activity, dialog, null, offersId, offersProgress);
            }
        });
    }

    /**
     * Displays the offer or saved offer if any, or error message on error.
     *
     * @param offersText the contents of the offers text file, if retrieved
     */
    private static void updateOffersUi(Activity activity, Dialog dialog, String offersText, int offersId, int offersProgress) {
        TextView tv;
        if (dialog != null) {
            tv = dialog.findViewById(offersId);
        } else {
            tv = activity.findViewById(offersId);
        }
        if (offersText != null) { // show fresh text
            tv.setText(offersText);
        } else { // load saved text or error text if nothing saved
            tv.setText(activity.getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE)
                    .getString(Constants.SHPREFS_OFFER, activity.getString(R.string.error_offers)));
        }
        tv.setVisibility(View.VISIBLE);
        // Hide spinner
        if (dialog != null) {
            dialog.findViewById(offersProgress).setVisibility(View.INVISIBLE);
        } else {
            activity.findViewById(offersProgress).setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Adds given pack to the database.
     */
    static void installPack(final Activity activity, int packId) {
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_PACKS).document(String.valueOf(packId))
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                try {
                                    new PacksDao().addPack(activity,
                                            new Pack(Integer.parseInt(document.getId()),
                                                    Integer.parseInt(document.get(Constants.FIELD_CATEGORY_ID).toString()),
                                                    true));

                                    String topicIds = document.get(Constants.FIELD_TOPIC_IDS).toString();
                                    savePackContents(activity,
                                            topicIds.split(Constants.TOPIC_ID_LIST_SEPARATOR),
                                            Constants.SHPREFS_PACKS_UPDATE);
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Invalid ID! Pack " + document.getId());
                                }
                            } else {
                                Log.e(TAG, "Pack not found");
                            }
                        } else {
                            Log.w(TAG, "Error getting pack: ", task.getException());
                        }
                    }
                });
    }

    /**
     * Adds all packs in given category to the database.
     */
    static void installCategory(final Activity activity, final int categoryId) {
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_PACKS)
                .whereEqualTo(Constants.FIELD_CATEGORY_ID, categoryId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                if (document != null && document.exists()) {
                                    try {
                                        new PacksDao().addPack(activity,
                                                new Pack(Integer.parseInt(document.getId()),
                                                        categoryId, true));

                                        String topicIds = document.get(Constants.FIELD_TOPIC_IDS).toString();
                                        savePackContents(activity,
                                                topicIds.split(Constants.TOPIC_ID_LIST_SEPARATOR),
                                                Constants.SHPREFS_PACKS_UPDATE);
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Invalid ID! Pack " + document.getId());
                                    }
                                } else {
                                    Log.e(TAG, "Pack not found");
                                }
                            }
                        } else {
                            Log.w(TAG, "Error getting pack: ", task.getException());
                        }
                    }
                });
    }

    /**
     * Checks if it is time to update packs per the subscribed-to membership.
     */
    static void checkUpdateMembership(Activity activity, int catPacks, int randomTopics) {
        SharedPreferences shprefs = activity.getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shprefs.edit();

        String lastUpdate = shprefs.getString(Constants.SHPREFS_SUBSCRIPTION_DATE_DIAMOND, "");
        if (!lastUpdate.isEmpty()) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(Constants.FORMAT_DATE);
            Date updated;
            try {
                updated = sdf.parse(lastUpdate);
                if (System.currentTimeMillis() >= updated.getTime() + (DateUtils.DAY_IN_MILLIS * 30)) {
                    Log.d(TAG, "Applying membership");

                    Utils.installPacks(activity, catPacks, randomTopics);

                    // Save the subscription date
                    Calendar cal = Calendar.getInstance(Constants.LOCALE);
                    cal.setTimeInMillis(System.currentTimeMillis());
                    editor.putString(Constants.SHPREFS_SUBSCRIPTION_DATE_DIAMOND, DateFormat.format(Constants.FORMAT_DATE, cal).toString());
                    editor.apply();
                }
            } catch (ParseException e) {
                Log.e(TAG, "Parsing date: " + e.toString());
            }
        } else {
            Log.e(TAG, "No subscription update time saved");
        }
    }

    /**
     * Installs given number of category packs and random topics. For subscription memberships.
     */
    static void installPacks(final Activity activity, int catPacks, final int randomTopics) {
        // Add category packs
        int[] packIds = new PacksDao().getUnboughtPacks(activity, catPacks);
        for (int packId : packIds) {
            Utils.installPack(activity, packId);
        }

        // Add random topics
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_TOPICS)
                .whereEqualTo(Constants.FIELD_CATEGORY_ID, Constants.DEFAULT_CAT_ID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int savedTopics = 0;
                            for (DocumentSnapshot document : task.getResult()) {
                                if (savedTopics >= randomTopics) {
                                    break;
                                }
                                if (document != null && document.exists()) {
                                    try {
                                        if (!new TopicsDao().topicExists(activity, Integer.parseInt(document.getId()))) {
                                            // Topic not owned, save it
                                            savePackContents(activity, new String[]{document.getId()}, Constants.SHPREFS_PACKS_UPDATE);
                                            savedTopics++;
                                        } // else already owned
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Invalid ID! Topic " + document.getId());
                                    }
                                } else {
                                    Log.e(TAG, "Topic not found");
                                }
                            }
                        } else {
                            Log.w(TAG, "Error getting topics: ", task.getException());
                        }
                    }
                });
    }

    static boolean hasSupplyForDiamond(Context context, int numRandTopics, int months) {
        return (new PacksDao().getTotalUnboughtCatPacks(context) >= months * Constants.MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH)
                && ((numRandTopics - new TopicsDao().getTotalRandomTopics(context)) >= months * Constants.MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH);
    }

    static boolean hasSupplyForRedDiamond(Context context, int numRandTopics, int months) {
        return (new PacksDao().getTotalUnboughtCatPacks(context) >= months * Constants.MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH)
                && ((numRandTopics - new TopicsDao().getTotalRandomTopics(context)) >= months * Constants.MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH);
    }
}
