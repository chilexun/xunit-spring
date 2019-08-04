package com.xrosstools.xunit.spring.service;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    public boolean validatePassword(String user, String password) {
        return "user".equals(user) && "password".equals(password);
    }
}
