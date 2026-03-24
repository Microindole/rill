package com.indolyn.rill.app.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.indolyn.rill")
public class RillApplication {

    public static void main(String[] args) {
        SpringApplication.run(RillApplication.class, args);
    }
}
