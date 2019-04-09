package polar.com.androidblesdk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;

public class Manager extends AppCompatActivity {


    AWSIoT awsIoT;
    String[] subscriber = {"Leschiera", "Test"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.manager);


        String user = this.getIntent().getStringExtra("user");
        TextView txtUser = this.findViewById(R.id.txtWelcomeUser);
        txtUser.setText(user);

        awsIoT = new AWSIoT(this );

        Button btnConn = this.findViewById(R.id.btnConnection);
        btnConn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.Connected != awsIoT.statusConnection){
                            for(int  i = 0; i< subscriber.length; i++) {
                                awsIoT.subscribe(subscriber[i]);
                            }

                        }
                    }
                }).start();
                view.setClickable(false);
            }
        });

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        awsIoT.disconnect();
    }

}
