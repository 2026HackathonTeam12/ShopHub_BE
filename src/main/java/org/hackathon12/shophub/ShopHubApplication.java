package org.hackathon12.shophub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShopHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopHubApplication.class, args);
    }

}
