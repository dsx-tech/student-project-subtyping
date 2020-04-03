package ann;

import ann.subtype.Top;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE,
        ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Subtype {
    Class<? extends Top> value();
}
