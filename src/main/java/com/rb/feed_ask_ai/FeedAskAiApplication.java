package com.rb.feed_ask_ai;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class FeedAskAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeedAskAiApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Feed Ask AI API")
						.version("1.0.0")
						.description("API for uploading PDF documents, embedding them using Ollama, and asking questions based on the embedded content")
				);
	}

}
