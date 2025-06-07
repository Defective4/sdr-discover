package io.github.defective4.sdr.sdrdscv.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(PARAMETER)
public @interface ConstructorParam {
    String argName();

    String defaultValue();

    String description();
}
