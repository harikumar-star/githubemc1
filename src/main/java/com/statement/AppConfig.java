package com.statement;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    //first setup
    // i done the second changes
    // thirds
    @Bean
    public BlockingQueue<byte[]> queue() {
        return new LinkedBlockingQueue<>();
    }
}
