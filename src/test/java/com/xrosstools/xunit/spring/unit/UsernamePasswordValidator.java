package com.xrosstools.xunit.spring.unit;

import com.xrosstools.xunit.Context;
import com.xrosstools.xunit.Validator;
import com.xrosstools.xunit.spring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

public class UsernamePasswordValidator implements Validator {
    @Autowired
    private UserService userService;

    @Override
    public boolean validate(Context context) {
        LoginContext ctx = (LoginContext)context;
        boolean result = userService.validatePassword(ctx.getUsername(), ctx.getPassword());
        ctx.setLoginSuccess(result);
        return result;
    }
}
