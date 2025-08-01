package com.example.PlagiarismChecker.Service;

import java.io.IOException;

import java.io.InputStream;

import java.math.BigDecimal;

import java.math.RoundingMode;

import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.Objects;

import java.util.Set;

import java.io.BufferedReader;

import java.io.InputStreamReader;

import java.util.stream.Collectors;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageImpl;

import org.springframework.data.domain.Pageable;

import com.example.PlagiarismChecker.DTO.CodeFileSummary;
import com.example.PlagiarismChecker.Repository.CodeFileRepository;
import com.example.PlagiarismChecker.model.CodeFile;

import jakarta.annotation.PostConstruct;

import jakarta.validation.ConstraintViolation;

import jakarta.validation.ConstraintViolationException;

import jakarta.validation.Validator;

import jakarta.validation.constraints.NotNull;

@Service

public class CodeFileService {

	private static final Logger logger = LoggerFactory.getLogger(CodeFileService.class);

	private static final int MAX_FILE_IDS = 100; // Limit for performance

	private static final int MAX_CONTENT_LENGTH = 50_000; // Reduced for memory efficiency

	@Autowired

	private CodeFileRepository codeFileRepository;

	@Autowired

	private final CustomCosineSimilarity cosineSimilarity;

	@Autowired

	private Validator validator;

	@Cacheable(value = "all-files", key = "'all'")
	public Page<CodeFileSummary> GetAllFilesASAP(Pageable pageable) {
		return codeFileRepository.findAllBy(pageable);
	}

	private static final Map<String, String[]> SUPPORTED_LANGUAGES = new HashMap<>();

	static {

		SUPPORTED_LANGUAGES.put("JAVA", new String[] { ".java" });

		SUPPORTED_LANGUAGES.put("PYTHON", new String[] { ".py", "ipynb" });

		SUPPORTED_LANGUAGES.put("CPP", new String[] { ".cpp", ".h", ".hpp" });

		SUPPORTED_LANGUAGES.put("GO", new String[] { ".go" });

		SUPPORTED_LANGUAGES.put("RUBY", new String[] { ".rb" });

		SUPPORTED_LANGUAGES.put("ADA", new String[] { ".ada", ".adb", ".ads" });

		SUPPORTED_LANGUAGES.put("JAVASCRIPT", new String[] { ".js" });

		SUPPORTED_LANGUAGES.put("TYPESCRIPT", new String[] { ".ts" });

	}

	public CodeFileService(CodeFileRepository codeFileRepository, Validator validator,

			CustomCosineSimilarity cosineSimilarity) {

		this.codeFileRepository = codeFileRepository;

		this.validator = validator;

		this.cosineSimilarity = cosineSimilarity;

	}

	@PostConstruct

	public void migrateExistingFiles() {

		logger.info("Starting migration to repopulate trigram vectors for existing files...");

		List<CodeFile> files = codeFileRepository.findAll();

		for (CodeFile file : files) {

			String content = file.getContent();

			String language = file.getLanguage();

			String normalizedContent = normalizeContent(content, language);

			Map<String, Integer> trigrams = generateTrigrams(normalizedContent, language);

			file.Settrigram_vector(trigrams);

			codeFileRepository.save(file);

			logger.info("Repopulated trigrams for file ID {}: {} trigrams", file.getId(), trigrams.size());

		}

		logger.info("Migration completed. Processed {} files.", files.size());

	}

	public CodeFile uploadFileStream(InputStream inputStream, String fileName, String language) throws IOException {

		if (inputStream == null) {

			throw new IllegalArgumentException("File input stream cannot be null");

		}

		String langUpper = language.toUpperCase();

		if (fileName == null || !isValidExtension(fileName, langUpper)) {

			throw new IllegalArgumentException(

					"Invalid file extension for language " + language + ". Supported extensions: "

							+ String.join(", ", SUPPORTED_LANGUAGES.getOrDefault(langUpper, new String[] {})));

		}

		// Estimate size (approximate, adjust if needed)

		long fileSize = inputStream.available();

		if (fileSize > 10 * 1024 * 1024) {

			throw new IllegalArgumentException("File size exceeds 10MB limit");

		}

		// Read content in chunks

		StringBuilder contentBuilder = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

			char[] buffer = new char[8192]; // 8KB buffer

			int bytesRead;

			while ((bytesRead = reader.read(buffer)) != -1) {

				contentBuilder.append(buffer, 0, bytesRead);

			}

		}

