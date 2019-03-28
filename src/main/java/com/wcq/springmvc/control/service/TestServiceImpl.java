package com.wcq.springmvc.control.service;

import com.wcq.springmvc.annotation.MyService;

@MyService
public class TestServiceImpl implements TestService {
    @Override
    public String get(String s) {
        return "Hello," + s;
    }
}
