package edu.stanford.slac.ad.eed.base_mongodb_lib;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("edu.stanford.slac.ad.eed")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