		String content = contentBuilder.toString();

		String normalizedContent = normalizeContent(content, langUpper);

		if (normalizedContent.isEmpty()) {

			throw new IllegalArgumentException("File content is empty after normalization for file: " + fileName);

		}

		CodeFile codeFile = new CodeFile();

		codeFile.setFileName(fileName);

		codeFile.setContent(normalizedContent);

		codeFile.setLanguage(langUpper);

		codeFile.setCreatedAt(LocalDateTime.now()); // Updated to use LocalDateTime

		Map<String, Integer> trigrams = generateTrigrams(normalizedContent, langUpper);

		if (trigrams.isEmpty()) {

			throw new IllegalArgumentException("No trigrams generated for file: " + fileName);

		}

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

	@Cacheable(value = "similarity", key = "{#fileId1, #fileId2}")

	public double calculateSimilarity(Long fileId1, Long fileId2) {

		logger.info("Comparing files: {} vs. {}", fileId1, fileId2);

		CodeFile file1 = codeFileRepository.findById(fileId1)

				.orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId1));

		CodeFile file2 = codeFileRepository.findById(fileId2)

				.orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId2));

		Map<String, Integer> vector1 = getTrigramVector(file1);

		Map<String, Integer> vector2 = getTrigramVector(file2);

		if (vector1 == null || vector1.isEmpty()) {

			throw new IllegalStateException("Trigram vector not found or empty for file ID: " + fileId1);

		}

		if (vector2 == null || vector2.isEmpty()) {

			throw new IllegalStateException("Trigram vector not found or empty for file ID: " + fileId2);

		}

		double similarity = cosineSimilarity.cosineSimilarity(vector1, vector2) * 100;

		double roundedSimilarity = Math.round(similarity * 100.0) / 100.0;

		logger.info("Similarity between files {} and {}: {}%", fileId1, fileId2, roundedSimilarity);

		return roundedSimilarity;

	}

	@Cacheable(value = "compareAll", key = "{#fileId, #pageable.pageNumber, #pageable.pageSize, #languageFilter, #minSimilarity}")
	public Page<SimilarityResult> compareAgainstAll(Long fileId, Pageable pageable, String languageFilter,
			Double minSimilarity) {

		logger.info(
				"Comparing file ID {} against paged files, page: {}, size: {}, languageFilter: {}, minSimilarity: {}",
				fileId, pageable.getPageNumber(), pageable.getPageSize(), languageFilter, minSimilarity);

		// Fetch target file and its trigram vector
		CodeFile targetFile = codeFileRepository.findById(fileId)
				.orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

		Map<String, Integer> targetVector = getTrigramVector(targetFile);
		if (targetVector == null || targetVector.isEmpty()) {
			throw new IllegalStateException("Trigram vector missing or empty for file ID: " + fileId);
		}

		// Normalize language filter
		String normalizedLanguageFilter = (languageFilter != null && !languageFilter.isEmpty())
				? languageFilter.toUpperCase()
				: null;

		final double effectiveMinSimilarity = (minSimilarity == null) ? 0.0 : minSimilarity;

		// Fetch paged files (only one page of results)
		Page<CodeFile> pageOfFiles = codeFileRepository.findByLanguage(normalizedLanguageFilter, pageable);

		List<SimilarityResult> results = pageOfFiles.getContent().stream()
				.filter(file -> !Objects.equals(file.getId(), fileId)).parallel().map(file -> {
					Map<String, Integer> otherVector = getTrigramVector(file);
					if (otherVector == null || otherVector.isEmpty())
						return null;

					double similarity = cosineSimilarity.cosineSimilarity(targetVector, otherVector) * 100;
					double rounded = Math.round(similarity * 100.0) / 100.0;

					return new SimilarityResult(file.getId(), file.getFileName(), file.getLanguage(), rounded);
				}).filter(Objects::nonNull).filter(result -> result.getSimilarity() >= effectiveMinSimilarity)
				.sorted(Comparator.comparingDouble(SimilarityResult::getSimilarity).reversed())
				.collect(Collectors.toList());

		logger.info("Returning {} results for page {} with pageSize {}", results.size(), pageable.getPageNumber(),
				pageable.getPageSize());

		return new PageImpl<>(results, pageable, pageOfFiles.getTotalElements());
	}

	public Map<String, Integer> generateTrigrams(String content, String language) {

		Map<String, Integer> trigrams = new HashMap<>();

		if (content == null || content.length() < 3) {

			logger.warn("Content too short to generate trigrams for language {}: {}", language, content);

			return trigrams;

		}

		content = content.substring(0, Math.min(content.length(), MAX_CONTENT_LENGTH));

		for (int i = 0; i < content.length() - 2; i++) {

			String trigram = content.substring(i, i + 3);

			trigrams.compute(trigram, (k, v) -> (v == null) ? 1 : v + 1);

		}

		logger.debug("Generated {} trigrams for language {}: {}", trigrams.size(), language, trigrams);

		return trigrams;

	}

	public String normalizeContent(String content, String language) {

		if (content == null || content.trim().isEmpty()) {

			logger.warn("Empty content provided for language: {}", language);

			return "";

		}

		switch (language.toUpperCase()) {

		case "PYTHON":

		case "RUBY":

			content = content.replaceAll("#.*?(?:\\n|$)", "");

			break;

		case "CPP":

		case "JAVA":

		case "JAVASCRIPT":

		case "TYPESCRIPT":

		case "GO":

			content = content.replaceAll("//.*?(?:\\n|$)", "").replaceAll("/\\*.*?\\*/", "");

			break;

		case "ADA":

			content = content.replaceAll("--.*?(?:\\n|$)", "");

			break;

		default:

			logger.warn("Unsupported language for normalization: {}", language);

			break;

		}

		content = content.replaceAll("\\s+", " ").toLowerCase();

		content = content.replaceAll("[{}();\\[\\]]", " ").replaceAll(

				"\\b(public|private|protected|static|final|abstract|class|def|function|void|int|float|double|str|string|bool|boolean|if|else|for|while|do|return|break|continue|try|catch|throw|new|self|this|super|package|import|include|using|namespace|struct|type)\\b",

				" ").replaceAll("\\b(true|false|null|none|nil)\\b", " ").replaceAll("=", " ")

				.replaceAll("[+\\-*/%><!&|]", " ")

				.replaceAll("\\b(print|println|cout|printf|puts|put|write|log|console)\\b", " ");

		content = content.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();

		content = content.replaceAll("[0-9]", " ").replaceAll("[^a-z_\\s]", " ");

		content = content.replaceAll("\\s+", " ");

		String normalizedContent = content.trim();

		logger.info("Normalized content for language {}: {}", language, normalizedContent);

		return normalizedContent;

	}

	public void deleteAllFiles() {

		logger.info("Deleting all files from the database...");

		codeFileRepository.deleteAll();

		logger.info("All files deleted successfully.");

	}

	@Cacheable(value = "trigrams", key = "#file.id")

	public Map<String, Integer> getTrigramVector(CodeFile file) {

		Map<String, Integer> trigramVector = file.Gettrigram_vector();

		if (trigramVector == null || trigramVector.isEmpty()) {

			logger.info("Generating trigram vector for file ID: {}", file.getId());

			trigramVector = generateTrigrams(file.getContent(), file.getLanguage());

			if (trigramVector.isEmpty()) {

				logger.warn("No trigrams generated for file ID: {}", file.getId());

			}

			file.Settrigram_vector(trigramVector);

			codeFileRepository.save(file);

		}

		return trigramVector;

	}

	public List<CodeFile> uploadBatchFiles(List<MultipartFile> files, String language) throws IOException {

		if (files == null || files.isEmpty()) {

			throw new IllegalArgumentException("No files provided for batch upload");

		}

		String langUpper = language.toUpperCase();

		List<CodeFile> codeFiles = new ArrayList<>();

		for (MultipartFile file : files) {

			validateFile(file, langUpper); // Your existing validation method

			try (InputStream inputStream = file.getInputStream()) {

				CodeFile codeFile = processFileStream(inputStream, file.getOriginalFilename(), langUpper);

				codeFiles.add(codeFile);

			} catch (IOException e) {

				logger.error("Failed to process file {}: {}", file.getOriginalFilename(), e.getMessage());

				throw e; // Or handle individually based on your needs

			}

		}

		List<CodeFile> savedFiles = codeFileRepository.saveAll(codeFiles);

		savedFiles.forEach(this::getTrigramVector); // Assuming this updates trigram_vector

		return savedFiles;

	}

	private CodeFile processFileStream(InputStream inputStream, String fileName, String language) throws IOException {

		long fileSize = inputStream.available();

		if (fileSize > 10 * 1024 * 1024) {

			throw new IllegalArgumentException("File size exceeds 10MB limit for file: " + fileName);

		}

		StringBuilder contentBuilder = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

			char[] buffer = new char[8192]; // 8KB buffer

			int bytesRead;

			while ((bytesRead = reader.read(buffer)) != -1) {

				contentBuilder.append(buffer, 0, bytesRead);

			}

		}

		String content = contentBuilder.toString();

		String normalizedContent = normalizeContent(content, language);

		if (normalizedContent.isEmpty()) {

			throw new IllegalArgumentException("File content is empty after normalization for file: " + fileName);

		}

		CodeFile codeFile = new CodeFile();

		codeFile.setFileName(fileName);

		codeFile.setContent(normalizedContent);

		codeFile.setLanguage(language);

		codeFile.setCreatedAt(LocalDateTime.now());

		return codeFile; // Trigram generation moved to post-save via getTrigramVector

	}

	@Transactional(readOnly = true)

	public List<SimilarityResult> compareBatchFiles(@NotNull Long targetFileId, @NotNull List<Long> fileIds,

			String languageFilter, Double minSimilarity) {

		if (fileIds == null || fileIds.isEmpty()) {

			throw new IllegalArgumentException("fileIds must not be null or empty");

		}

		if (fileIds.size() > MAX_FILE_IDS) {

			throw new IllegalArgumentException("Too many file IDs; max is " + MAX_FILE_IDS);

		}

		if (fileIds.contains(null)) {

			throw new IllegalArgumentException("fileIds cannot contain null values");

		}

		logger.info("Batch comparing file ID {} against files: {}", targetFileId, fileIds);

		CodeFile targetFile = codeFileRepository.findById(targetFileId)

				.orElseThrow(() -> new IllegalArgumentException("File not found: " + targetFileId));

		Map<String, Integer> targetVector = getTrigramVector(targetFile);

		if (targetVector == null || targetVector.isEmpty()) {

			throw new IllegalStateException("Trigram vector not found or empty for file ID: " + targetFileId);

		}

		String normalizedLanguageFilter = languageFilter != null ? languageFilter.toUpperCase() : null;

		if (normalizedLanguageFilter != null && !SUPPORTED_LANGUAGES.containsKey(normalizedLanguageFilter)) {

			throw new IllegalArgumentException("Unsupported language: " + languageFilter);

		}

		double effectiveMinSimilarity = minSimilarity != null ? minSimilarity : 0.0;

		List<CodeFile> filesToCompare = codeFileRepository.findAllById(fileIds).stream()

				.filter(file -> !file.getId().equals(targetFileId))

				.filter(file -> normalizedLanguageFilter == null || file.getLanguage().equals(normalizedLanguageFilter))

				.collect(Collectors.toList());

		return filesToCompare.stream().map(file -> {

			try {

				Map<String, Integer> otherVector = getTrigramVector(file);

				if (otherVector == null || otherVector.isEmpty()) {

					logger.error("Trigram vector not found or empty for file ID: {}", file.getId());

					return null;

				}

				double similarity = cosineSimilarity.cosineSimilarity(targetVector, otherVector) * 100;

				double roundedSimilarity = BigDecimal.valueOf(similarity).setScale(2, RoundingMode.HALF_UP)

						.doubleValue();

				logger.debug("Similarity between files {} and {} ({}): {}%", targetFileId, file.getId(),

						file.getLanguage(), roundedSimilarity);

				return new SimilarityResult(file.getId(), file.getFileName(), file.getLanguage(), roundedSimilarity);

			} catch (Exception e) {

				logger.error("Error processing file ID {}: {}, Cause: {}", file.getId(), e.getMessage(), e.getCause());

				return null;

			}

		}).filter(result -> result != null).filter(result -> result.getSimilarity() >= effectiveMinSimilarity)

				.sorted((r1, r2) -> Double.compare(r2.getSimilarity(), r1.getSimilarity()))

				.collect(Collectors.toList());

	}

	private void validateFile(MultipartFile file, String language) {

		if (file.isEmpty()) {

			throw new IllegalArgumentException("File is empty");

		}

		String fileName = file.getOriginalFilename();

		String[] extensions = SUPPORTED_LANGUAGES.get(language.toUpperCase());

		if (extensions == null || !Arrays.stream(extensions).anyMatch(fileName::endsWith)) {

			throw new IllegalArgumentException("Unsupported file extension for language: " + language);

		}

	}

}

