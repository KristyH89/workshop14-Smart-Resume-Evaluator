package se.lexicon.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record ResumeEvaluation(

        @JsonPropertyDescription("Overall match score from 0 to 100")
        int matchScore,

        @JsonPropertyDescription("Short verdict for the candidate, maximum 2 sentences")
        String summary,

        @JsonPropertyDescription("Skills and experiences from the resume that match the job requirements")
        List<String> strengths,

        @JsonPropertyDescription("Required skills from the job description that are missing in the resume")
        List<String> missingSkills,

        @JsonPropertyDescription("Concrete actions the candidate can take to improve the match")
        List<String> suggestions,

        @JsonPropertyDescription("Hiring recommendation based on the score")
        Recommendation recommendation
) {
    public enum Recommendation {
        STRONG_MATCH, POSSIBLE_MATCH, WEAK_MATCH
    }
}