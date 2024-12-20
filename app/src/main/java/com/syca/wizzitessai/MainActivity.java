package com.syca.wizzitessai;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import android.util.Log;
import android.view.WindowManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.syca.wizzitessai.databinding.CaptureOtpBinding;
import com.wizzitdigital.emv.sdk.EMVAdapter;
import com.wizzitdigital.emv.sdk.EMVAdapterListener;
import com.wizzitdigital.emv.sdk.EMVCfgVars;

public class MainActivity extends AppCompatActivity implements EMVAdapterListener {

    private EMVAdapter emvAdapter;
    private boolean isCurrentActivity = false;
    private final String merchantToken = "1234567788=";
    private JSONObject merchantDetailJson = new JSONObject();
    private String deviceId = "";
    private int txRef = 0;
    private String amount = "0";
    private final AlphaAnimation buttonClick = new AlphaAnimation(1f, 0.8f);
    private StringBuilder sb = new StringBuilder();
    private String session = "";
    private JSONObject txResult = new JSONObject();
    private boolean countdownInterfaceInitialized = false;
    private Handler mHandler;
    //private TextView tv_btn;
    private Dialog dialogCaptureOtp;
    private CaptureOtpBinding bindCaptureOtp;

    private List<IntKeyStringValue> pinCapabilities = Arrays.asList(
            new IntKeyStringValue(0, "Disabled"),
            new IntKeyStringValue(1, "Enabled")
    );
    private List<IntKeyStringValue> signatureFlags = Arrays.asList(
            new IntKeyStringValue(0, "Disabled"),
            new IntKeyStringValue(1, "Enabled")
    );
    private List<IntKeyStringValue> transactionTypes = Arrays.asList(
            new IntKeyStringValue(0, "Purchase"),
            new IntKeyStringValue(1, "Refund")
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(this.getSupportActionBar()!=null){
            this.getSupportActionBar().hide();
        }

        // Disable screen capture in production
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.init_adapter);
        setSupportActionBar(findViewById(R.id.toolbar));

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Log custom errors or messages
        FirebaseCrashlytics.getInstance().log("Lancement de l'application");