//@Cacheable(value = "compareAll", key = "{#fileId, #pageable.pageNumber, #pageable.pageSize, #languageFilter, #minSimilarity}")
//
//public Page<SimilarityResult> compareAgainstAll(Long fileId, Pageable pageable, String languageFilter,
//
//		Double minSimilarity) {
//
//	logger.info(
//
//			"Comparing file ID {} against all other files with pagination, languageFilter: {}, minSimilarity: {}",
//
//			fileId, languageFilter, minSimilarity);
//
//	CodeFile targetFile = codeFileRepository.findById(fileId)
//
//			.orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
//
//	Map<String, Integer> targetVector = getTrigramVector(targetFile);
//
//	if (targetVector == null || targetVector.isEmpty()) {
//
//		throw new IllegalStateException("Trigram vector not found or empty for file ID: " + fileId);
//
//	}
//
//	String normalizedLanguageFilter = languageFilter;
//
//	if (languageFilter != null && !languageFilter.isEmpty()) {
//
//		String langUpper = languageFilter.toUpperCase();
//
//		if (!SUPPORTED_LANGUAGES.containsKey(langUpper)) {
//
//			throw new IllegalArgumentException("Unsupported language: " + languageFilter + ". Supported languages: "
//
//					+ SUPPORTED_LANGUAGES.keySet());
//
//		}
//
//		normalizedLanguageFilter = langUpper;
//
//	}
//
//	final double effectiveMinSimilarity = (minSimilarity == null) ? 0.0 : minSimilarity;
//
//	Page<CodeFile> allFiles = codeFileRepository.findByLanguage(normalizedLanguageFilter, pageable);
//
//	logger.info("Fetched {} files from database for comparison with file ID: {}", allFiles.getTotalElements(),
//
//			fileId);
//
//	allFiles.getContent().forEach(file -> logger.debug("Fetched file: ID {}, Name {}, Language {}", file.getId(),
//
//			file.getFileName(), file.getLanguage()));
//
//	List<SimilarityResult> results = allFiles.getContent().stream()
//
//			.peek(file -> logger.debug("Processing file: ID {}, Name {}, Language {}", file.getId(),
//
//					file.getFileName(), file.getLanguage()))
//
//			.filter(file -> !Objects.equals(file.getId(), fileId)).map(file -> {
//
//				Map<String, Integer> otherVector = getTrigramVector(file);
//
//				if (otherVector == null || otherVector.isEmpty()) {
//
//					logger.error("Trigram vector not found or empty for file ID: {}", file.getId());
//
//					throw new IllegalStateException("Trigram vector missing or empty for file ID: " + file.getId());
//
//				}
//
//				double similarity = cosineSimilarity.cosineSimilarity(targetVector, otherVector) * 100;
//
//				double roundedSimilarity = Math.round(similarity * 100.0) / 100.0;
//
//				logger.info("Similarity between files {} and {} ({}): {}%", fileId, file.getId(),
//
//						file.getLanguage(), roundedSimilarity);
//
//				return new SimilarityResult(file.getId(), file.getFileName(), file.getLanguage(),
//
//						roundedSimilarity);
//
//			}).filter(result -> result.getSimilarity() >= effectiveMinSimilarity)
//
//			.peek(result -> logger.debug("Result: File ID {}, Language {}, Similarity {}", result.getFileId(),
//
//					result.getLanguage(), result.getSimilarity()))
//
//			.sorted((r1, r2) -> Double.compare(r2.getSimilarity(), r1.getSimilarity()))
//
//			.collect(Collectors.toList());
//
//	logger.info("Returning {} results after filtering and sorting", results.size());
//
//	return new PageImpl<>(results, pageable, allFiles.getTotalElements());
//
//}

//@Cacheable(value = "all-files", key = "'all'")
//
//public List<CodeFile> GetAllFiles() {
//
//	return codeFileRepository.findAll();
//
//}

//public Page<CodeFile> GetAllFiles(Pageable pageable) {
//	return codeFileRepository.findAll(pageable);
//}