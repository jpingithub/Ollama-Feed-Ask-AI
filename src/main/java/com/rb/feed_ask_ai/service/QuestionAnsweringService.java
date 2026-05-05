package com.rb.feed_ask_ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionAnsweringService {

    private final ChatClient chatClient;

    public String generateAnswer(String question, String context) {

        String promptText = String.format(
                "Based on the following context, answer the question.\n\nContext:\n%s\n\nQuestion: %s\n\nAnswer:",
                context, question
        );


        return chatClient.prompt()
                .user(promptText)
                .call()
                .content();
    }
}
