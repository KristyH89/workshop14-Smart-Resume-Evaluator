package se.lexicon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvaluationRequest(

        @NotBlank(message = "Resume text is required")
        @Size(min = 50, message = "Resume text is too short to evaluate")
        String resumeText,

        @NotBlank(message = "Job description text is required")
        @Size(min = 50, message = "Job description text is too short to evaluate")
        String jobDescriptionText
) {}