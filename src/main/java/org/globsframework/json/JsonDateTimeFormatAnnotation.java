package org.globsframework.json;

import org.globsframework.metamodel.GlobType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({ElementType.FIELD})
public @interface JsonDateTimeFormatAnnotation {

    String pattern();

    boolean asLocal() default false;

    String nullValue() default "";

    public GlobType TYPE = JsonDateTimeFormatType.TYPE;
}
