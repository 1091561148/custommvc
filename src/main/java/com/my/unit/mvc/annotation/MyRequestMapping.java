package com.my.unit.mvc.annotation;

import java.lang.annotation.*;

/**
 * @description：MyRequestMapping
 * @author：GJF
 */

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {
    String value();
}