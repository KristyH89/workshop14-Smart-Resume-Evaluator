package se.lexicon.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentTextExtractor {

    String extractText(MultipartFile file);
}