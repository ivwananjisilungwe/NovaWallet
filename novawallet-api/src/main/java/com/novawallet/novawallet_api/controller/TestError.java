package com.novawallet.novawallet_api.controller;


import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestError {


    @GetMapping("/test-error")
    public String test(){
        throw  new ResourceNotFoundException(
                "Testing error handler"
        );
    }

}
