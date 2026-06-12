package com.api.discovery.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.parser.OpenAPIV3Parser;

@Configuration
@Slf4j
public class AppConfig {

    @Bean
    public OpenAPIV3Parser openAPIV3Parser() {
        return new OpenAPIV3Parser();
    }
 
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
