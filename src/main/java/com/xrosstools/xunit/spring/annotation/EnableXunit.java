package com.xrosstools.xunit.spring.annotation;

import com.xrosstools.xunit.spring.config.XunitComponentRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(XunitComponentRegistrar.class)
public @interface EnableXunit {

    @AliasFor("paths")
    String[] value() default {};

    @AliasFor("value")
    String[] paths() default {};

}
