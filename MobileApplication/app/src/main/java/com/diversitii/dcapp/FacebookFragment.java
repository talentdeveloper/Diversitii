//package com.diversitii.dcapp;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.support.annotation.NonNull;
//import android.support.v4.app.Fragment;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Toast;
//
//import com.facebook.CallbackManager;
//import com.facebook.FacebookCallback;
//import com.facebook.FacebookException;
//import com.facebook.login.LoginResult;
//import com.facebook.login.widget.LoginButton;
//
///**
// * Respond to result of Facebook login attempt.
// */
//public class FacebookFragment extends Fragment {
//    private static final String TAG = FacebookFragment.class.getName();
//
//    private CallbackManager mCallbackManager;
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_login, container, false);
//
//        mCallbackManager = CallbackManager.Factory.create();
//        final LoginButton loginButton = view.findViewById(R.id.facebookBtn);
//        loginButton.setReadPermissions("email");
//        loginButton.setFragment(this);
//        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
//            @Override
//            public void onSuccess(LoginResult loginResult) {
//                Toast.makeText(getContext(), getString(R.string.success_facebook), Toast.LENGTH_LONG).show();
//                // The login button will now read "Log out"
//            }
//
//            @Override
//            public void onCancel() {
//                Log.d(TAG, "FacebookCallback onCancel");
//            }
//
//            @Override
//            public void onError(FacebookException exception) {
//                Log.e(TAG, exception.getMessage());
//                Toast.makeText(getContext(), getString(R.string.error_facebook), Toast.LENGTH_LONG).show();
//            }
//        });
//        return view;
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        mCallbackManager.onActivityResult(requestCode, resultCode, data);
//    }
//}
