package com.xrosstools.xunit.spring;

import com.xrosstools.xunit.spring.annotation.EnableXunit;
import org.springframework.context.annotation.ComponentScan;

@EnableXunit("${xunit.config.path}")
@ComponentScan
public class XunitAnnotationTestConfiguration {

}
