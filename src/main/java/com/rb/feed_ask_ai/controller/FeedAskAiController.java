package com.rb.feed_ask_ai.controller;

import com.rb.feed_ask_ai.api.ApiApi;
import com.rb.feed_ask_ai.entity.RagEntity;
import com.rb.feed_ask_ai.model.AskResponse;
import com.rb.feed_ask_ai.model.ErrorResponse;
import com.rb.feed_ask_ai.model.FeedUploadResponse;
import com.rb.feed_ask_ai.repository.RagRepository;
import com.rb.feed_ask_ai.service.EmbeddingService;
import com.rb.feed_ask_ai.service.PdfProcessingService;
import com.rb.feed_ask_ai.service.QuestionAnsweringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FeedAskAiController implements ApiApi {

    private final PdfProcessingService pdfProcessingService;
    private final QuestionAnsweringService questionAnsweringService;
    private final EmbeddingService embeddingService;
    private final RagRepository ragRepository;
    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Value("${top.match.max-results}")
    private Integer TOP_K;

    @Override
    public ResponseEntity<FeedUploadResponse> uploadFeed(MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
                ErrorResponse error = new ErrorResponse();
                error.setTimestamp(OffsetDateTime.now());
                error.setStatus(400);
                error.setError("Invalid PDF");
                error.setMessage("File must be a valid PDF");
                error.setPath("/api/v1/feed-ask/feed");
                return ResponseEntity.badRequest().body(null); // Should return error response
            }

            if (file.getSize() > 100 * 1024 * 1024) { // 100MB
                ErrorResponse error = new ErrorResponse();
                error.setTimestamp(OffsetDateTime.now());
                error.setStatus(413);
                error.setError("File too large");
                error.setMessage("File size exceeds maximum allowed (100MB)");
                error.setPath("/api/v1/feed-ask/feed");
                return ResponseEntity.status(413).body(null);
            }

            // Process PDF
            List<PdfProcessingService.RagChunk> result = pdfProcessingService.processPdf(file);

            for (PdfProcessingService.RagChunk chunk : result) {
                float[] embeddedVectors = embeddingService.generateEmbedding(chunk.content());
                RagEntity ragEntity = new RagEntity();
                ragEntity.setContent(chunk.content());
                ragEntity.setPageNumber(chunk.pageNumber());
                ragEntity.setEmbedding(OBJECT_MAPPER.writeValueAsString(embeddedVectors));
                ragRepository.save(ragEntity);
            }


            return null;

        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse();
            error.setTimestamp(java.time.OffsetDateTime.now());
            error.setStatus(500);
            error.setError("Server error");
            error.setMessage("Error processing PDF: " + e.getMessage());
            error.setPath("/api/v1/feed-ask/feed");
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public ResponseEntity<AskResponse> askQuestion(String prompt) {

        float[] questionEmbedding = embeddingService.generateEmbedding(prompt);
        List<RagEntity> allChuncks = ragRepository.findAll();


        List<RagEntity> contextEntities = allChuncks.stream()
                .sorted((c1, c2) -> {

                    float[] e1 = c1.getEmbedding() == null ? new float[0] : OBJECT_MAPPER.readValue(
                            c1.getEmbedding(), float[].class
                    );
                    float[] e2 = c2.getEmbedding() == null ? new float[0] : OBJECT_MAPPER.readValue(
                            c2.getEmbedding(), float[].class
                    );

                    double s1 = cosineSimilarity(
                            questionEmbedding, e1);
                    double s2 = cosineSimilarity(
                            questionEmbedding, e2);

                    return Double.compare(s2, s1); // DESC order
                })
                .limit(TOP_K)
                .toList();
        String context = contextEntities.
                stream()
                .map(RagEntity::getContent)
                .collect(Collectors.joining("\n\n"));

        log.info("Context for question: \n{}", context);

        String s = questionAnsweringService.generateAnswer(prompt, context);
        AskResponse askResponse = new AskResponse();
        askResponse.setAnswer(s);
        askResponse.setStatus(AskResponse.StatusEnum.SUCCESS);
        return ResponseEntity.ok(askResponse);
    }


    public static double cosineSimilarity(float[] v1, float[] v2) {
        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

}
