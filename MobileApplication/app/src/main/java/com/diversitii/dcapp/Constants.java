package com.diversitii.dcapp;

import android.content.Context;
import android.support.annotation.IntDef;
import android.util.Log;

import java.lang.annotation.Retention;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class Constants {
    private static final String TAG = Constants.class.getSimpleName();

    static final float MIN_TEXT_SIZE = 18;

    // Game mode
    static final int SINGLE_PLAYER = 0;
    static final int MULTIPLAYER = 1;
    static final int DEFAULT_GAME_MODE = SINGLE_PLAYER;
    static final int GAME_MODE_1 = 1; // default
    static final int GAME_MODE_2 = 2;
    static final int GAME_MODE_3 = 3;

    // Local database
    public static final int DB_FALSE = 0;
    public static final int DB_TRUE = 1;

    static final boolean DEFAULT_NOTIFICATIONS_OK = true;
    static final int PACKS_TO_PROMOTE_LIMIT = 10; // recommended to keep this number small to reduce loading time
    static final int DEFAULT_QUESTION_POINTS = 1;
    public static final int QUESTIONS_PER_TOPIC = 3;
    static final int NUM_TURNS = 3;
    public static final int MAX_ANSWERS = 6;
    public static final int TOPIC_DROP_DOWN_SZ = 5;
    static final int PACKS_PER_MULTIPACK = 10;
    static final int PAGE_SWITCH_TIME = 10000; // millis, 10 seconds
    static final int NO_PLAYER = -1;
    public static final String ICON_FILE_PREFIX = "comdiversitiidcapp_cat";
    static final String FORMAT_DATE = "dd-MM-yyyy";
    static final Locale LOCALE = Locale.ENGLISH;

    static String getCategoryIconFileName(int categoryId) {
        return ICON_FILE_PREFIX + categoryId + ICON_FILE_EXT;
    }

    // Settings, defaults are at index 0
    static int[] TIMER_VALUES = {0, 10, 15, 20, 30, 60, 120}; // seconds!
    static int[] LIVES_VALUES = {5, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 800, 900, 999};
    static int[] SCORES_VALUES = {5, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90, 100, 200, 300, 400, 500, 600, 700, 800, 900, 999}; // score at which to end game
    static int[] POINTS_VALUES = {1, 2, 3, 4, 5, 10}; // multiply question points value by to get score

    private static final int NUM_TOPIC_CHOICES = 4;
    private static final int NUM_TOPIC_CHOICES_PORTRAIT = 2;

    static int getMaxTopics(boolean isPortrait) {
        return (isPortrait) ? NUM_TOPIC_CHOICES_PORTRAIT : NUM_TOPIC_CHOICES;
    }

    private static final int NUM_PLAYERS = 4;
    private static final int NUM_PLAYERS_PORTRAIT = 2;

    static int getMaxPlayers(boolean isPortrait) {
        return (isPortrait) ? NUM_PLAYERS_PORTRAIT : NUM_PLAYERS;
    }

    // ****************** FIREBASE ******************

    static final String STORAGE_URL = "gs://mobileapplication-3c913.appspot.com/"; // DONE

    // Topic
    static final String NOTIFICATIONS_TOPIC = "notifications";

    // Firestore
    static final String COLLECTION_CATEGORIES = "categories";
    static final String COLLECTION_ANSWERS = "answers";
    static final String COLLECTION_SUBTOPICS = "subtopics";
    static final String COLLECTION_TOPICS = "topics";
    static final String COLLECTION_PACKS = "packs";
    static final String COLLECTION_QUESTIONS = "questions";

    static final String FIELD_DO_PROMOTE = "doPromote";
    static final String FIELD_CATEGORY_ID = "categoryId";
    static final String FIELD_SUBTOPIC_ID = "subtopicId";
    static final String FIELD_CORRECT_ANSWER_ID = "correctAnswerId";
    static final String FIELD_TOPIC_IDS = "topicIds";
    static final String FIELD_POINTS = "points";
    static final String FIELD_TEXT = "text";

    // Realtime database
    static final String METADATA_KEY = "pack_count";
    // Storage
    static final String OFFERS_FILE = "offers/offers.txt";
    static final int MAX_OFFERS_SIZE = 1024; // 1KB
    static final String DIR_ICONS = "icons/";
    static final String ICON_FILE_EXT = ".png";

    static final int FREE_PACK_ID = 0;
    public static final int DEFAULT_CAT_ID = -1;
    public static final int DEFAULT_SUBTOPIC_ID = -1;
    static final String TOPIC_ID_LIST_SEPARATOR = ";";
    static final String COLLECTION_ID_SEPARATOR = ".";

//    public static final String TOPICS_FILE = "topics.json";
//    public static final String QUESTIONS_FILE = "questions.json";
//    public static final String ANSWERS_FILE = "answers.json";
//    static final long FREE_PACK_UPDATE_TIME = 1500408484; // Unix timestamp (seconds) that included pack data was updated

    // ****************** Products ******************
    /*
     * NOTE
     * Ideally, instead of storing the entire BASE64_PUBLIC_KEY string literal here embedded
     * in the program, the app would construct the key at runtime from pieces or use bit
     * manipulation (for example, XOR with some other string) to hide the actual key. The key
     * itself is not secret information, but we don't want to make it easy for an attacker to
     * replace the public key with one of their own and then fake messages from the server.
     */
    static final String BASE64_PUBLIC_KEY =          "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjIjyuF9ucHbTss/qfmICqYiQU0EU1SZ5YcMEYEFeGGkGsfMhwxCuboxDViPMmDb0WdEyzmJRIEyrwb4reIOinOvbYa0IFek6BhG4Eg/3MBYJHazCvh5R5aztY6XRa/B4Q2Z0jR7aPxs64/Ac7B+DroUIOR6cXz7jeJqLVWHMlmvLERwBKvUEMWwJlZYpbVX2ENPQ5TkokI89sbed1oHwqD093G93jEmIc9LluoNTRfC4h1hcYh3ew5F/3uArmkLRKXGvQR/qIxfAiPEkiqshN5NFSaqmDFvqi+JjR4hweu6by8/bIwEYrqCGur86MLVZ8iM2SeC02bkfQag/9+sGCwIDAQAB";// DONE
    static final String SKU_MEMBERSHIP = "membership";
    private static final String SKU_PACK_PREFIX = "pack";
    private static final String SKU_MULTIPACK_PREFIX = "multipack";
    private static final String SKU_CATEGORY_PREFIX = "cat";

    static String getPackSku(int packNum) {
        return SKU_PACK_PREFIX + packNum;
    }

    static String getMultipackSku(int packNum) {
        return SKU_MULTIPACK_PREFIX + packNum;
    }

    static String getCatPackSku(int catId) {
        return SKU_CATEGORY_PREFIX + catId;
    }

//    static String getCategoryPackSku(int topicNum) {
//        return SKU_CATEGORY_PREFIX + topicNum;
//    }

    private static final String SKU_MEMBERSHIP_COPPER = "copper";
    private static final String SKU_MEMBERSHIP_BRONZE = "bronze";
    private static final String SKU_MEMBERSHIP_SILVER = "silver";
    private static final String SKU_MEMBERSHIP_GOLD = "gold";
    private static final String SKU_MEMBERSHIP_DIAMOND_ANNUAL = "diamond_annual";
    private static final String SKU_MEMBERSHIP_DIAMOND_BIANNUAL = "diamond_biannual";
    private static final String SKU_MEMBERSHIP_DIAMOND_QUARTERLY = "diamond_quarterly";
    private static final String SKU_MEMBERSHIP_RED_DIAMOND_ANNUAL = "red_diamond_annual";
    private static final String SKU_MEMBERSHIP_RED_DIAMOND_BIANNUAL = "red_diamond_biannual";
    private static final String SKU_MEMBERSHIP_RED_DIAMOND_QUARTERLY = "red_diamond_quarterly";

    static String getMembershipSku(@Membership int membershipId) {
        switch (membershipId) {
            case MEMBERSHIP_COPPER:
                return SKU_MEMBERSHIP_COPPER;
            case MEMBERSHIP_BRONZE:
                return SKU_MEMBERSHIP_BRONZE;
            case MEMBERSHIP_SILVER:
                return SKU_MEMBERSHIP_SILVER;
            case MEMBERSHIP_GOLD:
                return SKU_MEMBERSHIP_GOLD;
            case MEMBERSHIP_DIAMOND_ANNUAL:
                return SKU_MEMBERSHIP_DIAMOND_ANNUAL;
            case MEMBERSHIP_DIAMOND_BIANNUAL:
                return SKU_MEMBERSHIP_DIAMOND_BIANNUAL;
            case MEMBERSHIP_DIAMOND_QUARTERLY:
                return SKU_MEMBERSHIP_DIAMOND_QUARTERLY;
            case MEMBERSHIP_RED_DIAMOND_ANNUAL:
                return SKU_MEMBERSHIP_RED_DIAMOND_ANNUAL;
            case MEMBERSHIP_RED_DIAMOND_BIANNUAL:
                return SKU_MEMBERSHIP_RED_DIAMOND_BIANNUAL;
            case MEMBERSHIP_RED_DIAMOND_QUARTERLY:
                return SKU_MEMBERSHIP_RED_DIAMOND_QUARTERLY;
            case MEMBERSHIP_INVALID:
            default:
                Log.e(TAG, "Unhandled membership option " + membershipId);
                return null;
        }
    }

    @Retention(SOURCE)
    @IntDef({MEMBERSHIP_INVALID, MEMBERSHIP_COPPER, MEMBERSHIP_BRONZE, MEMBERSHIP_SILVER,
            MEMBERSHIP_GOLD, MEMBERSHIP_DIAMOND_ANNUAL, MEMBERSHIP_DIAMOND_BIANNUAL,
            MEMBERSHIP_DIAMOND_QUARTERLY, MEMBERSHIP_RED_DIAMOND_ANNUAL, MEMBERSHIP_RED_DIAMOND_BIANNUAL,
            MEMBERSHIP_RED_DIAMOND_QUARTERLY})
    @interface Membership {
    }

    static final int MEMBERSHIP_INVALID = -1;
    static final int MEMBERSHIP_COPPER = 0;
    static final int MEMBERSHIP_BRONZE = 1;
    static final int MEMBERSHIP_SILVER = 2;
    static final int MEMBERSHIP_GOLD = 3;
    static final int MEMBERSHIP_DIAMOND_ANNUAL = 4;
    static final int MEMBERSHIP_DIAMOND_BIANNUAL = 5;
    static final int MEMBERSHIP_DIAMOND_QUARTERLY = 6;
    static final int MEMBERSHIP_RED_DIAMOND_ANNUAL = 7;
    static final int MEMBERSHIP_RED_DIAMOND_BIANNUAL = 8;
    static final int MEMBERSHIP_RED_DIAMOND_QUARTERLY = 9;

    static String getMembershipName(Context context, @Membership int membershipId) {
        switch (membershipId) {
            case MEMBERSHIP_COPPER:
                return context.getString(R.string.copper);
            case MEMBERSHIP_BRONZE:
                return context.getString(R.string.bronze);
            case MEMBERSHIP_SILVER:
                return context.getString(R.string.silver);
            case MEMBERSHIP_GOLD:
                return context.getString(R.string.gold);
            case MEMBERSHIP_DIAMOND_ANNUAL:
                return context.getString(R.string.diamond_annual);
            case MEMBERSHIP_DIAMOND_BIANNUAL:
                return context.getString(R.string.diamond_biannual);
            case MEMBERSHIP_DIAMOND_QUARTERLY:
                return context.getString(R.string.diamond_quarterly);
            case MEMBERSHIP_RED_DIAMOND_ANNUAL:
                return context.getString(R.string.red_diamond_annual);
            case MEMBERSHIP_RED_DIAMOND_BIANNUAL:
                return context.getString(R.string.red_diamond_biannual);
            case MEMBERSHIP_RED_DIAMOND_QUARTERLY:
                return context.getString(R.string.red_diamond_quarterly);
            case MEMBERSHIP_INVALID:
            default:
                Log.e(TAG, "Unhandled membership option " + membershipId);
                return context.getString(R.string.membership);
        }
    }

    static final int MEMBERSHIP_COPPER_LAST_PACK = 25;
    static final int MEMBERSHIP_BRONZE_LAST_PACK = 50;
    static final int MEMBERSHIP_SILVER_LAST_PACK = 75;
    static final int MEMBERSHIP_GOLD_LAST_PACK = 100;
    static final int MEMBERSHIP_DIAMOND_CAT_PACKS_PER_MONTH = 1;
    static final int MEMBERSHIP_RED_DIAMOND_CAT_PACKS_PER_MONTH = 2;
    static final int MEMBERSHIP_DIAMOND_RAND_TOPICS_PER_MONTH = 35;
    static final int MEMBERSHIP_RED_DIAMOND_RAND_TOPICS_PER_MONTH = 105;
    static final int MONTHS_ANNUAL = 12;
    static final int MONTHS_BIANNUAL = 6;
    static final int MONTHS_QUARTERLY = 3;


    // ****************** SharedPreferences ******************
    static final String SHPREFS = "SHPREFS";
    static final String SHPREFS_IS_DATABASE_INIT = "SHPREFS_IS_DATABASE_INIT"; // boolean, default false
    static final String SHPREFS_FIRST_PLAYER = "SHPREFS_FIRST_PLAYER"; // int, player ID from 0 -- use to determine if a game is in progress (NO_PLAYER if no game)
    static final String SHPREFS_GAME_MODE = "SHPREFS_GAME_MODE"; // int SINGLE_PLAYER or MULTIPLAYER
    static final String SHPREFS_NAME_SINGLE_PLAYER = "SHPREFS_NAME_SINGLE_PLAYER"; // string
    private static final String SHPREFS_NAME_PLAYER1 = "SHPREFS_NAME_PLAYER1"; // string
    private static final String SHPREFS_NAME_PLAYER2 = "SHPREFS_NAME_PLAYER2"; // string
    private static final String SHPREFS_NAME_PLAYER3 = "SHPREFS_NAME_PLAYER3"; // string
    private static final String SHPREFS_NAME_PLAYER4 = "SHPREFS_NAME_PLAYER4"; // string
    static final String SHPREFS_SCORE_SINGLE = "SHPREFS_SCORE_SINGLE"; // int
    static final String SHPREFS_SCORE_PLAYER1 = "SHPREFS_SCORE_PLAYER1"; // int
    static final String SHPREFS_SCORE_PLAYER2 = "SHPREFS_SCORE_PLAYER2"; // int
    static final String SHPREFS_SCORE_PLAYER3 = "SHPREFS_SCORE_PLAYER3"; // int
    static final String SHPREFS_SCORE_PLAYER4 = "SHPREFS_SCORE_PLAYER4"; // int
    static final String SHPREFS_NOTIFICATIONS_OK = "SHPREFS_NOTIFICATIONS_OK"; // boolean
    static final String SHPREFS_MUSIC_ON = "SHPREFS_MUSIC_ON"; // boolean
    static final String SHPREFS_SOUND_EFFECTS_VOL = "SHPREFS_SOUND_EFFECTS_VOL"; // int, 0 - 100
    static final String SHPREFS_OFFER = "SHPREFS_OFFER"; // string, cached offer
    static final String SHPREFS_OFFER_UPDATE_MILLIS = "SHPREFS_OFFER_UPDATE_MILLIS"; // long, offer file update time
    static final String SHPREFS_SETTINGS_TIMER = "SHPREFS_SETTINGS_TIMER"; // int, index into TIMER_VALUES
    static final String SHPREFS_SETTINGS_POINTS = "SHPREFS_SETTINGS_POINTS"; // int, index into POINTS_VALUES
    static final String SHPREFS_SETTINGS_LIVES = "SHPREFS_SETTINGS_LIVES"; // int, index into LIVES_VALUES
    static final String SHPREFS_SETTINGS_SCORES = "SHPREFS_SETTINGS_SCORES"; // int, index into SCORES_VALUES
    static final String SHPREFS_LIVES_SINGLE_PLAYER = "SHPREFS_LIVES_SINGLE_PLAYER"; // int
    static final String SHPREFS_LIVES_PLAYER1 = "SHPREFS_LIVES_PLAYER1"; // int
    static final String SHPREFS_LIVES_PLAYER2 = "SHPREFS_LIVES_PLAYER2"; // int
    static final String SHPREFS_LIVES_PLAYER3 = "SHPREFS_LIVES_PLAYER3"; // int
    static final String SHPREFS_LIVES_PLAYER4 = "SHPREFS_LIVES_PLAYER4"; // int
    static final String SHPREFS_USER_ADDED_TOPIC_ID = "SHPREFS_USER_ADDED_TOPIC_ID"; // int
    static final String SHPREFS_USER_GAME_MODE = "SHPREFS_USER_GAME_MODE"; // int
    static final String SHPREFS_BOUGHT_COPPER = "SHPREFS_BOUGHT_COPPER"; // boolean
    static final String SHPREFS_BOUGHT_BRONZE = "SHPREFS_BOUGHT_BRONZE"; // boolean
    static final String SHPREFS_BOUGHT_SILVER = "SHPREFS_BOUGHT_SILVER"; // boolean
    static final String SHPREFS_BOUGHT_GOLD = "SHPREFS_BOUGHT_GOLD"; // boolean
    static final String SHPREFS_SUBSCRIBED_DIAMOND = "SHPREFS_SUBSCRIBED_DIAMOND"; // boolean
    //    static final String SHPREFS_SUBSCRIBED_RED_DIAMOND = "SHPREFS_SUBSCRIBED_RED_DIAMOND"; // boolean
    static final String SHPREFS_SUBSCRIPTION_DATE_DIAMOND = "SHPREFS_SUBSCRIPTION_DATE_DIAMOND"; // String
//    static final String SHPREFS_SUBSCRIPTION_DATE_RED_DIAMOND = "SHPREFS_SUBSCRIPTION_DATE_RED_DIAMOND"; // String

    // SharedPreferences key prefixes
    static final String SHPREFS_PACKS_UPDATE = "SHPREFS_PACKS_UPDATE"; // long, Unix timestamp (seconds)
    //    private static final String SHPREFS_TOPICS_UPDATE = "SHPREFS_TOPICS_UPDATE"; // long, Unix timestamp (seconds)
//    private static final String SHPREFS_QUESTIONS_UPDATE = "SHPREFS_QUESTIONS_UPDATE"; // long, Unix timestamp (seconds)
//    private static final String SHPREFS_ANSWERS_UPDATE = "SHPREFS_ANSWERS_UPDATE"; // long, Unix timestamp (seconds)
//    private static final String SHPREFS_IS_PACK_UNLOCKED = "SHPREFS_IS_PACK_UNLOCKED"; // boolean
    private static final String SHPREFS_IS_MULTIPACK_UNLOCKED = "SHPREFS_IS_MULTIPACK_UNLOCKED"; // boolean

//    /**
//     * Gets the SharedPreferences key for the update time of the given pack's topics file.
//     *
//     * @param packNumber the pack numberPacks count from 0 (0 is the free pack)
//     */
//    static String getTopicsUpdateTime(int packNumber) {
//        return SHPREFS_TOPICS_UPDATE + packNumber;
//    }

//    /**
//     * Gets the SharedPreferences key for the update time of the given pack's questions file.
//     *
//     * @param packNumber the pack numberPacks count from 0 (0 is the free pack)
//     */
//    static String getQuestionsUpdateTime(int packNumber) {
//        return SHPREFS_QUESTIONS_UPDATE + packNumber;
//    }
//
//    /**
//     * Gets the SharedPreferences key for the update time of the given pack's answers file.
//     *
//     * @param packNumber the pack numberPacks count from 0 (0 is the free pack)
//     */
//    static String getAnswersUpdateTime(int packNumber) {
//        return SHPREFS_ANSWERS_UPDATE + packNumber;
//    }

//    /**
//     * Gets the SharedPreferences key for storing whether or not the given pack is unlocked.
//     *
//     * @param packNumber the pack number. Packs count from 0 (0 is the free pack)
//     */
//    static String getPackKey(int packNumber) {
//        return SHPREFS_IS_PACK_UNLOCKED + packNumber;
//    }

    /**
     * Gets the SharedPreferences key for storing whether or not the given multipack is unlocked.
     *
     * @param multipackNumber the multipack number. Packs count from 1
     */
    static String getMultipackKey(int multipackNumber) {
        return SHPREFS_IS_MULTIPACK_UNLOCKED + multipackNumber;
    }

    private static final String[] PLAYER_SCORE_KEYS = new String[]{SHPREFS_SCORE_PLAYER1, SHPREFS_SCORE_PLAYER2, SHPREFS_SCORE_PLAYER3, SHPREFS_SCORE_PLAYER4};
    private static final String[] PLAYER_SCORE_KEYS_PORTRAIT = new String[]{SHPREFS_SCORE_PLAYER1, SHPREFS_SCORE_PLAYER2};

    static String[] getPlayerScoreKeys(boolean isPortrait) {
        return (isPortrait) ? PLAYER_SCORE_KEYS_PORTRAIT : PLAYER_SCORE_KEYS;
    }

    // Player name display resource IDs
    private static final int[] PLAYER_NAME_IDS = new int[]{R.id.playerName1, R.id.playerName2, R.id.playerName3, R.id.playerName4};
    private static final int[] PLAYER_NAME_IDS_PORTRAIT = new int[]{R.id.playerName1, R.id.playerName2};

    static int[] getPlayerNameIds(boolean isPortrait) {
        return (isPortrait) ? PLAYER_NAME_IDS_PORTRAIT : PLAYER_NAME_IDS;
    }

    private static final int[] PLAYER_ICON_IDS = new int[]{R.id.iv_player_icon1, R.id.iv_player_icon2, R.id.iv_player_icon3, R.id.iv_player_icon4};
    private static final int[] PLAYER_ICON_IDS_PORTRAIT = new int[]{R.id.iv_player_icon1, R.id.iv_player_icon2};

    static int[] getPlayerIconIds(boolean isPortrait) {
        return (isPortrait) ? PLAYER_ICON_IDS_PORTRAIT : PLAYER_ICON_IDS;
    }

    private static final int[] PLAYER_DEFAULT_NAME_IDS = new int[]{R.string.player_1, R.string.player_2, R.string.player_3, R.string.player_4};
    private static final int[] PLAYER_DEFAULT_NAME_IDS_PORTRAIT = new int[]{R.string.player_1, R.string.player_2};

    static int[] getPlayerDefaultNameIds(boolean isPortrait) {
        return (isPortrait) ? PLAYER_DEFAULT_NAME_IDS_PORTRAIT : PLAYER_DEFAULT_NAME_IDS;
    }

    // Player name display resource IDs to player name storage
    private static final Map<Integer, String> PLAYER_NAME_STORAGE;
    private static final Map<Integer, String> PLAYER_NAME_STORAGE_PORTRAIT;

    static Map<Integer, String> getPlayerNameStorageIds(boolean isPortrait) {
        return (isPortrait) ? PLAYER_NAME_STORAGE_PORTRAIT : PLAYER_NAME_STORAGE;
    }


    static {
        Map<Integer, String> tmp = new HashMap<>();
        tmp.put(PLAYER_NAME_IDS[0], SHPREFS_NAME_PLAYER1);
        tmp.put(PLAYER_NAME_IDS[1], SHPREFS_NAME_PLAYER2);
        tmp.put(PLAYER_NAME_IDS[2], SHPREFS_NAME_PLAYER3);
        tmp.put(PLAYER_NAME_IDS[3], SHPREFS_NAME_PLAYER4);
        PLAYER_NAME_STORAGE = Collections.unmodifiableMap(tmp);

        Map<Integer, String> tmp2 = new HashMap<>();
        tmp2.put(PLAYER_NAME_IDS[0], SHPREFS_NAME_PLAYER1);
        tmp2.put(PLAYER_NAME_IDS[1], SHPREFS_NAME_PLAYER2);
        PLAYER_NAME_STORAGE_PORTRAIT = Collections.unmodifiableMap(tmp2);
    }
}
