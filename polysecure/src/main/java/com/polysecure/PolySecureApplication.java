package com.polysecure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class PolySecureApplication {
    public static void main(String[] args) {
        SpringApplication.run(PolySecureApplication.class, args);
    }
}
