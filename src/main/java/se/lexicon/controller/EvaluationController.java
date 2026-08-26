package se.lexicon.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import se.lexicon.dto.EvaluationRequest;
import se.lexicon.dto.ResumeEvaluation;
import se.lexicon.service.DocumentTextExtractor;
import se.lexicon.service.EvaluationService;
import se.lexicon.service.RedactionService;

@RestController
@RequestMapping("/api/v1/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final DocumentTextExtractor textExtractor;
    private final RedactionService redactionService;

    public EvaluationController(EvaluationService evaluationService,
                                DocumentTextExtractor textExtractor,
                                RedactionService redactionService) {
        this.evaluationService = evaluationService;
        this.textExtractor = textExtractor;
        this.redactionService = redactionService;
    }

    @PostMapping
    public ResponseEntity<ResumeEvaluation> evaluate(@RequestBody @Valid EvaluationRequest request) {

        ResumeEvaluation result = evaluationService.evaluate(
                request.resumeText(),
                request.jobDescriptionText()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeEvaluation> evaluateUpload(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam(value = "jobDescription", required = false) MultipartFile jobDescription,
            @RequestParam(value = "jobDescriptionText", required = false) String jobDescriptionText) {

        // Extract and redact the resume before it reaches the AI
        String rawText = textExtractor.extractText(resume);
        String resumeText = redactionService.redact(rawText);

        // The job description may come as a file or as plain text
        String jobText = (jobDescription != null && !jobDescription.isEmpty())
                ? textExtractor.extractText(jobDescription)
                : jobDescriptionText;

        if (jobText == null || jobText.isBlank()) {
            throw new IllegalArgumentException(
                    "Provide a job description, either as a file or as text");
        }

        return ResponseEntity.ok(evaluationService.evaluate(resumeText, jobText));
    }
}

