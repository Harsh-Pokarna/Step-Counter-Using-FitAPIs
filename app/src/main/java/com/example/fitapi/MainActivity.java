package com.example.fitapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    GoogleSignInClient mGoogleSignInClient;

    int RC_SIGN_IN = 0, GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1;

    Button signInBtn;
    TextView responseDataTv;

    FitnessOptions
            fitnessOptions = FitnessOptions.builder().addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ).build();;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {

        signInBtn = findViewById(R.id.sign_in_btn);
        responseDataTv = findViewById(R.id.sign_in_btn);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            accessGoogleFit();
        }

        signInBtn.setOnClickListener(view -> {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            // Build a GoogleSignInClient with the options specified by gso.
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                accessGoogleFit();
            }
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            setUpFitness();
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("TAG", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private void setUpFitness() {

        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                    account,
                    fitnessOptions);
        } else {
            accessGoogleFit();
        }
    }

    private void accessGoogleFit() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusMonths(1);
            long endSeconds = end.atZone(ZoneId.systemDefault()).toEpochSecond();
            long startSeconds = start.atZone(ZoneId.systemDefault()).toEpochSecond();

            DataReadRequest readRequest = new DataReadRequest.Builder()
                    .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .setTimeRange(startSeconds, endSeconds, TimeUnit.SECONDS)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .build();

            GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
            Fitness.getHistoryClient(this, account).readData(readRequest)
                    .addOnSuccessListener(dataReadResponse -> {
                        Log.e("TAG", "response data was success");
                        responseDataTv.setText(dataReadResponse.getBuckets().toString());

                    }).addOnFailureListener(e -> {
                Log.e("TAG", "Fetching Data failed: " + e.getMessage());
                responseDataTv.setText("Error: " + e.getMessage());
            });

        }

    }
}