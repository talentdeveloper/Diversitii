package com.diversitii.dcapp;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean mDoAlertOverwrite = true;
    private String mFileExtension = Constants.CSV_FILE_EXT;
    private FirebaseFirestore mDatabase;
    private WriteBatch mBatch;
    private StorageReference mStorageRef;
//    private CollectionReference mAnswersRef;
    private String mMsgLog = "Messages:";
    private boolean update_flag_cats = false;
    private boolean update_flag_topic = false;
    private boolean update_flag_question = false;
    private boolean update_flag_answer = false;
    private boolean update_flag_subtop = false;
    private boolean update_flag_pack = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((RadioGroup) findViewById(R.id.radioAlert)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Do overwrite: " + (checkedId == R.id.radioYes));

                mDoAlertOverwrite = (checkedId == R.id.radioYes);
            }
        });

        ((RadioGroup) findViewById(R.id.radioFormat)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Use CSV: " + (checkedId == R.id.radioCsv));

                mFileExtension = (checkedId == R.id.radioCsv) ? Constants.CSV_FILE_EXT : Constants.JSON_FILE_EXT;
            }
        });

        doFirebaseAuth();
    }

    private void updateMessageLog(String msg) {
        mMsgLog += "\n" + msg;
        ((TextView) findViewById(R.id.tv_msg_log)).setText(mMsgLog);
    }

    private void doFirebaseAuth() {
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            mDatabase = FirebaseFirestore.getInstance();

                            initUi();
                        } else {
                            Log.e(TAG, "Firebase authentication failed: " + task.getException());
                            Toast.makeText(MainActivity.this,
                                    "Firebase authentication failed: " + task.getException(),
                                    Toast.LENGTH_SHORT).show();
                            updateMessageLog("Firebase authentication failed: " + task.getException());
                        }
                    }
                });
    }

    private void initUi() {
        findViewById(R.id.btnCats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoading();
                mStorageRef = FirebaseStorage.getInstance().getReference();

                JSONArray json = getFile("cats" + mFileExtension);
                mBatch = mDatabase.batch();
                fillCats(json, 0);
            }
        });
        findViewById(R.id.btnPacks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoading();

                JSONArray json = getFile("packs" + mFileExtension);
                mBatch = mDatabase.batch();
                fillPacks(json, 0);
            }
        });
        findViewById(R.id.btnTopics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoading();

                JSONArray json = getFile("topics" + mFileExtension);
                mBatch = mDatabase.batch();
                fillTopics(json, 0);
            }
        });
        findViewById(R.id.btnQuestions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoading();

                JSONArray json = getFile("questions" + mFileExtension);
                mBatch = mDatabase.batch();
                fillQuestions(json, 0);
            }
        });
        findViewById(R.id.btnAnswers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoading();

                JSONArray json = getFile("answers" + mFileExtension);
                mBatch = mDatabase.batch();
//                if (mDoAlertOverwrite) {
//                    mAnswersRef = mDatabase.collection(Constants.COLLECTION_ANSWERS);
//                }
                fillAnswers(json, 0);
            }
        });
        findViewById(R.id.btnPSubtopics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoading();

                JSONArray json = getFile("subtopics" + mFileExtension);
                mBatch = mDatabase.batch();
                fillSubtopics(json, 0);
            }
        });
        findViewById(R.id.btnClearLog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMsgLog = "Messages:";
                updateMessageLog("");
            }
        });

        // CAUTION Check async logic
