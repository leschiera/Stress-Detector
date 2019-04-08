package polar.com.androidblesdk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;

public class Manager extends AppCompatActivity {

    AWSIoT awsIoT;
    String[] subscriber = {"Leschiera", "Test"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        final AWSIoT awsIoT = new AWSIoT(this );

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected != awsIoT.statusConnection){
                    awsIoT.subscribe("Leschiera");
                    awsIoT.subscribe("Test");

                }
            }
        }).start();

    }


}
