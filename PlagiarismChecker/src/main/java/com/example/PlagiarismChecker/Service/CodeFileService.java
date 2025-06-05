package com.example.PlagiarismChecker.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.text.similarity.CosineSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.PlagiarismChecker.Repository.CodeFileRepository;
import com.example.PlagiarismChecker.model.CodeFile;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class CodeFileService {

    private static final Logger logger = LoggerFactory.getLogger(CodeFileService.class);

    @Autowired
    private CodeFileRepository codeFileRepository;

    @Autowired
    private Validator validator;

    public List<CodeFile> GetAllFiles() {
        return codeFileRepository.findAll();
    }

    // Supported languages and their file extensions
    private static final Map<String, String[]> SUPPORTED_LANGUAGES = new HashMap<>();

    static {
        SUPPORTED_LANGUAGES.put("JAVA", new String[] { ".java" });
        SUPPORTED_LANGUAGES.put("PYTHON", new String[] { ".py" });
        SUPPORTED_LANGUAGES.put("CPP", new String[] { ".cpp", ".h", ".hpp" });
        SUPPORTED_LANGUAGES.put("GO", new String[] { ".go" });
        SUPPORTED_LANGUAGES.put("RUBY", new String[] { ".rb" });
        SUPPORTED_LANGUAGES.put("ADA", new String[] { ".ada", ".adb", ".ads" });
        SUPPORTED_LANGUAGES.put("JAVASCRIPT", new String[] { ".js" });
        SUPPORTED_LANGUAGES.put("TYPESCRIPT", new String[] { ".ts" });
    }

    // Migrate existing files to populate trigram_vector if null
    @PostConstruct
    public void migrateExistingFiles() {
        logger.info("Starting migration to repopulate trigram vectors for existing files...");
        List<CodeFile> files = codeFileRepository.findAll();
        for (CodeFile file : files) {
            String content = file.getContent();
            String language = file.getLanguage();
            String normalizedContent = normalizeContent(content, language); // Re-normalize
            Map<String, Integer> trigrams = generateTrigrams(normalizedContent, language);
            file.Settrigram_vector(trigrams);
            codeFileRepository.save(file);
            logger.info("Repopulated trigrams for file ID {}: {} trigrams", file.getId(), trigrams.size());
        }
        logger.info("Migration completed. Processed {} files.", files.size());
    }

    // Save uploaded file to database
    public CodeFile uploadFile(MultipartFile file, String language) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        String fileName = file.getOriginalFilename();
        String langUpper = language.toUpperCase();

        if (fileName == null || !isValidExtension(fileName, langUpper)) {
            throw new IllegalArgumentException(
                    "Invalid file extension for language " + language + ". Supported extensions: "
                            + String.join(", ", SUPPORTED_LANGUAGES.getOrDefault(langUpper, new String[] {})));
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        CodeFile codeFile = new CodeFile();
        codeFile.setFileName(fileName);
        String normalizedContent = normalizeContent(content, langUpper);
        codeFile.setContent(normalizedContent);
        codeFile.setLanguage(langUpper);
        // Generate and store trigrams
        Map<String, Integer> trigrams = generateTrigrams(normalizedContent, langUpper);
        codeFile.Settrigram_vector(trigrams);
        logger.info("Generated trigrams for file {}: {} trigrams: {}", fileName, trigrams.size(), trigrams);

        Set<ConstraintViolation<CodeFile>> violations = validator.validate(codeFile);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        logger.info("Saving file: {} with language: {}", fileName, langUpper);
        CodeFile savedFile = codeFileRepository.save(codeFile);
        logger.info("Saved file ID {} with trigram vector: {}", savedFile.getId(), savedFile.Gettrigram_vector());
        return savedFile;
    }

    // Validate file extension against supported languages
    public boolean isValidExtension(String fileName, String language) {
        String[] extensions = SUPPORTED_LANGUAGES.get(language);
        if (extensions == null) {
            return false;
        }
        for (String ext : extensions) {
            if (fileName.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    // Calculate similarity between two files using cosine similarity on cached trigrams
    public double calculateSimilarity(Long fileId1, Long fileId2) {
        logger.info("Comparing files: {} vs. {}", fileId1, fileId2);
        CodeFile file1 = codeFileRepository.findById(fileId1)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId1));
        CodeFile file2 = codeFileRepository.findById(fileId2)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId2));

        Map<String, Integer> vector1String = file1.Gettrigram_vector();
        Map<String, Integer> vector2String = file2.Gettrigram_vector();

        if (vector1String == null || vector2String == null) {
            throw new IllegalStateException("Trigram vectors not found for files: " + fileId1 + ", " + fileId2);
        }

        // Convert Map<String, Integer> to Map<CharSequence, Integer>
        Map<CharSequence, Integer> vector1 = new HashMap<>();
        Map<CharSequence, Integer> vector2 = new HashMap<>();
        vector1String.forEach((key, value) -> vector1.put(key, value));
        vector2String.forEach((key, value) -> vector2.put(key, value));

        CosineSimilarity cosineSimilarity = new CosineSimilarity();
        double similarity = cosineSimilarity.cosineSimilarity(vector1, vector2) * 100;
        // Round to 2 decimal places
        double roundedSimilarity = Math.round(similarity * 100.0) / 100.0;
        logger.info("Similarity between files {} and {}: {}%", fileId1, fileId2, roundedSimilarity);
        return roundedSimilarity;
    }

    // Compare a file against all other files in the database with pagination and filters
    public Page<SimilarityResult> compareAgainstAll(Long fileId, Pageable pageable, String languageFilter, Double minSimilarity) {
        logger.info("Comparing file ID {} against all other files with pagination, languageFilter: {}, minSimilarity: {}", fileId, languageFilter, minSimilarity);
        CodeFile targetFile = codeFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        Map<String, Integer> targetVectorString = targetFile.Gettrigram_vector();
        if (targetVectorString == null) {
            throw new IllegalStateException("Trigram vector not found for file: " + fileId);
        }

        // Normalize and validate languageFilter
        String normalizedLanguageFilter = languageFilter;
        if (languageFilter != null && !languageFilter.isEmpty()) {
            String langUpper = languageFilter.toUpperCase();
            if (!SUPPORTED_LANGUAGES.containsKey(langUpper)) {
                throw new IllegalArgumentException("Unsupported language: " + languageFilter + ". Supported languages: " + SUPPORTED_LANGUAGES.keySet());
            }
            normalizedLanguageFilter = langUpper; // Use this normalized value in the query
        }

        // Validate minSimilarity and create an effectively final variable
        final double effectiveMinSimilarity = (minSimilarity == null) ? 0.0 : minSimilarity;

        // Convert target vector to Map<CharSequence, Integer>
        Map<CharSequence, Integer> targetVector = new HashMap<>();
        targetVectorString.forEach((key, value) -> targetVector.put(key, value));

        // Fetch files with pagination and language filter at the database level
        Page<CodeFile> allFiles = codeFileRepository.findByLanguage(normalizedLanguageFilter, pageable);
        logger.info("Fetched {} files from database for comparison with file ID {}", allFiles.getTotalElements(), fileId);

        // Log all files fetched
        allFiles.getContent().forEach(file -> 
            logger.debug("Fetched file: ID {}, Name {}, Language {}", file.getId(), file.getFileName(), file.getLanguage())
        );

        CosineSimilarity cosineSimilarity = new CosineSimilarity();
        List<SimilarityResult> results = allFiles.getContent().stream()
                .peek(file -> logger.debug("Processing file: ID {}, Name {}, Language {}", file.getId(), file.getFileName(), file.getLanguage()))
                .filter(file -> {
                    boolean shouldInclude = !Objects.equals(file.getId(), fileId);
                    logger.debug("File ID {}: Should include? {} (target file ID: {})", file.getId(), shouldInclude, fileId);
                    return shouldInclude;
                }) // Exclude the target file itself
                .map(file -> {
                    Map<String, Integer> otherVectorString = file.Gettrigram_vector();
                    if (otherVectorString == null) {
                        logger.warn("Trigram vector not found for file ID {}", file.getId());
                        return null;
                    }

                    // Convert other vector to Map<CharSequence, Integer>
                    Map<CharSequence, Integer> otherVector = new HashMap<>();
                    otherVectorString.forEach((key, value) -> otherVector.put(key, value));

                    double similarity = cosineSimilarity.cosineSimilarity(targetVector, otherVector) * 100;
                    double roundedSimilarity = Math.round(similarity * 100.0) / 100.0;
                    logger.info("Similarity between files {} and {} ({}): {}%", fileId, file.getId(), file.getLanguage(), roundedSimilarity);

                    return new SimilarityResult(file.getId(), file.getFileName(), file.getLanguage(), roundedSimilarity);
                })
                .filter(result -> result != null)
                .filter(result -> result.getSimilarity() >= effectiveMinSimilarity) // Use the final variable
                .peek(result -> logger.debug("Result: File ID {}, Language {}, Similarity {}", result.getFileId(), result.getLanguage(), result.getSimilarity()))
                .sorted((r1, r2) -> Double.compare(r2.getSimilarity(), r1.getSimilarity())) // Sort by similarity descending
                .collect(Collectors.toList());

        logger.info("Returning {} results after filtering and sorting", results.size());
        return new PageImpl<>(results, pageable, allFiles.getTotalElements());
    }

    // Generate trigram frequency map
    public Map<String, Integer> generateTrigrams(String content, String language) {
        Map<String, Integer> trigrams = new HashMap<>();
        if (content == null || content.length() < 3) {
            logger.warn("Content too short to generate trigrams for language {}: {}", language, content);
            return trigrams;
        }
        // Limit content length to prevent memory issues
        content = content.substring(0, Math.min(content.length(), 100_000));
        for (int i = 0; i < content.length() - 2; i++) {
            String trigram = content.substring(i, i + 3);
            trigrams.compute(trigram, (k, v) -> (v == null) ? 1 : v + 1);
        }
        logger.debug("Generated {} trigrams for language {}: {}", trigrams.size(), language, trigrams);
        return trigrams;
    }

    // Normalize content for trigram generation
    public String normalizeContent(String content, String language) {
        if (content == null || content.trim().isEmpty()) {
            logger.warn("Empty content provided for language: {}", language);
            return "";
        }
        content = content.replaceAll("\\s+", " ").toLowerCase();

        // Remove comments based on language
        switch (language.toUpperCase()) {
        case "PYTHON":
        case "RUBY":
            content = content.replaceAll("#.*?(?=\\s|$)", "");
            break;
        case "CPP":
        case "JAVA":
        case "JAVASCRIPT":
        case "TYPESCRIPT":
        case "GO":
            content = content.replaceAll("//.*?(?=\\s|$)", "")
                    .replaceAll("/\\*.*?\\*/", "");
            break;
        case "ADA":
            content = content.replaceAll("--.*?(?=\\s|$)", "");
            break;
        }

        // Remove language-specific syntax and keywords
        content = content.replaceAll("[{}();\\[\\]]", " ")
                 .replaceAll("\\b(public|private|protected|static|final|abstract|class|def|function|void|int|float|double|str|string|bool|boolean|if|else|for|while|do|return|break|continue|try|catch|throw|new|self|this|super|package|import|include|using|namespace|struct|type)\\b", " ")
                 .replaceAll("\\b(true|false|null|none|nil)\\b", " ")
                 .replaceAll("=", " ")
                 .replaceAll("[+\\-*/%><!&|]", " ")
                 .replaceAll("\\b(print|println|cout|printf|puts|put|write|log|console)\\b", " "); // Normalize output functions

        // Normalize identifiers (camelCase to snake_case)
        content = content.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();

        // Remove numbers and special characters, keep only words
        content = content.replaceAll("[0-9]", " ").replaceAll("[^a-z_\\s]", " ");

        content = content.replaceAll("\\s+", " ");
        String normalizedContent = content.trim();
        logger.info("Normalized content for language {}: {}", language, normalizedContent);
        return normalizedContent;
    }

    // Delete all files from the database (for testing purposes)
    public void deleteAllFiles() {
        logger.info("Deleting all files from the database...");
        codeFileRepository.deleteAll();
        logger.info("All files deleted successfully.");
    }
}