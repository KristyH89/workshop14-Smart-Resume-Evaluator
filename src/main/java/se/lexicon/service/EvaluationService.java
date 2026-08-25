package se.lexicon.service;

import se.lexicon.dto.ResumeEvaluation;

public interface EvaluationService {

    ResumeEvaluation evaluate(String resumeText, String jobDescriptionText);
}