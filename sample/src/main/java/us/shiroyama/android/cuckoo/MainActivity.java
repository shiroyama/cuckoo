package us.shiroyama.android.cuckoo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import us.shiroyama.android.cuckoo.greeter.EnglishGreeterWithRecording;
import us.shiroyama.android.cuckoo.greeter.EnglishGreeterWithRecordingImpl;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EnglishGreeterWithRecording greeter = EnglishGreeterWithRecordingImpl.newInstance();

        ((TextView) findViewById(R.id.anonymous)).setText(greeter.sayHello());
        ((TextView) findViewById(R.id.somebody)).setText(greeter.sayHello("SHIROYAMA"));

        greeter.logToWhomList();
    }
}
