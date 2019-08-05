package com.xrosstools.xunit.spring;

import com.xrosstools.xunit.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = XunitAnnotationTestConfiguration.class)
@TestPropertySource(properties = {"xunit.config.path = xunit/basic.xunit"})
public class XunitConfigBeanPostProcessorTest {

    private static final String packageId = "test";
    private static final String name = "BasicUnits";
    private static final String factoryBeanName = packageId + "." + name;
    private static final String[] childNames = {"processor", "converter", "reference", "abibranch",
            "abranch", "achain", "adecorator", "anadapter", "do-whileloop", "whileloop"};

    private XunitFactory factoryBean;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Before
    public void testUnitBeanDefinition() {
        factoryBean = beanFactory.getBean(factoryBeanName, XunitSpringFactory.class);
        Assert.assertNotNull(factoryBean);
        Assert.assertEquals("packageId error", packageId, factoryBean.getPackageId());
        for(String childName : childNames) {
            String childBeanName = factoryBeanName + ":" + childName;
            Unit childBean = beanFactory.getBean(childBeanName, Unit.class);
            Assert.assertNotNull(childBean);
        }
    }

    @Test
    public void referenceNodeBean() {
        Processor parentBean = beanFactory.getBean(factoryBeanName + ":processor", Processor.class);
        MapContext ctx = new MapContext();
        ctx.put("key", "123");
        parentBean.process(ctx);

        Processor referBean = beanFactory.getBean(factoryBeanName + ":reference", Processor.class);
        referBean.process(ctx);
    }

    @Test
    public void customChainImpl() {
        try {
            Processor mychain = factoryBean.getProcessor("MyChain");
            mychain.process(new Context() {});
        }catch (Exception e)  {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void customBranchImpl() {
        try {
            Processor myBranch = factoryBean.getProcessor("MyBranch");
            myBranch.process(new Context() {});
        }catch (Exception e)  {
            Assert.fail(e.getMessage());
        }
    }

}
