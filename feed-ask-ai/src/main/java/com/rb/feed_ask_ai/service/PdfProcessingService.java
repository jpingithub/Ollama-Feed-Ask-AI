package com.rb.feed_ask_ai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfProcessingService {


    private static final int CHUNK_SIZE = 1000;
    private static final int OVERLAP = 200;

    public static List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");

        StringBuilder buffer = new StringBuilder();

        for (String sentence : sentences) {
            if (buffer.length() + sentence.length() > CHUNK_SIZE) {
                chunks.add(buffer.toString());

                // overlap
                String overlapText = buffer.substring(
                        Math.max(0, buffer.length() - OVERLAP)
                );

                buffer.setLength(0);
                buffer.append(overlapText);
            }
            buffer.append(sentence).append(" ");
        }

        if (!buffer.isEmpty()) {
            chunks.add(buffer.toString());
        }

        return chunks;
    }


    public List<RagChunk> processPdf(MultipartFile file) {

        List<RagChunk> result = new ArrayList<>();
        List<String> pages = readPages(file);

        int pageNo = 1;
        for (String pageText : pages) {

            List<String> chunks = chunk(pageText);
            for (String chunk : chunks) {
                result.add(new RagChunk(
                        chunk,
                        pageNo
                ));
            }
            pageNo++;
        }
        return result;

    }


    public static List<String> readPages(MultipartFile file) {
        List<String> pages = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             PDDocument document = Loader.loadPDF(is.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document)
                        .replaceAll("\\s+", " ")
                        .trim();

                if (!text.isEmpty()) {
                    pages.add(text);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to read PDF", e);
        }
        return pages;
    }

    public record RagChunk(String content, int pageNumber) {
    }


}
