package com.example.fcmcategorymicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class FcmCategoryMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FcmCategoryMicroserviceApplication.class, args);
    }

}
