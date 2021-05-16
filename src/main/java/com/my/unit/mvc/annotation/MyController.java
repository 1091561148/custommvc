package com.my.unit.mvc.annotation;
import java.lang.annotation.*;

/**
 * @description：MyController
 * @author：GJF
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {
    String value() default "";
}