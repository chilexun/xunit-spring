package com.xrosstools.xunit.spring.unit;

import com.xrosstools.xunit.Context;
import com.xrosstools.xunit.Processor;
import com.xrosstools.xunit.spring.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;

public class SmsProcessor implements Processor {
    @Autowired
    private SmsService smsService;

    @Override
    public void process(Context context) {
        smsService.sendSms(((LoginContext)context).getUsername());
    }
}
