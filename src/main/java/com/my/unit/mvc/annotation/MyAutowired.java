package com.my.unit.mvc.annotation;

import java.lang.annotation.*;

/**
 * @description：MyAutowired
 * @author：GJF
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {
    String value() default "";
}
