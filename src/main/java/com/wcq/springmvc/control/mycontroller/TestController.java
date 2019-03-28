package com.wcq.springmvc.control.mycontroller;

import com.wcq.springmvc.annotation.MyAutowired;
import com.wcq.springmvc.annotation.MyController;
import com.wcq.springmvc.annotation.MyRequestMapping;
import com.wcq.springmvc.annotation.MyRequestParam;
import com.wcq.springmvc.control.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
@MyRequestMapping("/demo")
public class TestController {
    @MyAutowired
    private TestService myService;
    @MyRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @MyRequestParam("name") String name){
//        String result = "My name is " + name;
        String result = myService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @MyRequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @MyRequestParam("a") Integer a, @MyRequestParam("b") Integer b){
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @MyRequestMapping("/remove")
    public void remove(HttpServletRequest req,HttpServletResponse resp,
                       @MyRequestParam("id") Integer id){
    }
}
