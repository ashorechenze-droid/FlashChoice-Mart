package com.example.fcmusermicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class FcmUserMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FcmUserMicroserviceApplication.class, args);
    }

}