        try {

            emvAdapter = new EMVAdapter(this, null);
            //emvAdapter.setAdapterActivity(this);

            deviceId = emvAdapter.getMobileDeviceId();
            
            emvAdapter.checkDeviceRegistration();

        } catch (Exception ex) {
            new RuntimeException("Crash WIZZIT: "+ex.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isCurrentActivity = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isCurrentActivity = false;
    }

    @Override
    public void onAdapterInitializing() {
        // Not implemented
    }

    @Override
    public void onAdapterInitComplete(boolean isInitialized, String reason) {
        if (isInitialized) {
            showConfig();
        }
    }

    @Override
    public void onSessionInitComplete(boolean isInitialized, String reason) {
        try {
            txRef += 1;
            if (isInitialized) {
                runOnUiThread(() -> {
                    setContentView(R.layout.tap_to_pay);
                    ((TextView) findViewById(R.id.textViewTransactionRef)).setText(new DecimalFormat("00000").format(txRef));
                    ((TextView) findViewById(R.id.textViewAmount)).setText(formatAmount());
                    Button btnCancel = findViewById(R.id.buttonCancel);
                    btnCancel.setOnClickListener(v -> cancelSessionClicked());
                });
            }else{
                Toast.makeText(MainActivity.this, reason, Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, "Error Session "+ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSessionCountdown(int remainingSeconds) {
        try {
            runOnUiThread(() -> {
                if (!countdownInterfaceInitialized) {
                    ((TextView) findViewById(R.id.textViewTapToPay)).setText("Tap Card To Pay");
                    ((TextView) findViewById(R.id.textViewSecondsLeft)).setText("seconds left");
                    ((Button) findViewById(R.id.buttonCancel)).setVisibility(View.VISIBLE);
                    countdownInterfaceInitialized = true;
                }
                ((TextView) findViewById(R.id.textViewTimeoutSeconds)).setText(String.valueOf(remainingSeconds));
            });
        } catch (Exception ex) {
            // Handle exception
        }
    }

    @Override
    public void onCardProcessing() {
        runOnUiThread(() -> {
            try {
                ((TextView) findViewById(R.id.textViewTapToPay)).setText("Please Wait...");
                ((TextView) findViewById(R.id.textViewSecondsLeft)).setText("");
                ((TextView) findViewById(R.id.textViewTimeoutSeconds)).setText("");
                ((Button) findViewById(R.id.buttonCancel)).setVisibility(View.INVISIBLE);
                countdownInterfaceInitialized = false;
            } catch (Exception ex) {
                // Handle exception
            }
        });
    }

    @Override
    public void onCardProcessingComplete() {
        runOnUiThread(() -> {
            try {
                ((TextView) findViewById(R.id.textViewTapToPay)).setText("Please remove card");
                ((TextView) findViewById(R.id.textViewSecondsLeft)).setText("");
                ((TextView) findViewById(R.id.textViewTimeoutSeconds)).setText("");
                // Beeper.beep(300); // Assuming Beeper is a utility class you have.
            } catch (Exception ex) {
                // Handle exception
            }
        });
    }

    @Override
    public void onCardRemoved() {
        runOnUiThread(() -> {
            try {
                ((TextView) findViewById(R.id.textViewTapToPay)).setText("Please wait...");
                ((TextView) findViewById(R.id.textViewSecondsLeft)).setText("");
                ((TextView) findViewById(R.id.textViewTimeoutSeconds)).setText("");
            } catch (Exception ex) {
                // Handle exception
            }
        });
    }

    @Override
    public void onSessionComplete(boolean isSuccessful, String statusCode, String reason, Map<String, String> sessionData) {
        try {
            runOnUiThread(() -> {
                setContentView(R.layout.tx_result);
                ((TextView) findViewById(R.id.textViewSuccess)).setText(isSuccessful ? "APPROVED" : "FAILED");
                ((TextView) findViewById(R.id.textViewAuth)).setText(statusCode);
                ((TextView) findViewById(R.id.textViewReason)).setText(reason);
                ((TextView) findViewById(R.id.textViewAmount)).setText(formatAmount());
                String formattedDate = new SimpleDateFormat("dd/MM/yyyy hh:mm").format(Calendar.getInstance().getTime());
                ((TextView) findViewById(R.id.textViewDate)).setText(formattedDate);
                ((TextView) findViewById(R.id.textViewReference)).setText(new DecimalFormat("00000").format(txRef));
            });
        } catch (Exception ex) {
            // Handle exception
        }
    }

    private void cancelSessionClicked() {
        try {
            emvAdapter.cancelSession();
            Toast.makeText(MainActivity.this, "Cancel Session", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            // Handle exception
        }
    }

    private void showConfig() {
        try {
            runOnUiThread(() -> {
                setContentView(R.layout.config);

                EditText txtTestServerURL = findViewById(R.id.txtServerUrl);
                EditText txtCountryCode = findViewById(R.id.txtOTP);
                EditText txtCurrency = findViewById(R.id.txtCurrency);
                EditText txtReaderLimit = findViewById(R.id.txtReaderLimit);
                Spinner spinPin = findViewById(R.id.spinPinCapability);
                Spinner spinSig = findViewById(R.id.spinSigCapability);
                Spinner spinTrans = findViewById(R.id.spinTransactionType);
                Spinner spinMCP = findViewById(R.id.spinMastercardProfile);
                TextView txtSdkVersion = findViewById(R.id.txtSdkVersion);
                Button btnDoTransaction = findViewById(R.id.btnDoTransaction);

                ArrayAdapter<IntKeyStringValue> pinReqAdapter = new ArrayAdapter<>(
                        this,
                        R.layout.support_simple_spinner_dropdown_item,
                        pinCapabilities
                );
                spinPin.setAdapter(pinReqAdapter);

                ArrayAdapter<IntKeyStringValue> sigFlagAdapter = new ArrayAdapter<>(
                        this,
                        R.layout.support_simple_spinner_dropdown_item,
                        signatureFlags
                );
                spinSig.setAdapter(sigFlagAdapter);

                ArrayAdapter<IntKeyStringValue> transTypeAdapter = new ArrayAdapter<>(
                        this,
                        R.layout.support_simple_spinner_dropdown_item,
                        transactionTypes
                );
                spinTrans.setAdapter(transTypeAdapter);

                ArrayAdapter<String> mcProfileAdapter = new ArrayAdapter<>(
                        this,
                        R.layout.support_simple_spinner_dropdown_item,
                        emvAdapter.getMastercardProfiles()
                );
                spinMCP.setAdapter(mcProfileAdapter);

                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                int defaultMCProfile = 0;
                if (!sharedPref.contains("mastercardProfile")) {
                    Log.d("DEBUG", "NO MASTERCARD PROFILE FOUND IN PREFS");
                    int pos = mcProfileAdapter.getPosition("PPS_MChip1");
                    Log.d("DEBUG", "PPS_MCHIP1 POS " + pos);
                    defaultMCProfile = (pos != -1) ? pos : 0;
                }

                String testServerURL = sharedPref.getString("testServerURL", "http://192.168.0.101:8081/emvPayment");
                String countryCode = sharedPref.getString("countryCode", "0368");
                String currencyCode = sharedPref.getString("currencyCode", "0952");
                float readerLimit = sharedPref.getFloat("readerLimit", 100f);
                int pinRequirement = sharedPref.getInt("pinRequirement", 1);
                int signatureFlag = sharedPref.getInt("signatureFlag", 0);
                int transactionType = sharedPref.getInt("transactionType", 0);
                int mastercardProfile = sharedPref.getInt("mastercardProfile", defaultMCProfile);

                txtTestServerURL.setText(testServerURL);
                txtCountryCode.setText(countryCode);
                txtCurrency.setText(currencyCode);
                txtReaderLimit.setText(String.valueOf(readerLimit));
                spinPin.setSelection(pinRequirement);
                spinSig.setSelection(signatureFlag);
                spinTrans.setSelection(transactionType);

                Map<String, Object> versions = emvAdapter.getVersionInfo();
                StringBuilder versionString = new StringBuilder("SDK Version Information");
                for (Map.Entry<String, Object> entry : versions.entrySet()) {
                    if (versionString.length() > 0) {
                        versionString.append("\n");
                    }
                    versionString.append(entry.getKey()).append(": ").append(entry.getValue().toString());
                }
                txtSdkVersion.setText(versionString.toString());


            });
        } catch (Exception ex) {
            // Handle exception
        }
    }

    public void doTransaction(View view) {
        try {
            System.out.println("CHECKPOINT DO TRANSACTION");

            EditText txtTestServerURL = findViewById(R.id.txtServerUrl);
            EditText txtCountryCode = findViewById(R.id.txtOTP);
            EditText txtCurrency = findViewById(R.id.txtCurrency);
            EditText txtReaderLimit = findViewById(R.id.txtReaderLimit);

            Spinner spinPin = findViewById(R.id.spinPinCapability);
            Spinner spinSig = findViewById(R.id.spinSigCapability);
            Spinner spinTrans = findViewById(R.id.spinTransactionType);
            Spinner spinMCP = findViewById(R.id.spinMastercardProfile);

            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getCOUNTRY_CODE(), txtCountryCode.getText().toString());
            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getCURRENCY_CODE(), txtCurrency.getText().toString());
            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getPIN_REQUIREMENT(), spinPin.getSelectedItemPosition());
            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getREADER_LIMIT(), Double.parseDouble(txtReaderLimit.getText().toString()));
            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getSIGNATURE_FLAG(), spinSig.getSelectedItemPosition());

            String txType = (spinTrans.getSelectedItemPosition() == 0) ? "00" : "20";
            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getTX_TYPE(), txType);

            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getCURRENCY_EXPONENT(), 2);
            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getOVERRIDE_MASTERCARD_PROFILE_CONFIG(), true);


            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getMOCK_AUTH_CODE(), "01");
            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getMOCK_STATUS_CODE(), "00");

            emvAdapter.setConfig(EMVCfgVars.INSTANCE.getENFORCE_PIN_CVM(), true);

            // For production use
            emvAdapter.setSelectedMastercardProfile("MPOS");

            showPaymentCaptureView();

        } catch (Exception ex) {
            System.out.println("ERROR " + ex);
        }
    }

    public void saveConfiguration(View view) {
        try {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

            EditText txtTestServerURL = findViewById(R.id.txtServerUrl);
            EditText txtCountryCode = findViewById(R.id.txtOTP);
            EditText txtCurrency = findViewById(R.id.txtCurrency);
            EditText txtReaderLimit = findViewById(R.id.txtReaderLimit);

            Spinner spinPin = findViewById(R.id.spinPinCapability);
            Spinner spinSig = findViewById(R.id.spinSigCapability);
            Spinner spinTrans = findViewById(R.id.spinTransactionType);
            Spinner spinMCP = findViewById(R.id.spinMastercardProfile);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("testServerURL", txtTestServerURL.getText().toString());
            editor.putString("countryCode", txtCountryCode.getText().toString());
            editor.putString("currencyCode", txtCurrency.getText().toString());
            editor.putFloat("readerLimit", Float.parseFloat(txtReaderLimit.getText().toString()));
            editor.putInt("pinRequirement", spinPin.getSelectedItemPosition());
            editor.putInt("signatureFlag", spinSig.getSelectedItemPosition());
            editor.putInt("transactionType", spinTrans.getSelectedItemPosition());
            editor.putInt("mastercardProfile", spinMCP.getSelectedItemPosition());
            editor.commit();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void showPaymentCaptureView() {
        try {
            runOnUiThread(() -> {
                setContentView(R.layout.payment_capture);
                TextView txtInput = findViewById(R.id.txtInput);
                txtInput.setInputType(InputType.TYPE_CLASS_TEXT);
                TextView txtDisplay = findViewById(R.id.txtDisplay);
                txtDisplay.setText("Enter Amount");
                amount = "0";
                txtInput.setText(formatAmount());
            });
        } catch (Exception ex) {
            // Handle exception
        }
    }

    private String formatAmount() {
        DecimalFormat decim = new DecimalFormat("0.00");
        return decim.format(Double.parseDouble(amount) / 100);
    }

    private void initSession() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCurrentActivity) {
                        try {
                            emvAdapter.initSession(
                                    merchantToken,
                                    new DecimalFormat("00000").format(txRef),
                                    Integer.parseInt(amount, 10)
                            );
                        } catch (Exception ex) {
                            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "No Current Activity", Toast.LENGTH_SHORT).show();
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                initSession();
                            }
                        }, 5000);
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addAmount(View view) {
        try {
            TextView txtInput = findViewById(R.id.txtInput);
            view.startAnimation(buttonClick);
            Button b = (Button) view;
            String buttonText = b.getText().toString();
            amount += buttonText;
            txtInput.setText(formatAmount());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void clearAmount(View view) {
        try {
            amount = "0";
            TextView txtInput = findViewById(R.id.txtInput);
            view.startAnimation(buttonClick);
            txtInput.setText(formatAmount());
            sb.setLength(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void invokeEMV(View view) {
        initSession();
    }


    @Override
    public void onCardProcessingNotify(@NonNull String s) {

    }

    @Override
    public void onCheckDeviceRegistrationComplete(boolean b, @NonNull String s, @NonNull String s1, @NonNull String s2, @NonNull String s3, @NonNull String s4) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                    if (b) {
                        emvAdapter.initAdapter();
                    } else {
                        emvAdapter.registerDevice(null, "5224bfb0-b6e9-11ef-8139-e93b81e4f561", "0000000BOPS2I01");
                    }
            }
        });
    }

    @Override
    public void onDeviceRegistrationComplete(boolean b, @NonNull String s) {
        if (b) {
            emvAdapter.initAdapter();
        } else {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    if (!isFinishing() && !isDestroyed()) {  // Check if activity is valid
                        dialogCaptureOtp = new Dialog(MainActivity.this);  // Use activity context
                        bindCaptureOtp = CaptureOtpBinding.inflate(LayoutInflater.from(MainActivity.this));
                        bindCaptureOtp.txtOTP.setText("");
                        bindCaptureOtp.txtOTPStatus.setText("Failed to register with supplied verification code. Reason: " + s);

                        bindCaptureOtp.btnOTPContinue.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (bindCaptureOtp.txtOTP.getText().toString() != "") {
                                    emvAdapter.registerDevice(bindCaptureOtp.txtOTP.getText().toString(), null, null);
                                    bindCaptureOtp.txtOTP.setError(null);
                                    dialogCaptureOtp.dismiss();
                                } else {
                                    bindCaptureOtp.txtOTP.setError("Enter OTP");
                                }
                            }
                        });

                        dialogCaptureOtp.setContentView(bindCaptureOtp.getRoot());
                        dialogCaptureOtp.getWindow().setGravity(Gravity.CENTER);
                        dialogCaptureOtp.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                        dialogCaptureOtp.setCancelable(true);
                        dialogCaptureOtp.show();
                    }else{
                        Toast.makeText(MainActivity.this, "Activity not valid", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public void onDeviceUnRegistrationComplete(boolean b, @NonNull String s) {

    }

    @Override
    public void onSessionTimeout() {
        Toast.makeText(MainActivity.this, "Timeout Session", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    // Continue with other methods similarly
}
