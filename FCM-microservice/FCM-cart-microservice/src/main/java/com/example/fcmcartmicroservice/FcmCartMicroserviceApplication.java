package com.example.fcmcartmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FcmCartMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FcmCartMicroserviceApplication.class, args);
    }

}
