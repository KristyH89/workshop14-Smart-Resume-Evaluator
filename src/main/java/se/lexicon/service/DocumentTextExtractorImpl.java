package se.lexicon.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class DocumentTextExtractorImpl implements DocumentTextExtractor {

    // Guard against PDFs with an unreasonable amount of text
    private static final int MAX_CHARACTERS = 100_000;

    private final Tika tika = new Tika();

    @Override
    public String extractText(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        tika.setMaxStringLength(MAX_CHARACTERS);

        try (InputStream stream = file.getInputStream()) {
            String text = tika.parseToString(stream);

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "No text could be extracted. The file may be a scanned image.");
            }
            return text;

        } catch (IOException | TikaException e) {
            throw new RuntimeException("Failed to extract text from " + file.getOriginalFilename(), e);
        }
    }
}