//        findViewById(R.id.btnAll).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                fillCats();
//                fillPacks();
//                fillTopics();
//                fillQuestions();
//                fillAnswers();
//                fillSubtopics();
//            }
//        });
    }

    private void fillCats(JSONArray catsJson, int i) {
        if (catsJson != null) {
            if (catsJson.length() < 1) {
                hideLoading();
                Toast.makeText(this, "No CATEGORIES provided", Toast.LENGTH_LONG).show();
                updateMessageLog("No CATEGORIES provided");
            } else if (i < catsJson.length()) {
                // Create a new category
                int catId;
                String text; //, icon;
                try {
                    catId = Integer.parseInt(catsJson.getJSONObject(i).get("TopicId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "CATEGORY file item CategoryId missing or not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("CATEGORY file item CategoryId missing or not a number");
                    hideLoading();
                    return;
                }
                try {
                    text = catsJson.getJSONObject(i).get("Answers").toString();
                } catch (JSONException e) {
                    Toast.makeText(this, "CATEGORY file item with CategoryId " + catId + " missing Text", Toast.LENGTH_LONG).show();
                    updateMessageLog("CATEGORY file item with CategoryId " + catId + " missing Text");
                    hideLoading();
                    return;
                }
//                try {
//                    icon = catsJson.getJSONObject(i).get("Icon").toString();
//                    if (!icon.endsWith(Constants.ICON_FILE_EXT)) {
//                        Toast.makeText(this, "CATEGORY file item with CategoryId " + catId + " has Icon with invalid file extension", Toast.LENGTH_LONG).show();
//                        hideLoading();
//                        return;
//                    }
//                } catch (JSONException e) {
//                    Toast.makeText(this, "CATEGORY file item with CategoryId " + catId + " missing Icon", Toast.LENGTH_LONG).show();
//                    hideLoading();
//                    return;
//                }

                if (mDoAlertOverwrite) {
                    final int cId = catId;
                    final String txt = text;
                    final int iPlus = i + 1;
                    final JSONArray json = catsJson;
                    // Check if CategoryId exists (asynchronous)
                    mDatabase.collection(Constants.COLLECTION_CATEGORIES).document(cId + "").get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document != null && document.exists()&& update_flag_cats == false) { // second check required
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setTitle("Confirm:");
                                            builder.setMessage("CATEGORY " + cId + " exists, overwrite?");
                                            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Update document
                                                    mBatch.set(mDatabase.collection(Constants.COLLECTION_CATEGORIES).document(cId + ""),
                                                            new Category(txt));
                                                    uploadImage(cId);

                                                    dialog.dismiss();
                                                    update_flag_cats = true;

                                                    fillCats(json, iPlus);
                                                }
                                            });
                                            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    hideLoading();
                                                    Toast.makeText(MainActivity.this, "Operation canceled", Toast.LENGTH_LONG).show();
                                                    updateMessageLog("Operation canceled");
                                                    dialog.dismiss();
                                                }
                                            });
                                            AlertDialog alert = builder.create();
                                            alert.show();
                                        } else {
                                            // Add document
                                            mBatch.set(mDatabase.collection(Constants.COLLECTION_CATEGORIES).document(cId + ""),
                                                    new Category(txt));
                                            uploadImage(cId);

                                            fillCats(json, iPlus);
                                        }
                                    } else {
                                        hideLoading();
                                        Log.e(TAG, "Overwrite check failed with " + task.getException() + ", operation canceled");
                                        Toast.makeText(MainActivity.this,
                                                "Overwrite check failed with " + task.getException() + ", operation canceled",
                                                Toast.LENGTH_LONG).show();
                                        updateMessageLog("Overwrite check failed with " + task.getException() + ", operation canceled");
                                    }
                                }
                            });
                } else {
                    // Add or update document
                    mBatch.set(mDatabase.collection(Constants.COLLECTION_CATEGORIES).document(catId + ""),
                            new Category(text));
                    uploadImage(catId);

                    fillCats(catsJson, ++i);
                }
            } else { // base case, commit batch write
                hideLoading();
                mBatch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "Exceptions: " + task.getException());

                        Toast.makeText(MainActivity.this, "Completed CATEGORY upload", Toast.LENGTH_LONG).show();
                        updateMessageLog("Completed CATEGORY upload");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "CATEGORY upload error: " + e.toString());
                        Toast.makeText(MainActivity.this, "CATEGORY upload error: " + e.toString(), Toast.LENGTH_LONG).show();
                        updateMessageLog("CATEGORY upload error: " + e.toString());
                    }
                });
            }
        } else {
            hideLoading();
            Toast.makeText(this, "Unable to parse CATEGORY file", Toast.LENGTH_LONG).show();
            updateMessageLog("Unable to parse CATEGORY file");
        }
    }

    private void fillPacks(JSONArray packsJson, int i) {
        if (packsJson != null) {
            if (packsJson.length() < 1) {
                hideLoading();
                Toast.makeText(this, "No PACKS provided", Toast.LENGTH_LONG).show();
                updateMessageLog("No PACKS provided");
            } else if (i < packsJson.length()) {
                // Create a new pack
                int packId, catId = Constants.DEFAULT_CAT_ID;
                boolean doPromote;
                String topicIds;
                try {
                    packId = Integer.parseInt(packsJson.getJSONObject(i).get("PackId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "PACK file item PackId missing or not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("PACK file item PackId missing or not a number");
                    hideLoading();
                    return;
                }
                try {
                    doPromote = Boolean.parseBoolean(packsJson.getJSONObject(i).get("DoPromote").toString());
                } catch (Exception e) {
                    Toast.makeText(this, "PACK file item with PackId " + packId + " missing DoPromote or not a boolean (either true or false)", Toast.LENGTH_LONG).show();
                    updateMessageLog("PACK file item with PackId " + packId + " missing DoPromote or not a boolean (either true or false)");
                    hideLoading();
                    return;
                }
                try {
                    catId = Integer.parseInt(packsJson.getJSONObject(i).get("CategoryId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Log.w(TAG, "PACK file item with PackId " + packId + " missing CategoryId or not a number");
                }
                try {
                    topicIds = packsJson.getJSONObject(i).get("TopicIds").toString();
                    String[] tIds = topicIds.split(Constants.LIST_SEPARATOR);
                    int[] test = new int[tIds.length];
                    for (int j = 0; j < tIds.length; ++j) {
                        test[j] = Integer.parseInt(tIds[j]);
                    }
                    Log.d(TAG, "don't compile this code out: " + test[0]);
                } catch (Exception e) {
                    Toast.makeText(this, "PACK file item with PackId " + packId + " missing TopicIds or failed to parse TopicIds", Toast.LENGTH_LONG).show();
                    updateMessageLog("PACK file item with PackId " + packId + " missing TopicIds or failed to parse TopicIds");
                    hideLoading();
                    return;
                }

                if (mDoAlertOverwrite) {
                    final String pId = packId + "";
                    final int cId = catId;
                    final boolean promote = doPromote;
                    final String ids = topicIds;
                    final int iPlus = i + 1;
                    final JSONArray json = packsJson;
                    // Check if PackId exists (asynchronous)
                    mDatabase.collection(Constants.COLLECTION_PACKS).document(pId).get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document != null && document.exists() && update_flag_pack == false) { // second check required
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setTitle("Confirm:");
                                            builder.setMessage("PACK " + pId + " exists, overwrite?");
                                            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Update document
                                                    mBatch.set(mDatabase.collection(Constants.COLLECTION_PACKS).document(pId),
                                                            new Pack(promote, cId, ids));
                                                    dialog.dismiss();
                                                    update_flag_pack = true;

                                                    fillPacks(json, iPlus);
                                                }
                                            });
                                            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    hideLoading();
                                                    Toast.makeText(MainActivity.this, "Operation canceled", Toast.LENGTH_LONG).show();
                                                    updateMessageLog("Operation canceled");
                                                    dialog.dismiss();
                                                }
                                            });
                                            AlertDialog alert = builder.create();
                                            alert.show();
                                        } else {
                                            // Add document
                                            mBatch.set(mDatabase.collection(Constants.COLLECTION_PACKS).document(pId),
                                                    new Pack(promote, cId, ids));

                                            fillPacks(json, iPlus);
                                        }
                                    } else {
                                        hideLoading();
                                        Log.e(TAG, "Overwrite check failed with " + task.getException() + ", operation canceled");
                                        Toast.makeText(MainActivity.this,
                                                "Overwrite check failed with " + task.getException() + ", operation canceled",
                                                Toast.LENGTH_LONG).show();
                                        updateMessageLog("Overwrite check failed with " + task.getException() + ", operation canceled");
                                    }
                                }
                            });
                } else {
                    // Add or update document
                    mBatch.set(mDatabase.collection(Constants.COLLECTION_PACKS).document(packId + ""),
                            new Pack(doPromote, catId, topicIds));

                    fillPacks(packsJson, ++i);
                }
            } else { // base case, commit batch write
                hideLoading();
                mBatch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "Exceptions: " + task.getException());

                        Toast.makeText(MainActivity.this, "Completed PACK upload", Toast.LENGTH_LONG).show();
                        updateMessageLog("Completed PACK upload");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "PACK upload error: " + e.toString());
                        Toast.makeText(MainActivity.this, "PACK upload error: " + e.toString(), Toast.LENGTH_LONG).show();
                        updateMessageLog("PACK upload error: " + e.toString());
                    }
                });
            }
        } else {
            hideLoading();
            Toast.makeText(this, "Unable to parse PACK file", Toast.LENGTH_LONG).show();
            updateMessageLog("Unable to parse PACK file");
        }
    }

    private void fillTopics(JSONArray topicsJson, int i) {
        if (topicsJson != null) {
            if (topicsJson.length() < 1) {
                hideLoading();
                Toast.makeText(this, "No TOPICS provided", Toast.LENGTH_LONG).show();
                updateMessageLog("No TOPICS provided");
            } else if (i < topicsJson.length()) {
                // Create a new topic
                int topicId, catId;
                String text;
                try {
                    topicId = Integer.parseInt(topicsJson.getJSONObject(i).get("TopicId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "TOPIC file item TopicId missing or not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("TOPIC file item TopicId missing or not a number");
                    hideLoading();
                    return;
                }
                try {
                    text = topicsJson.getJSONObject(i).get("Topic").toString();
                } catch (JSONException e) {
                    Toast.makeText(this, "TOPIC file item with TopicId " + topicId + " missing Topic", Toast.LENGTH_LONG).show();
                    updateMessageLog("TOPIC file item with TopicId " + topicId + " missing Topic");
                    hideLoading();
                    return;
                }
                try {
                    catId = Integer.parseInt(topicsJson.getJSONObject(i).get("CategoryId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "TOPIC file item with TopicId " + topicId + " missing CategoryId or not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("TOPIC file item with TopicId " + topicId + " missing CategoryId or not a number");
                    hideLoading();
                    return;
                }

                if (mDoAlertOverwrite) {
                    final String tId = topicId + "";
                    final int cId = catId;
                    final String txt = text;
                    final int iPlus = i + 1;
                    final JSONArray json = topicsJson;
                    // Check if TopicId exists (asynchronous)
                    mDatabase.collection(Constants.COLLECTION_TOPICS).document(tId).get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document != null && document.exists() && update_flag_topic == false) { // second check required
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setTitle("Confirm:");
                                            builder.setMessage("TOPIC " + tId + " exists, overwrite?");
                                            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Update document
                                                    mBatch.set(mDatabase.collection(Constants.COLLECTION_TOPICS).document(tId),
                                                            new Topic(txt, cId));
                                                    dialog.dismiss();
                                                    update_flag_topic = true;

                                                    fillTopics(json, iPlus);
                                                }
                                            });
                                            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    hideLoading();
                                                    Toast.makeText(MainActivity.this, "Operation canceled", Toast.LENGTH_LONG).show();
                                                    updateMessageLog("Operation canceled");
                                                    dialog.dismiss();
                                                }
                                            });
                                            AlertDialog alert = builder.create();
                                            alert.show();
                                        } else {
                                            // Add document
                                            mBatch.set(mDatabase.collection(Constants.COLLECTION_TOPICS).document(tId),
                                                    new Topic(txt, cId));

                                            fillTopics(json, iPlus);
                                        }
                                    } else {
                                        hideLoading();
                                        Log.e(TAG, "Overwrite check failed with " + task.getException() + ", operation canceled");
                                        Toast.makeText(MainActivity.this,
                                                "Overwrite check failed with " + task.getException() + ", operation canceled",
                                                Toast.LENGTH_LONG).show();
                                        updateMessageLog("Overwrite check failed with " + task.getException() + ", operation canceled");
                                    }
                                }
                            });
                } else {
                    // Add or update document
                    mBatch.set(mDatabase.collection(Constants.COLLECTION_TOPICS).document(topicId + ""),
                            new Topic(text, catId));

                    fillTopics(topicsJson, ++i);
                }
            } else { // base case, commit batch write
                hideLoading();
                mBatch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "Exceptions: " + task.getException());

                        Toast.makeText(MainActivity.this, "Completed TOPIC upload", Toast.LENGTH_LONG).show();
                        updateMessageLog("Completed TOPIC upload");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "TOPIC upload error: " + e.toString());
                        Toast.makeText(MainActivity.this, "TOPIC upload error: " + e.toString(), Toast.LENGTH_LONG).show();
                        updateMessageLog("TOPIC upload error: " + e.toString());
                    }
                });
            }
        } else {
            hideLoading();
            Toast.makeText(this, "Unable to parse TOPIC file", Toast.LENGTH_LONG).show();
            updateMessageLog("Unable to parse TOPIC file");
        }
    }

    private void fillQuestions(JSONArray questionsJson, int i) {
        if (questionsJson != null) {
            if (questionsJson.length() < 1) {
                hideLoading();
                Toast.makeText(this, "No QUESTIONS provided", Toast.LENGTH_LONG).show();
                updateMessageLog("No QUESTIONS provided");
            } else if (i < questionsJson.length()) {
                // Create a new question
                int questionId, topicId, answerId, subtopicId, points;
                String text;
                try {
                    questionId = Integer.parseInt(questionsJson.getJSONObject(i).get("QuestionId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "QUESTION file item QuestionId missing or not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("QUESTION file item QuestionId missing or not a number");
                    hideLoading();
                    return;
                }
                try {
                    topicId = Integer.parseInt(questionsJson.getJSONObject(i).get("TopicId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "QUESTION file item with QuestionId " + questionId + " missing TopicId or not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("QUESTION file item with QuestionId " + questionId + " missing TopicId or not a number");
                    hideLoading();
                    return;
                }
                try {
                    answerId = Integer.parseInt(questionsJson.getJSONObject(i).get("AnswerId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "QUESTION file item with QuestionId " + questionId + " and TopicId " + topicId + " missing AnswerId or not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("QUESTION file item with QuestionId " + questionId + " and TopicId " + topicId + " missing AnswerId or not a number");
                    hideLoading();
                    return;
                }
                try {
                    if (questionsJson.getJSONObject(i).has("SubtopicId")) {
                        subtopicId = Integer.parseInt(questionsJson.getJSONObject(i).get("SubtopicId").toString());
                    } else {
                        subtopicId = Constants.DEFAULT_SUBTOPIC_ID;
                    }
                } catch (NumberFormatException e) {
                    subtopicId = Constants.DEFAULT_SUBTOPIC_ID;
//                    Toast.makeText(this, "QUESTION file item with QuestionId " + questionId + " SubtopicId is not a number", Toast.LENGTH_LONG).show();
//                    hideLoading();
//                    return;
                } catch (JSONException e) {
//                    subtopicId = Constants.DEFAULT_SUBTOPIC_ID;
                    Toast.makeText(this, "QUESTION file item with QuestionId " + questionId + " SubtopicId is not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("QUESTION file item with QuestionId " + questionId + " SubtopicId is not a number");
                    hideLoading();
                    return;
                }
                try {
                    if (questionsJson.getJSONObject(i).has("Points")) {
                        points = Integer.parseInt(questionsJson.getJSONObject(i).get("Points").toString());
                    } else {
                        points = Constants.DEFAULT_QUESTION_POINTS;
                    }
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "QUESTION file item with QuestionId " + questionId + " Points is not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("QUESTION file item with QuestionId " + questionId + " Points is not a number");
                    hideLoading();
                    return;
                }
                try {
                    text = questionsJson.getJSONObject(i).get("Questions").toString();
                } catch (JSONException e) {
                    Toast.makeText(this, "QUESTION file item with QuestionId " + questionId + " missing Questions", Toast.LENGTH_LONG).show();
                    updateMessageLog("QUESTION file item with QuestionId " + questionId + " missing Questions");
                    hideLoading();
                    return;
                }

                final String compositeId = "" + topicId + Constants.COMPOSITE_ID_SEPARATOR + questionId;
                if (mDoAlertOverwrite) {
                    final int qId = questionId, tId = topicId, aId = answerId, sId = subtopicId, pts = points;
                    final String txt = text;
                    final int iPlus = i + 1;
                    final JSONArray json = questionsJson;
                    // Check if question exists (asynchronous)
                    mDatabase.collection(Constants.COLLECTION_QUESTIONS).document(compositeId).get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document != null && document.exists() && update_flag_question == false) { // second check required
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setTitle("Confirm:");
                                            builder.setMessage("QUESTION " + qId + " exists for topic " + tId + ", overwrite?");
                                            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Update document
                                                    mBatch.set(mDatabase.collection(Constants.COLLECTION_QUESTIONS).document(compositeId),
                                                            new Question(qId, tId, txt, sId, pts, aId));
                                                    dialog.dismiss();
                                                    update_flag_question = true;
                                                    fillQuestions(json, iPlus);
                                                }
                                            });
                                            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    hideLoading();
                                                    Toast.makeText(MainActivity.this, "Operation canceled", Toast.LENGTH_LONG).show();
                                                    updateMessageLog("Operation canceled");
                                                    dialog.dismiss();
                                                }
                                            });
                                            AlertDialog alert = builder.create();
                                            alert.show();
                                        } else {
                                            // Add document
                                            mBatch.set(mDatabase.collection(Constants.COLLECTION_QUESTIONS).document(compositeId),
                                                    new Question(qId, tId, txt, sId, pts, aId));

                                            fillQuestions(json, iPlus);
                                        }
                                    } else {
                                        hideLoading();
                                        Log.e(TAG, "Overwrite check failed with " + task.getException() + ", operation canceled");
                                        Toast.makeText(MainActivity.this,
                                                "Overwrite check failed with " + task.getException() + ", operation canceled",
                                                Toast.LENGTH_LONG).show();
                                        updateMessageLog("Overwrite check failed with " + task.getException() + ", operation canceled");
                                    }
                                }
                            });
                } else {
                    // Add or update document
                    mBatch.set(mDatabase.collection(Constants.COLLECTION_QUESTIONS).document(compositeId),
                            new Question(questionId, topicId, text, subtopicId, points, answerId));

                    fillQuestions(questionsJson, ++i);
                }
            } else { // base case, commit batch write
                hideLoading();
                mBatch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "Exceptions: " + task.getException());

                        Toast.makeText(MainActivity.this, "Completed QUESTION upload", Toast.LENGTH_LONG).show();
                        updateMessageLog("Completed QUESTION upload");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "QUESTION upload error: " + e.toString());
                        Toast.makeText(MainActivity.this, "QUESTION upload error: " + e.toString(), Toast.LENGTH_LONG).show();
                        updateMessageLog("QUESTION upload error: " + e.toString());
                    }
                });
            }
        } else {
            hideLoading();
            Toast.makeText(this, "Unable to parse QUESTION file", Toast.LENGTH_LONG).show();
            updateMessageLog("Unable to parse QUESTION file");
        }
    }

    private void fillAnswers(JSONArray answersJson, int i) {
        if (answersJson != null) {
            if (answersJson.length() < 1) {
                hideLoading();
                Toast.makeText(this, "No ANSWERS provided", Toast.LENGTH_LONG).show();
                updateMessageLog("No ANSWERS provided");
            } else if (i < answersJson.length()) {
                // Create a new answer
                int answId, topicId;
                String text;
                try {
                    answId = Integer.parseInt(answersJson.getJSONObject(i).get("AnswerId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "ANSWER file item AnswerId missing or not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("ANSWER file item AnswerId missing or not a number");
                    hideLoading();
                    return;
                }
                try {
                    topicId = Integer.parseInt(answersJson.getJSONObject(i).get("TopicId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "ANSWER file item with AnswerId " + answId + " missing TopicId or not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("ANSWER file item with AnswerId " + answId + " missing TopicId or not a number");
                    hideLoading();
                    return;
                }
                try {
                    text = answersJson.getJSONObject(i).get("Answers").toString();
                } catch (JSONException e) {
                    Toast.makeText(this, "ANSWER file item with TopicId " + topicId + " and AnswerId " + answId + " missing Answers", Toast.LENGTH_LONG).show();
                    updateMessageLog("ANSWER file item with TopicId " + topicId + " and AnswerId " + answId + " missing Answers");
                    hideLoading();
                    return;
                }

                final String compositeId = "" + topicId + Constants.COMPOSITE_ID_SEPARATOR + answId;
                if (mDoAlertOverwrite) {
                    final int aId = answId;
                    final int tId = topicId;
                    final String txt = text;
                    final int iPlus = i + 1;
                    final JSONArray json = answersJson;
                    // Check if answer exists (asynchronous)
                    mDatabase.collection(Constants.COLLECTION_ANSWERS).document(compositeId).get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                //                    (mAnswersRef.whereEqualTo("answerId", aId).whereEqualTo("topicId", tId)).get()
//                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
//                                        final QuerySnapshot snapshot = task.getResult();
                                        final DocumentSnapshot document = task.getResult();
//                                        if (snapshot != null && !snapshot.isEmpty()) {
                                        if (document != null && document.exists() && update_flag_answer == false) {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setTitle("Confirm:");
                                            builder.setMessage("ANSWER " + aId + " for topic " + tId + " exists, overwrite?");
                                            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Update document
//                                                    mBatch.set(mDatabase.collection(Constants.COLLECTION_ANSWERS).document(
//                                                            snapshot.getDocuments().get(0).getId()),
//                                                            new Answer(aId, txt, tId));
                                                    mBatch.set(mDatabase.collection(Constants.COLLECTION_ANSWERS).document(compositeId),
                                                            new Answer(aId, txt, tId));
                                                    dialog.dismiss();
                                                    update_flag_answer = true;

                                                    fillAnswers(json, iPlus);
                                                }
                                            });
                                            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    hideLoading();
                                                    Toast.makeText(MainActivity.this, "Operation canceled", Toast.LENGTH_LONG).show();
                                                    updateMessageLog("Operation canceled");
                                                    dialog.dismiss();
                                                }
                                            });
                                            AlertDialog alert = builder.create();
                                            alert.show();
                                        } else {
                                            // Add document
                                            mBatch.set(mDatabase.collection(Constants.COLLECTION_ANSWERS).document(compositeId),
                                                    new Answer(aId, txt, tId));

                                            fillAnswers(json, iPlus);
                                        }
                                    } else {
                                        hideLoading();
                                        Log.e(TAG, "Overwrite check failed with " + task.getException() + ", operation canceled");
                                        Toast.makeText(MainActivity.this,
                                                "Overwrite check failed with " + task.getException() + ", operation canceled",
                                                Toast.LENGTH_LONG).show();
                                        updateMessageLog("Overwrite check failed with " + task.getException() + ", operation canceled");
                                    }
                                }
                            });
                } else {
                    // Add document
                    mBatch.set(mDatabase.collection(Constants.COLLECTION_ANSWERS).document(compositeId),
                            new Answer(answId, text, topicId));

                    fillAnswers(answersJson, ++i);
                }
            } else { // base case, commit batch write
                hideLoading();
                mBatch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "Exceptions: " + task.getException());

                        Toast.makeText(MainActivity.this, "Completed ANSWER upload", Toast.LENGTH_LONG).show();
                        updateMessageLog("Completed ANSWER upload");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "ANSWER upload error: " + e.toString());
                        Toast.makeText(MainActivity.this, "ANSWER upload error: " + e.toString(), Toast.LENGTH_LONG).show();
                        updateMessageLog("ANSWER upload error: " + e.toString());
                    }
                });
            }
        } else {
            hideLoading();
            Toast.makeText(this, "Unable to parse ANSWER file", Toast.LENGTH_LONG).show();
            updateMessageLog("Unable to parse ANSWER file");
        }
    }

    private void fillSubtopics(JSONArray subJson, int i) {
        if (subJson != null) {
            if (subJson.length() < 1) {
                hideLoading();
                Toast.makeText(this, "No SUBTOPICS provided", Toast.LENGTH_LONG).show();
                updateMessageLog("No SUBTOPICS provided");
            } else if (i < subJson.length()) {
                // Create a new subtopic
                int subId;
                String text;
                try {
                    subId = Integer.parseInt(subJson.getJSONObject(i).get("SubtopicId").toString());
                } catch (JSONException | NumberFormatException e) {
                    Toast.makeText(this, "SUBTOPIC file item SubtopicId missing or not a number", Toast.LENGTH_LONG).show();
                    updateMessageLog("SUBTOPIC file item SubtopicId missing or not a number");
                    hideLoading();
                    return;
                }
                try {
                    text = subJson.getJSONObject(i).get("Text").toString();
                } catch (JSONException e) {
                    Toast.makeText(this, "SUBTOPIC file item with SubtopicId " + subId + " missing Text", Toast.LENGTH_LONG).show();
                    updateMessageLog("SUBTOPIC file item with SubtopicId " + subId + " missing Text");
                    hideLoading();
                    return;
                }

                if (mDoAlertOverwrite) {
                    final String cId = subId + "";
                    final String txt = text;
                    final int iPlus = i + 1;
                    final JSONArray json = subJson;
                    // Check if SubtopicId exists (asynchronous)
                    mDatabase.collection(Constants.COLLECTION_SUBTOPICS).document(cId).get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document != null && document.exists() && update_flag_subtop == false) { // second check required
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setTitle("Confirm:");
                                            builder.setMessage("SUBTOPIC " + cId + " exists, overwrite?");
                                            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Update document
                                                    mBatch.set(mDatabase.collection(Constants.COLLECTION_SUBTOPICS).document(cId),
                                                            new Subtopic(txt));
                                                    dialog.dismiss();
                                                    update_flag_subtop = true;

                                                    fillSubtopics(json, iPlus);
                                                }
                                            });
                                            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    hideLoading();
                                                    Toast.makeText(MainActivity.this, "Operation canceled", Toast.LENGTH_LONG).show();
                                                    updateMessageLog("Operation canceled");
                                                    dialog.dismiss();
                                                }
                                            });
                                            AlertDialog alert = builder.create();
                                            alert.show();
                                        } else {
                                            // Add document
                                            mBatch.set(mDatabase.collection(Constants.COLLECTION_SUBTOPICS).document(cId),
                                                    new Subtopic(txt));

                                            fillSubtopics(json, iPlus);
                                        }
                                    } else {
                                        hideLoading();
                                        Log.e(TAG, "Overwrite check failed with " + task.getException() + ", operation canceled");
                                        Toast.makeText(MainActivity.this,
                                                "Overwrite check failed with " + task.getException() + ", operation canceled",
                                                Toast.LENGTH_LONG).show();
                                        updateMessageLog("Overwrite check failed with " + task.getException() + ", operation canceled");
                                    }
                                }
                            });
                } else {
                    // Add or update document
                    mBatch.set(mDatabase.collection(Constants.COLLECTION_SUBTOPICS).document(subId + ""),
                            new Subtopic(text));

                    fillSubtopics(subJson, ++i);
                }
            } else { // base case, commit batch write
                hideLoading();
                mBatch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "Exceptions: " + task.getException());

                        Toast.makeText(MainActivity.this, "Completed SUBTOPIC upload", Toast.LENGTH_LONG).show();
                        updateMessageLog("Completed SUBTOPIC upload");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "SUBTOPIC upload error: " + e.toString());
                        Toast.makeText(MainActivity.this, "SUBTOPIC upload error: " + e.toString(), Toast.LENGTH_LONG).show();
                        updateMessageLog("SUBTOPIC upload error: " + e.toString());
                    }
                });
            }
        } else {
            hideLoading();
            Toast.makeText(this, "Unable to parse SUBTOPIC file", Toast.LENGTH_LONG).show();
            updateMessageLog("Unable to parse SUBTOPIC file");
        }
    }

    private void showLoading() {
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        findViewById(R.id.main).setVisibility(View.INVISIBLE);
    }

    private void hideLoading() {
        findViewById(R.id.loading).setVisibility(View.GONE);
        findViewById(R.id.main).setVisibility(View.VISIBLE);
    }

    // Gets file contents as JSONArray from CSV/JSON file.
    private JSONArray getFile(String fileName) {
        if (mFileExtension.equals(Constants.CSV_FILE_EXT)) {
            try {
                JSONArray arr = new JSONArray();
                BufferedReader br = new BufferedReader(new InputStreamReader(this.getAssets().open(fileName)));
                String line1 = br.readLine();
                if (line1 != null) {
                    String[] keys = line1.split(",");
                    String line;
                    String[] lineArr;
                    while ((line = br.readLine()) != null) {
                        lineArr = line.split(",");
                        JSONObject obj = new JSONObject();
                        for (int i = 0; i < keys.length && i < lineArr.length && !keys[i].isEmpty(); ++i) {
                            obj.put(keys[i], lineArr[i]);
                        }
                        arr.put(obj);
                    }
                    return arr;
                } else {
                    Log.e(TAG, "Empty CSV file");
                    return null;
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Reading CSV file: " + e.toString());
            }
            return null;
        } else { // use JSON file
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.getAssets().open(fileName)));
                StringBuilder file = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    file.append(line).append('\n');
                }
                try {
                    return new JSONArray(file.toString());
                } catch (JSONException e) {
                    Log.e(TAG, "Error reading json");
                }
            } catch (IOException e) {
                Log.e(TAG, "Reading file: " + e.toString());
            }
            return null;
        }
    }

    // Uploads category's image to Firebase Storage's Constants.DIR_ICONS folder.
    private void uploadImage(final int catId) {
        try {
            UploadTask uploadTask = mStorageRef.child(Constants.DIR_ICONS + catId + Constants.ICON_FILE_EXT).putStream(getAssets().open("cat" + catId + "/icon.png"));
            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "CATEGORY " + catId + " icon upload error: " + e.toString());

                    Toast.makeText(MainActivity.this, "CATEGORY " + catId + " icon upload error: " + e.toString(), Toast.LENGTH_LONG).show();
                    updateMessageLog("CATEGORY " + catId + " icon upload error: " + e.toString());
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    // Uri downloadUrl = taskSnapshot.getDownloadUrl();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            Toast.makeText(MainActivity.this, "CATEGORY " + catId + " icon file parse error (missing file?)", Toast.LENGTH_LONG).show();
            updateMessageLog("CATEGORY " + catId + " icon file parse error (missing file?)");
        }
    }
}
