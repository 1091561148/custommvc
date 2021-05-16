package com.my.unit.mvc.annotation;

import java.lang.annotation.*;

/**
 * @description：MyRequestParam
 * @author：GJF
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {
    String value();
}