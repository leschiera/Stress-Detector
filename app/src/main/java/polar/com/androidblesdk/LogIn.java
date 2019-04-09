package polar.com.androidblesdk;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;


public class LogIn extends AppCompatActivity {
    static final List<String> users=  new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        users.add("Leschiera");
        users.add("Osmanaj");
        users.add("Rizza");
        users.add("Test");

        final EditText user = this.findViewById(R.id.txtUser);
        final EditText password = this.findViewById(R.id.txtPassword);
        final Button login = this.findViewById(R.id.btnLogin);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(users.contains(user.getText().toString())  ) {
                    Intent intent = new Intent(LogIn.this, Worker.class);
                    Bundle arguments = new Bundle();
                    arguments.putString("user", user.getText().toString());
                    intent.putExtras(arguments);
                    startActivity(intent);
                }else{
                    Intent intent = new Intent(LogIn.this, Manager.class);
                    Bundle arguments = new Bundle();
                    arguments.putString("user", user.getText().toString());
                    intent.putExtras(arguments);
                    startActivity(intent);
                }
            }
        });

    }
}
