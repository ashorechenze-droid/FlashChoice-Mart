package com.example.fcmgoodmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FcmGoodMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FcmGoodMicroserviceApplication.class, args);
    }

}
