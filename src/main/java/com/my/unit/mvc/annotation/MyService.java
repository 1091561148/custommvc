package com.my.unit.mvc.annotation;

import java.lang.annotation.*;

/**
 * @description：MyService
 * @author：GJF
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {
    String value() default "";
}
