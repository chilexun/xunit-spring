package com.xrosstools.xunit.spring;

import com.xrosstools.xunit.Context;
import com.xrosstools.xunit.Processor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = XunitAnnotationTestConfiguration.class)
@TestPropertySource(properties = {"xunit.config.path = xunit/reference.xunit"})
public class CrossFileReferenceTest {

    @Resource(name = "test.reference")
    private XunitSpringFactory factory;

    @Resource(name = "test.BasicUnits")
    private XunitSpringFactory basicFactory;

    @Test
    public void referenceChain() {
        Processor chain = factory.getProcessor("reference chain");
        chain.process(new Context() {});
    }
}
