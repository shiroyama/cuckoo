package us.shiroyama.android.cuckoo.greeter;

/**
 * @author Fumihiko Shiroyama
 */

public class EnglishGreeter implements Greeter {
    @Override
    public String sayHello() {
        return sayHello("ANONYMOUS");
    }

    @Override
    public String sayHello(String toWhom) {
        return String.format("Hello %s !", toWhom);
    }
}
