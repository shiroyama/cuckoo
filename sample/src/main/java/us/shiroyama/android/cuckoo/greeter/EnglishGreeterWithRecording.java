package us.shiroyama.android.cuckoo.greeter;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import us.shiroyama.android.cuckoo.annotations.By;
import us.shiroyama.android.cuckoo.annotations.Delegate;

/**
 * @author Fumihiko Shiroyama
 */

@Delegate
public abstract class EnglishGreeterWithRecording implements Greeter {
    private final List<String> toWhomList = new ArrayList<>();

    @By
    final EnglishGreeter englishGreeter = new EnglishGreeter();

    @Override
    public String sayHello(String toWhom) {
        toWhomList.add(toWhom);
        return englishGreeter.sayHello(toWhom);
    }

    public void logToWhomList() {
        for (String toWhom : toWhomList) {
            Log.d(EnglishGreeterWithRecording.class.getSimpleName(), toWhom);
        }
    }
}
