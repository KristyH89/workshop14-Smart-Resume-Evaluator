package se.lexicon.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import se.lexicon.dto.ResumeEvaluation;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    // User prompt template with placeholders, filled at runtime
    private static final String USER_TEMPLATE = """
            Evaluate the following resume against the job description below.

            ### RESUME
            {resumeText}

            ### JOB DESCRIPTION
            {jobDescriptionText}
            """;

    private final ChatClient chatClient;
    private final Resource systemPrompt;

    public EvaluationServiceImpl(ChatClient.Builder builder,
                                 @Value("classpath:/prompts/recruiter-system.st") Resource systemPrompt) {
        this.chatClient = builder.build();
        this.systemPrompt = systemPrompt;
    }

    @Override
    public ResumeEvaluation evaluate(String resumeText, String jobDescriptionText) {

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("resumeText cannot be null or empty");
        }
        if (jobDescriptionText == null || jobDescriptionText.isBlank()) {
            throw new IllegalArgumentException("jobDescriptionText cannot be null or empty");
        }

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u.text(USER_TEMPLATE)
                            .param("resumeText", resumeText)
                            .param("jobDescriptionText", jobDescriptionText))
                    .call()
                    .entity(ResumeEvaluation.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate resume", e);
        }
    }
}