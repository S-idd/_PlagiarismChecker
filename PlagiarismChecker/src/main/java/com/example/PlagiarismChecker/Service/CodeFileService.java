package com.example.PlagiarismChecker.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
		logger.info("Starting migration to populate trigram vectors for existing files...");
		List<CodeFile> files = codeFileRepository.findAll();
		for (CodeFile file : files) {
			if (file.Gettrigram_vector() == null) {
				String content = file.getContent();
				String language = file.getLanguage();
				Map<String, Integer> trigrams = generateTrigrams(content, language);
				file.Settrigram_vector(trigrams);
				codeFileRepository.save(file);
				logger.info("Populated trigrams for file ID {}: {} trigrams", file.getId(), trigrams.size());
			}
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

	// Calculate similarity between two files using cosine similarity on cached
	// trigrams
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

	// Generate trigram frequency map
	public Map<String, Integer> generateTrigrams(String content, String language) {
		Map<String, Integer> trigrams = new HashMap<>();
		if (content == null || content.length() < 3) {
			logger.warn("Content too short to generate trigrams for language {}: {}", language, content);
			return trigrams;
		}
		// Remove redundant normalization since uploadFile already does this
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

		// Language-specific normalization (optional, can be expanded with ANTLR)
		switch (language.toUpperCase()) {
		case "PYTHON":
		case "RUBY":
			content = content.replaceAll("#.*?(?=\\s|$)", ""); // Remove single-line comments
			break;
		case "CPP":
		case "JAVA":
		case "JAVASCRIPT":
		case "TYPESCRIPT":
		case "GO":
			content = content.replaceAll("//.*?(?=\\s|$)", "") // Remove single-line comments
					.replaceAll("/\\*.*?\\*/", ""); // Remove multi-line comments
			break;
		case "ADA":
			content = content.replaceAll("--.*?(?=\\s|$)", ""); // Remove Ada comments
			break;
		}
		String normalizedContent = content.trim();
		logger.info("Normalized content for language {}: {}", language, normalizedContent);
		return normalizedContent;
	}

	// Compare a file against all other files in the database with pagination
	public Page<SimilarityResult> compareAgainstAll(Long fileId, Pageable pageable) {
		logger.info("Comparing file ID {} against all other files with pagination", fileId);
		CodeFile targetFile = codeFileRepository.findById(fileId)
				.orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

		Map<String, Integer> targetVectorString = targetFile.Gettrigram_vector();
		if (targetVectorString == null) {
			throw new IllegalStateException("Trigram vector not found for file: " + fileId);
		}

		// Convert target vector to Map<CharSequence, Integer>
		Map<CharSequence, Integer> targetVector = new HashMap<>();
		targetVectorString.forEach((key, value) -> targetVector.put(key, value));

		// Fetch files with pagination
		Page<CodeFile> allFiles = codeFileRepository.findAll(pageable);
		CosineSimilarity cosineSimilarity = new CosineSimilarity();

		List<SimilarityResult> results = allFiles.getContent().stream().filter(file -> !file.getId().equals(fileId)) // Exclude
																														// the
																														// target
																														// file
																														// itself
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
					logger.info("Similarity between files {} and {}: {}%", fileId, file.getId(), roundedSimilarity);

					return new SimilarityResult(file.getId(), file.getFileName(),
							file.getLanguage(), roundedSimilarity);
				}).filter(result -> result != null)
				.sorted((r1, r2) -> Double.compare(r2.getSimilarity(), r1.getSimilarity())) // Sort by similarity
																							// descending
				.collect(Collectors.toList());

		return new PageImpl<>(results, pageable, allFiles.getTotalElements());
	}
}