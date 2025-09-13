package com.cut.cardona.api.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("hello")
public class HelloController {


    @GetMapping()
    public String helloWorld(@RequestBody String parametro) {
        return "hello worl from pobretones";
    }


}
