//package com.example.PlagiarismChecker.__CodeFileRepositoryUnitTestes__;
//
//import com.example.PlagiarismChecker.model.CodeFile;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//import com.example.PlagiarismChecker.Repository.CodeFileRepository;
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//
//
//@DataJpaTest
//public class CodeFileRepositoryTestes {
//
//    @Autowired
//    private CodeFileRepository codeFileRepository;
//
//    @Autowired
//    private TestEntityManager entityManager;
//
//    private CodeFile javaFile1;
//    private CodeFile javaFile2;
//    private CodeFile pythonFile;
//
//    @BeforeEach
//    void setUp() {
//        // Initialize test data
//        javaFile1 = new CodeFile();
//        javaFile1.setFileName("test1.java");
//        javaFile1.setContent("public class Test1 {}");
//        javaFile1.setLanguage("JAVA");
//        javaFile1.setCreatedAt(LocalDateTime.now());
//        Map<String, Integer> trigram1 = new HashMap<>();
//        trigram1.put("pub", 1);
//        javaFile1.Settrigram_vector(trigram1);
//
//        javaFile2 = new CodeFile();
//        javaFile2.setFileName("test2.java");
//        javaFile2.setContent("public class Test2 {}");
//        javaFile2.setLanguage("JAVA");
//        javaFile2.setCreatedAt(LocalDateTime.now());
//        Map<String, Integer> trigram2 = new HashMap<>();
//        trigram2.put("cla", 1);
//        javaFile2.Settrigram_vector(trigram2);
//
//        pythonFile = new CodeFile();
//        pythonFile.setFileName("script.py");
//        pythonFile.setContent("print('Hello')");
//        pythonFile.setLanguage("PYTHON");
//        pythonFile.setCreatedAt(LocalDateTime.now());
//        Map<String, Integer> pythonTrigrams = new HashMap<>();
//        pythonTrigrams.put("pri", 1);
//        pythonFile.Settrigram_vector(pythonTrigrams);
//    }
//
//    @Test
//    void findByLanguage_WhenLanguageIsJava_ReturnsPaginatedJavaFiles() {
//        // Arrange
//        entityManager.persist(javaFile1);
//        entityManager.persist(javaFile2);
//        entityManager.persist(pythonFile);
//        entityManager.flush();
//        Pageable pageable = PageRequest.of(0, 1); // First page, 1 item per page
//
//        // Act
//        Page<CodeFile> result = codeFileRepository.findByLanguage("JAVA", pageable);
//
//        // Assert
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getTotalElements()).isEqualTo(2);
//        assertThat(result.getTotalPages()).isEqualTo(2);
//        assertThat(result.getContent().get(0).getLanguage()).isEqualTo("JAVA");
//    }
//
//    @Test
//    void findByLanguage_WhenLanguageIsNull_ReturnsAllFilesPaginated() {
//        // Arrange
//        entityManager.persist(javaFile1);
//        entityManager.persist(javaFile2);
//        entityManager.persist(pythonFile);
//        entityManager.flush();
//        Pageable pageable = PageRequest.of(0, 2); // First page, 2 items per page
//
//        // Act
//        Page<CodeFile> result = codeFileRepository.findByLanguage(null, pageable);
//
//        // Assert
//        assertThat(result.getContent()).hasSize(2);
//        assertThat(result.getTotalElements()).isEqualTo(3);
//        assertThat(result.getTotalPages()).isEqualTo(2);
//        assertThat(result.getContent()).extracting("fileName").containsAnyOf("test1.java", "test2.java", "script.py");
//    }
//
//    @Test
//    void findByLanguage_WhenNoFilesMatchLanguage_ReturnsEmptyPage() {
//        // Arrange
//        entityManager.persist(javaFile1);
//        entityManager.flush();
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Act
//        Page<CodeFile> result = codeFileRepository.findByLanguage("RUBY", pageable);
//
//        // Assert
//        assertThat(result.getContent()).isEmpty();
//        assertThat(result.getTotalElements()).isEqualTo(0);
//        assertThat(result.getTotalPages()).isEqualTo(0);
//    }
//
//    @Test
//    void findAll_WhenFilesExist_ReturnsAllFiles() {
//        // Arrange
//        entityManager.persist(javaFile1);
//        entityManager.persist(pythonFile);
//        entityManager.flush();
//
//        // Act
//        List<CodeFile> result = codeFileRepository.findAll();
//
//        // Assert
//        assertThat(result).hasSize(2);
//        assertThat(result).extracting("fileName").containsExactlyInAnyOrder("test1.java", "script.py");
//    }
//
//    @Test
//    void findAll_WhenNoFilesExist_ReturnsEmptyList() {
//        // Act
//        List<CodeFile> result = codeFileRepository.findAll();
//
//        // Assert
//        assertThat(result).isEmpty();
//    }
//
//    @Test
//    void findById_WhenFileExists_ReturnsFile() {
//        // Arrange
//        Long id = entityManager.persistAndGetId(javaFile1, Long.class);
//        entityManager.flush();
//
//        // Act
//        Optional<CodeFile> result = codeFileRepository.findById(id);
//
//        // Assert
//        assertThat(result).isPresent();
//        assertThat(result.get().getFileName()).isEqualTo("test1.java");
//    }
//
//    @Test
//    void findById_WhenFileDoesNotExist_ReturnsEmptyOptional() {
//        // Act
//        Optional<CodeFile> result = codeFileRepository.findById(999L);
//
//        // Assert
//        assertThat(result).isEmpty();
//    }
//
//    @Test
//    void save_WhenValidFile_PersistsFile() {
//        // Act
//        CodeFile savedFile = codeFileRepository.save(javaFile1);
//
//        // Assert
//        assertThat(savedFile.getId()).isNotNull();
//        CodeFile foundFile = entityManager.find(CodeFile.class, savedFile.getId());
//        assertThat(foundFile.getFileName()).isEqualTo("test1.java");
//        assertThat(foundFile.getLanguage()).isEqualTo("JAVA");
//    }
//
//    @Test
//    void deleteAll_WhenFilesExist_RemovesAllFiles() {
//        // Arrange
//        entityManager.persist(javaFile1);
//        entityManager.persist(pythonFile);
//        entityManager.flush();
//
//        // Act
//        codeFileRepository.deleteAll();
//
//        // Assert
//        List<CodeFile> result = codeFileRepository.findAll();
//        assertThat(result).isEmpty();
//    }
//}
