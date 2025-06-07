package io.github.defective4.sdr.sdrdscv.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface BuilderParam {
    String argName();

    String defaultField() default "";

    String description() default "";
}
