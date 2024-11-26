package me.srrapero720.dimthread.mixin.tools;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Volatilize {
    boolean declared() default false;
}
