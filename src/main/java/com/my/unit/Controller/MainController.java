package com.my.unit.Controller;

import com.my.unit.mvc.annotation.MyController;
import com.my.unit.mvc.annotation.MyRequestMapping;
import com.my.unit.mvc.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
@MyRequestMapping("api")
public class MainController {

    @MyRequestMapping("test")
    public String myTest(HttpServletRequest request, HttpServletResponse response,
                       @MyRequestParam("param") String param){
        try {
            response.getWriter().write( "Test2Controller:the param you send is :"+param);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "5152542 error !";
    }
}
