package com.example.PlagiarismChecker.Repository;

import com.example.PlagiarismChecker.DTO.CodeFileSummary;
import com.example.PlagiarismChecker.model.CodeFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

public interface CodeFileRepository extends JpaRepository<CodeFile, Long> {

	@NonNull
	Page<CodeFile> findAll(@NonNull Pageable pageable);

	@Query("SELECT c FROM CodeFile c WHERE (:language IS NULL OR c.language = :language)")
	Page<CodeFile> findByLanguage(@Param("language") String language, Pageable pageable);

	@Query("SELECT c FROM CodeFile c WHERE (:language IS NULL OR c.language = :language)")
	List<CodeFile> findByLanguageUnpaged(@Param("language") String language);

	@Query("SELECT COUNT(c) FROM CodeFile c WHERE (:language IS NULL OR c.language = :language) AND c.id != :fileId")
	long countByLanguageExcludingFileId(@Param("language") String language, @Param("fileId") Long fileId);

	List<CodeFile> findAllByIdInAndLanguage(List<Long> ids, String language);

	@Query("SELECT c FROM CodeFile c WHERE c.content LIKE %:keyword%")
	List<CodeFile> findByContentContaining(@Param("keyword") String keyword);

	@Query("SELECT c.id AS id, c.fileName AS fileName, c.language AS language FROM CodeFile c")
	Page<CodeFileSummary> findAllBy(Pageable pageable);

	boolean existsByContentHash(String contentHash);

	/**
	 * Find files where trigrams haven't been generated yet Used by async trigram
	 * generation job
	 */
	@Query("SELECT c FROM CodeFile c WHERE c.trigramsGenerated = false")
	List<CodeFile> findFilesWithoutTrigrams();

	/**
	 * Bulk update trigrams generated flag
	 */
	@Modifying
	@Query("UPDATE CodeFile c SET c.trigramsGenerated = true WHERE c.id IN :ids")
	void markTrigramsGenerated(@Param("ids") List<Long> ids);

	/**
	 * Find files by language without trigrams
	 */
	@Query("SELECT c FROM CodeFile c WHERE c.language = :language AND c.trigramsGenerated = false")
	List<CodeFile> findByLanguageWithoutTrigrams(@Param("language") String language);

	/**
	 * Count files without trigrams
	 */
	@Query("SELECT COUNT(c) FROM CodeFile c WHERE c.trigramsGenerated = false")
	long countFilesWithoutTrigrams();

	@Query("""
			    SELECT c.contentHash, c
			    FROM CodeFile c
			    WHERE c.contentHash IN :hashes
			""")
	List<Object[]> findByContentHashes(@Param("hashes") List<String> hashes);
	
	default Map<String, CodeFile> findExistingByHashes(List<String> hashes) {
	    return findByContentHashes(hashes).stream()
	        .collect(Collectors.toMap(
	            row -> (String) row[0],
	            row -> (CodeFile) row[1]
	        ));
	}


}