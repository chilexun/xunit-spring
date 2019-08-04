package com.xrosstools.xunit.spring.unit;

import com.xrosstools.xunit.Context;
import com.xrosstools.xunit.Validator;
import com.xrosstools.xunit.spring.service.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;

public class OuterNetValidator implements Validator {
    @Autowired
    private NetworkService networkService;

    @Override
    public boolean validate(Context context) {
        return networkService.isOuterNet();
    }
}
