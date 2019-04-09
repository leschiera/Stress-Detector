package polar.com.androidblesdk;

import android.Manifest;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;

import org.reactivestreams.Publisher;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Timer;
import java.util.TimerTask;


import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.model.PolarDeviceInfo;

import polar.com.sdk.api.model.PolarHrData;
import polar.com.sdk.api.model.PolarOhrPPGData;
import polar.com.sdk.api.model.PolarOhrPPIData;
import polar.com.sdk.api.model.PolarSensorSetting;

/**TensorflowLite **/
import org.tensorflow.lite.Interpreter;

public class Worker extends AppCompatActivity {
    private final static String TAG = Worker.class.getSimpleName();
    PolarBleApi api;

    Disposable ecgDisposable;
    Disposable accDisposable;
    Disposable ppgDisposable;
    Disposable ppiDisposable;
    String DEVICE_ID = "32B91E2C"; // TODO replace with your device id

    /**  GUI **/
    TextView txtUser;
    Button btnConnect;

    Activity activity;
    Timer timer = new Timer();

    AWSIoT awsIoT;
    boolean connected = false;

    /**TenorflowLite **/
    Interpreter tflite;

    String user = "Test";
    boolean stress = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.worker);

        /** GUI **/
        user = this.getIntent().getStringExtra("user");
        txtUser = this.findViewById(R.id.txtWelcomeUser);
        txtUser.setText(user);
        btnConnect = this.findViewById(R.id.btnConnection);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button btn = (Button) view;
                if(!connected) {
                    connect();
                    btn.setText("Disconnect");
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText( activity,"No Sensor Found",Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    };
                    timer = new Timer();
                    timer.schedule(task,4000);
                }else{
                    disconnect();
                    btn.setText("Connect");
                }
                connected = !connected;
            }
        });

        /** TensorFlow Lite **/
        try{
            tflite = new Interpreter(loadModelFile());
        }catch (Exception ex){
            ex.printStackTrace();
        }



        activity = this;
        awsIoT = new AWSIoT(this);

        // Notice PolarBleApi.ALL_FEATURES are enabled
        api = PolarBleApiDefaultImpl.defaultImplementation(this, PolarBleApi.ALL_FEATURES);


        api.setApiLogger(new PolarBleApi.PolarBleApiLogger() {
            @Override
            public void message(String s) {
                Log.d(TAG, s);
            }
        });




        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo());

        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean powered) {
                Log.d(TAG, "BLE power: " + powered);
                if(powered){
                    btnConnect.setClickable(true);
                }else{
                    btnConnect.setClickable(false);
                }
            }

            @Override
            public void polarDeviceConnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "CONNECTED: " + polarDeviceInfo.deviceId);
                DEVICE_ID = polarDeviceInfo.deviceId;
            }

            @Override
            public void polarDeviceConnecting(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId);
                DEVICE_ID = polarDeviceInfo.deviceId;
            }

            @Override
            public void polarDeviceDisconnected(PolarDeviceInfo polarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: " + polarDeviceInfo.deviceId);
                ecgDisposable = null;
                accDisposable = null;
                ppgDisposable = null;
                ppiDisposable = null;
            }

            @Override
            public void ecgFeatureReady(String identifier) {
                Log.d(TAG, "ECG READY: " + identifier);
                // ecg streaming can be started now if needed
            }

            @Override
            public void accelerometerFeatureReady(String identifier) {
                Log.d(TAG, "ACC READY: " + identifier);
                // acc streaming can be started now if needed
            }

            @Override
            public void ppgFeatureReady(String identifier) {
                Log.d(TAG, "PPG READY: " + identifier);
                // ohr ppg can be started
            }

            @Override
            public void ppiFeatureReady(String identifier) {
                Log.d(TAG, "PPI READY: " + identifier);
                // ohr ppi can be started
            }

            @Override
            public void biozFeatureReady(String identifier) {
                Log.d(TAG, "BIOZ READY: " + identifier);
                // ohr ppi can be started
            }

            @Override
            public void hrFeatureReady(String identifier) {
                Log.d(TAG, "HR READY: " + identifier);
                // hr notifications are about to start
            }

            @Override
            public void fwInformationReceived(String identifier, String fwVersion) {
                Log.d(TAG, "FW: " + fwVersion);

            }

            //return the battery level
            @Override
            public void batteryLevelReceived(String identifier, int level) {
                Log.d(TAG, "BATTERY LEVEL: " + level);

            }

            //return the value after connected
            @Override
            public void hrNotificationReceived(String identifier, PolarHrData data) {
                timer.cancel();
                Log.d(TAG, user);
                Log.d(TAG, "HR value: " + data.hr + " rrAvailable: " + data.rrAvailable + " rrsMs: " + data.rrsMs + " rr: " + data.rrs + " contact: " + data.contactStatus + "," + data.contactStatusSupported);


                float prediction = doInference(0,data.hr);



                if(prediction >0.5 || stress) {
                    stress = false;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected != awsIoT.statusConnection) {
                                try {
                                    this.wait(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }awsIoT.publish("Stress Detected", user );
                        }
                    }).start();
                }

            }

            @Override
            public void polarFtpFeatureReady(String s) {
                Log.d(TAG, "FTP ready");
            }


        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && savedInstanceState == null) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }


    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == 1) {
            Log.d(TAG,"bt ready");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        api.backgroundEntered();
    }

    @Override
    public void onResume() {
        super.onResume();
        api.foregroundEntered();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        api.shutDown();
        disconnect();
        awsIoT.disconnect();
    }


    public void connect() {
        api.connectToPolarDevice(DEVICE_ID);

    }

    public void disconnect(){
        api.disconnectFromPolarDevice(DEVICE_ID);

    }




    public void ppg(){
        if(ppgDisposable == null) {
            ppgDisposable = api.requestPpgSettings(DEVICE_ID).toFlowable().flatMap(new Function<PolarSensorSetting, Publisher<PolarOhrPPGData>>() {
                @Override
                public Publisher<PolarOhrPPGData> apply(PolarSensorSetting polarPPGSettings) throws Exception {
                    return api.startOhrPPGStreaming(DEVICE_ID,polarPPGSettings.maxSettings());
                }
            }).subscribe(
                    new Consumer<PolarOhrPPGData>() {
                        @Override
                        public void accept(PolarOhrPPGData polarOhrPPGData) throws Exception {
                            for( PolarOhrPPGData.PolarOhrPPGSample data : polarOhrPPGData.samples ){
                                Log.d(TAG,"    ppg0: " + data.ppg0 + " ppg1: " + data.ppg1 + " ppg2: " + data.ppg2 + "ambient: " + data.ambient);
                            }
                        }
                    },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.e(TAG,""+throwable.getLocalizedMessage());
                        }
                    },
                    new Action() {
                        @Override
                        public void run() throws Exception {
                            Log.d(TAG,"complete");
                        }
                    }
            );
        } else {
            ppgDisposable.dispose();
            ppgDisposable = null;
        }
    }


    public void ppi(){
        if(ppiDisposable == null) {
            ppiDisposable = api.startOhrPPIStreaming(DEVICE_ID).observeOn(AndroidSchedulers.mainThread()).subscribe(
                    new Consumer<PolarOhrPPIData>() {
                        @Override
                        public void accept(PolarOhrPPIData ppiData) throws Exception {
                            for(PolarOhrPPIData.PolarOhrPPISample sample : ppiData.samples) {
                                Log.d(TAG, "ppi: " + sample.ppi
                                        + " blocker: " + sample.blockerBit + " errorEstimate: " + sample.errorEstimate);
                            }
                        }
                    },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.e(TAG,""+throwable.getLocalizedMessage());
                        }
                    },
                    new Action() {
                        @Override
                        public void run() throws Exception {
                            Log.d(TAG,"complete");
                        }
                    }
            );
        } else {
            ppiDisposable.dispose();
            ppiDisposable = null;
        }
    }

    /** TensorFlowMethods **/

    /**Memory-map the model file in Assets. */
    private MappedByteBuffer loadModelFile() throws IOException{
        // Open the model using an input stream, and memory map it to load
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model.tflite");
        FileInputStream inputStream =  new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset,declaredLength);
    }

    private float doInference(float input, float hr){
        float[][] inputVal = new float[1][1];
        inputVal[0][0] = hr;

        int i = tflite.getInputTensorCount();
        int j = tflite.getOutputTensorCount();
        float[] outputVal = new float[1];

        tflite.run(inputVal,outputVal);

        float inferredValue = outputVal[0];
        return inferredValue;
    }


    /*
    public void setTime(){
        api.setLocalTime(DEVICE_ID,new Date()).subscribe(
                new Action() {
                    @Override
                    public void run() throws Exception {
                        Log.d(TAG,"time set to device");
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.d(TAG,"set time failed: " + throwable.getLocalizedMessage());
                    }
                });
    }
    */

}
