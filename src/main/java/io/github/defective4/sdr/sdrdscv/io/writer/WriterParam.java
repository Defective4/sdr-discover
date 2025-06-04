package io.github.defective4.sdr.sdrdscv.io.writer;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(PARAMETER)
public @interface WriterParam {
    String argName();

    String defaultValue();

    String description();
}
