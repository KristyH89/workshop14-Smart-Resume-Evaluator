# 📄 Smart Resume Evaluator

![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen?style=flat-square&logo=springboot)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-brightgreen?style=flat-square&logo=spring)
![Apache Tika](https://img.shields.io/badge/Apache%20Tika-3.3.1-blue?style=flat-square&logo=apache)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)

A Spring Boot API that compares a resume against a job description and returns a
structured evaluation: a match score, proven strengths, missing skills and
actionable suggestions. Built for Lexicon Workshop 1.

Resumes and job descriptions can be sent as plain text or uploaded as PDF files.
Personal data in an uploaded resume is redacted before anything is sent to the
AI model.

---

## 🎯 Learning goals

| Goal | How it is applied in this project |
|------|-----------------------------------|
| **Personas (system prompts)** | The AI acts as a Senior Technical Recruiter. The persona lives in `prompts/recruiter-system.st`, separate from the Java code, so it can be changed without recompiling. |
| **Prompt templates** | The user prompt uses `{resumeText}` and `{jobDescriptionText}` placeholders, filled at runtime with `.param(...)`. No string concatenation. |
| **Structured output** | `ChatClient.entity(ResumeEvaluation.class)` turns the model response into a Java record. Field level guidance is given through `@JsonPropertyDescription`, which ends up in the generated JSON schema. |

---

## 🛠️ Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| AI | Spring AI 2.0.0, OpenAI (gpt-4o) |
| Validation | Jakarta Bean Validation |
| Text extraction | Apache Tika 3.3.1 |
| Build | Maven |

---

## 🚀 Getting started

### Prerequisites

* JDK 25
* An OpenAI API key

### Configuration

The API key is read from an environment variable and is never stored in the
repository.

**IntelliJ IDEA:** Run → Edit Configurations → Environment variables →
`OPENAI_API_KEY=your-key-here`

**PowerShell:**

```powershell
$env:OPENAI_API_KEY="your-key-here"
```

### Run

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts on `http://localhost:8080`.

The chat model runs with `temperature: 0.0`. For an evaluation task this is a
deliberate choice: repeated runs of the same request should give the same
verdict. See the reproducibility section below for the measurements behind it.

---

## 📡 Endpoints

| Method | Path | Body | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/evaluations` | JSON | Evaluate a resume and job description sent as plain text |
| `POST` | `/api/v1/evaluations/upload` | multipart/form-data | Evaluate an uploaded resume file, with the job description as a file or as text |

### Example request

```http
POST http://localhost:8080/api/v1/evaluations
Content-Type: application/json

{
  "resumeText": "...",
  "jobDescriptionText": "..."
}
```

### Example response

```json
{
  "matchScore": 71,
  "summary": "You have a strong foundation in web design and development, but gaining experience with Elementor and web hosting will improve your fit for this role.",
  "requiredSkills": [
    "Practical experience building websites in an established website builder environment",
    "Understanding of how a website is built, published and maintained",
    "A good sense of form, structure and usability"
  ],
  "strengths": [
    "Practical experience building websites: Built a personal portfolio site in WordPress and other projects.",
    "Understanding of how a website is built, published and maintained: Experience with building and publishing websites."
  ],
  "missingSkills": [
    "Build and publish websites in WordPress and Elementor"
  ],
  "suggestions": [
    "Gain experience with Elementor to enhance your WordPress skills."
  ],
  "recommendation": "POSSIBLE_MATCH"
}
```

`recommendation` is one of `STRONG_MATCH`, `POSSIBLE_MATCH` or `WEAK_MATCH`.

---

## 🧱 Project structure

| Package | Responsibility |
|---------|----------------|
| `controller` | HTTP endpoints and request validation |
| `service` | Evaluation logic, text extraction and redaction |
| `dto` | `EvaluationRequest` and `ResumeEvaluation` records |
| `exception` | Global exception handling |
| `resources/prompts` | The recruiter system prompt |

---

## 🔒 Privacy: redaction before the AI call

An uploaded resume passes through `RedactionService` before it reaches the
model. Detected personal data is replaced with `[REDACTED]`.

| Pattern | Example |
|---------|---------|
| Email address | `name@example.com` |
| Phone number | `+46 70 123 45 67`, `070-123 45 67` |
| Swedish personal identity number | `YYMMDD-XXXX` |
| Street address | a line containing a street name followed by a number |
| Postal code and city | `331 30 Varnamo` |
| LinkedIn and GitHub profile URLs | `linkedin.com/in/...` |

The order of replacement matters. A personal identity number also matches the
phone number pattern, so the most specific patterns are applied first.

**Known limitations.** Names are not redacted, because regular expressions
cannot detect them reliably. That would require named entity recognition. The
phone number pattern is deliberately broad, which means it can also match year
ranges. For privacy, over-redacting was preferred over under-redacting. Only the
resume is redacted, not the job description, since a job description contains no
personal data belonging to the candidate.

---

## 🧪 Prompt engineering findings

The Java code was finished quickly. Almost all of the quality of this API turned
out to live in the system prompt and in the schema descriptions. Each change
below was tested against the same request.

| Iteration | Change | Result |
|-----------|--------|--------|
| 1 | Baseline persona | Nice-to-have skills were reported as missing. Feedback was vague. |
| 2 | Distinguish required from nice-to-have skills | Nice-to-have skills disappeared from `missingSkills`. Correct. |
| 3 | Derive `recommendation` from `matchScore` | The recommendation became reproducible across runs. |
| 4 | Sharpen the `@JsonPropertyDescription` on `strengths` | The model started adding evidence to each strength instead of listing bare keywords. Schema level guidance worked where a system prompt rule did not. |
| 5 | Add a `requiredSkills` field | Made the model list the requirements before scoring, which exposed that it was reading only part of the job description. |
| 6 | Require `strengths` plus `missingSkills` to cover `requiredSkills` | The lists became consistent, but the model started guessing evidence to fill the gaps. |
| 7 | Restrict `requiredSkills` to the requirements section, in both the prompt and the schema | `requiredSkills` became identical across all runs. Task descriptions no longer leaked into the requirements. |

### Where guidance belongs: prompt or schema

Two mechanisms were used to steer the model, and they are not equally effective
for the same job.

A rule in the **system prompt** works well for relationships between fields.
Deriving the recommendation from the score, and requiring the two skill lists to
cover the requirements, both worked on the first attempt.

A **`@JsonPropertyDescription` on the field itself** works better for the content
of a single field. Asking for evidence in `strengths` was ignored as a prompt
rule, but was followed immediately once it was placed on the field, where it ends
up in the generated JSON schema.

### Reproducibility

To find out how stable the output is, the same request was sent three times per
combination.

| Setting | Result over three identical runs |
|---------|----------------------------------|
| `temperature: 0.2` | Scores of 43, 43 and 57 for the same resume and job description, with different recommendations |
| `temperature: 0.0`, before iteration 7 | Stable score, but the supporting lists still differed per run |
| `temperature: 0.0`, after iteration 7 | Junior developer vacancy: three identical responses. Web designer vacancy: identical score, still varying evidence |

Setting the temperature to 0 was necessary but not sufficient. The remaining
variation came from one step: deciding which sentences in the job description
count as a requirement. Once iteration 7 fixed that step, the vacancy with an
explicit numbered requirements list became fully reproducible.

### The model does not aggregate reliably

The scoring rule asks for the percentage of requirements that the resume proves.
The model does not consistently compute it.

| Case | Proven | Total | Expected | Returned |
|------|--------|-------|----------|----------|
| CV3 against the junior developer vacancy | 6 | 6 | 100 | 83 |
| CV3 against the web designer vacancy | 4 | 7 | 57 | 57 |
| CV1 against the web designer vacancy | 1 | 7 | 14 | 28 |

Sometimes the arithmetic is exact, sometimes the score is a plausible impression
that does not follow from the model's own lists. The score turned out to be the
most stable part of the response, which suggests it is formed as a general
impression rather than calculated from the lists.

**Conclusion:** structure and judgement can be enforced through the prompt and
the schema. Aggregation cannot. For consistent numbers the score should be
calculated in Java from the size of the returned lists, leaving the model to do
what it is good at: judging text.

---

## 📊 Validation

Three resume variants were evaluated against two job descriptions. All figures
were measured with `temperature: 0.0` and the final prompt.

| Resume variant | Web designer (POZ) | Junior fullstack developer |
|----------------|-------------------|---------------------------|
| CV1, no project section | 28 | 50 |
| CV2, projects with a design focus | 57 | 50 |
| CV3, projects with a development focus | 57 | **83** |

**Adding projects has a large effect.** The same resume went from 28 to 57 on the
web designer vacancy purely by adding a project section with concrete, linked
work. Skills mentioned as study topics were not accepted as proof, while the same
skills demonstrated in a project were.

**Relevance matters more than volume.** CV3 scores 83 on the developer vacancy
and 57 on the design vacancy, while CV1 and CV2 both score 50 on the developer
vacancy. The evaluator rewards the right projects rather than more projects.

**Soft skill vacancies do not differentiate.** CV2 and CV3 score identically on
the web designer vacancy. Five of its seven requirements are soft skills that
cannot be proven from a resume, and the two technical requirements are worded so
strictly that only genuine website builder work qualifies. Both variants contain
the same WordPress project, so both score the same. This is a limitation of the
approach rather than of the implementation: the evaluator is only as precise as
the requirements it is given.

---

## 🔭 Possible next steps

* Calculate `matchScore` in Java from the size of the returned lists instead of asking the model to compute it, and derive `recommendation` from that value
* Add unit tests with a mocked `ChatClient`
* Support scanned PDFs through OCR, which Tika currently returns as empty text
* Use named entity recognition to redact names
