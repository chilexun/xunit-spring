package com.xrosstools.xunit.spring.service;

import org.springframework.stereotype.Service;

@Service
public class SmsService {

    public void sendSms(String user){
        System.out.println("Send SMS code to:" + user);
    }
}
