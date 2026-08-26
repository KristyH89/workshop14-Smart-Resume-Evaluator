package se.lexicon.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class RedactionServiceImpl implements RedactionService {

    private static final String PLACEHOLDER = "[REDACTED]";

    private static final Pattern EMAIL = Pattern.compile(
            "[\\w.+-]+@[\\w-]+\\.[\\w.-]+");

    // Swedish and international formats: +46 70 123 45 67, 070-123 45 67, 0701234567
    private static final Pattern PHONE = Pattern.compile(
            "(\\+\\d{1,3}[\\s-]?)?\\(?\\d{2,4}\\)?[\\s-]?\\d{2,3}[\\s-]?\\d{2,3}[\\s-]?\\d{2,4}");

    // Swedish personal identity number: YYMMDD-XXXX or YYYYMMDD-XXXX
    private static final Pattern PERSONAL_ID = Pattern.compile(
            "\\b(19|20)?\\d{6}[-+]?\\d{4}\\b");

    // Street address followed by a house number
    private static final Pattern ADDRESS = Pattern.compile(
            "(?im)^.*\\b(gatan|vagen|vägen|street|straat|road|laan|str\\.?)\\s*\\d+.*$");

    // Swedish postal code followed by a city
    private static final Pattern POSTAL_CODE = Pattern.compile(
            "\\b\\d{3}\\s?\\d{2}\\s+[A-ZÅÄÖ][a-zåäö]+");

    private static final Pattern URL_PROFILE = Pattern.compile(
            "(?i)(https?://)?(www\\.)?(linkedin\\.com|github\\.com)/\\S+");

    @Override
    public String redact(String text) {

        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text;

        // Order matters: personal id before phone, otherwise phone eats the digits
        result = PERSONAL_ID.matcher(result).replaceAll(PLACEHOLDER);
        result = EMAIL.matcher(result).replaceAll(PLACEHOLDER);
        result = URL_PROFILE.matcher(result).replaceAll(PLACEHOLDER);
        result = POSTAL_CODE.matcher(result).replaceAll(PLACEHOLDER);
        result = ADDRESS.matcher(result).replaceAll(PLACEHOLDER);
        result = PHONE.matcher(result).replaceAll(PLACEHOLDER);

        return result;
    }
}