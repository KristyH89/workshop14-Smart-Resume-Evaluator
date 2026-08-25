package se.lexicon.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.lexicon.dto.EvaluationRequest;
import se.lexicon.dto.ResumeEvaluation;
import se.lexicon.service.EvaluationService;

@RestController
@RequestMapping("/api/v1/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public ResponseEntity<ResumeEvaluation> evaluate(@RequestBody @Valid EvaluationRequest request) {

        ResumeEvaluation result = evaluationService.evaluate(
                request.resumeText(),
                request.jobDescriptionText()
        );

        return ResponseEntity.ok(result);
    }
}