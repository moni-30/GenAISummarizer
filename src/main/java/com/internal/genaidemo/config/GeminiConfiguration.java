package com.internal.genaidemo.config;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GeminiConfiguration {

    @Bean
    public VertexAI vertexAI(){
        return new VertexAI("sep-rjha-08dec-cts","us-central1");
    }

    public GenerativeModel generativeModel(VertexAI vertexAI){
        return new GenerativeModel("gemini-pro-vision",vertexAI);
    }
}
