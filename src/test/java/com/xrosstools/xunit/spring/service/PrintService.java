package com.xrosstools.xunit.spring.service;

import org.springframework.stereotype.Service;

@Service
public class PrintService {

    public void print(String msg) {
        System.out.println(msg);
    }
}
