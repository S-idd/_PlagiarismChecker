package com.example.PlagiarismChecker.Repository;
import com.example.PlagiarismChecker.model.CodeFile;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

public interface CodeFileRepository extends JpaRepository<CodeFile, Long> {
    @NonNull
    Page<CodeFile> findAll(@NonNull Pageable pageable);
    
    @Query("SELECT c FROM CodeFile c WHERE (:language IS NULL OR c.language = :language)")
    Page<CodeFile> findByLanguage(@Param("language") String language, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM CodeFile c WHERE (:language IS NULL OR c.language = :language) AND c.id != :fileId")
    long countByLanguageExcludingFileId(@Param("language") String language, @Param("fileId") Long fileId);
    
    List<CodeFile> findAllByIdInAndLanguage(List<Long> ids, String language);
}