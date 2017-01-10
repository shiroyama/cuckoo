package us.shiroyama.android.cuckoo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for classes that delegate processing to fields annotated by {@link By}
 *
 * @author Fumihiko Shiroyama
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Delegate {
}
