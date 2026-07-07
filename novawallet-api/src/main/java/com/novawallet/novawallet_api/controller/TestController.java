package com.novawallet.novawallet_api.controller;


import com.novawallet.novawallet_api.dto.ApiResponse;
import com.novawallet.novawallet_api.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {


    @GetMapping("/error")
    public String testError(){
        throw  new ResourceNotFoundException(
                "Testing error handler"
        );
    }

    @GetMapping("/success")
    public ApiResponse<String> testSuccess(){

        return ApiResponse.success(
                "NovaWallet API is running",
                "Request successful"
        );

    }


}
