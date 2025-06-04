package com.example.PlagiarismChecker.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.PlagiarismChecker.model.CodeFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public interface CodeFileRepository extends JpaRepository<CodeFile, Long> {
	@NonNull
	Page<CodeFile> findAll(@NonNull Pageable pageable);
}