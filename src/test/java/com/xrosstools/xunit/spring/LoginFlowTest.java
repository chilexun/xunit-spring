package com.xrosstools.xunit.spring;

import com.xrosstools.xunit.Processor;
import com.xrosstools.xunit.Validator;
import com.xrosstools.xunit.XunitFactory;
import com.xrosstools.xunit.spring.unit.LoginContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = XunitAnnotationTestConfiguration.class)
@TestPropertySource(properties = {"xunit.config.path = xunit/login_sample.xunit"})
public class LoginFlowTest {

    @Resource(name = "test.Sample")
    private XunitFactory xunitFactory;

    @Autowired
    @Qualifier("test.Sample:LoginFlow:LoginBranch:UsernamePasswordValidator")
    private Validator passwordValidator;

    @Test
    public void login() {
        try {
            Processor processor = xunitFactory.getProcessor("LoginFlow");
            LoginContext context = new LoginContext("user", "password");
            processor.process(context);
            Assert.assertTrue(context.isLoginSuccess());

            context.setPassword("wrongPwd");
            processor.process(context);
            Assert.assertFalse(context.isLoginSuccess());
        }catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void passwordValidate() {
        LoginContext context = new LoginContext("user", "password");
        boolean result = passwordValidator.validate(context);
        Assert.assertTrue(result);
    }
}
