//package com.example.PlagiarismChecker.__CodeFileModelUnitTestes__;
//
//import jakarta.validation.ConstraintViolation;
//import jakarta.validation.Validation;
//import jakarta.validation.Validator;
//import jakarta.validation.ValidatorFactory;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import com.example.PlagiarismChecker.model.CodeFile;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//public class CodeFileModelTestes {
//
//    private static Validator validator;
//    private CodeFile codeFile;
//
//    @BeforeAll
//    static void setUpValidator() {
//        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//        validator = factory.getValidator();
//    }
//
//    @BeforeEach
//    void setUp() {
//        codeFile = new CodeFile();
//        codeFile.setFileName("test.java");
//        codeFile.setContent("public class Test {}");
//        codeFile.setLanguage("JAVA");
//        codeFile.setCreatedAt(LocalDateTime.of(2025, 6, 7, 10, 0));
//        Map<String, Integer> trigrams = new HashMap<>();
//        trigrams.put("pub", 1);
//        trigrams.put("lic", 1);
//        codeFile.Settrigram_vector(trigrams);
//    }
//
//    @Test
//    void constructor_Default_Success() {
//        // Arrange
//        CodeFile emptyCodeFile = new CodeFile();
//
//        // Assert
//        assertThat(emptyCodeFile.getId()).isNull();
//        assertThat(emptyCodeFile.getFileName()).isNull();
//        assertThat(emptyCodeFile.getContent()).isNull();
//        assertThat(emptyCodeFile.getLanguage()).isNull();
//        assertThat(emptyCodeFile.getCreatedAt()).isNull();
//        assertThat(emptyCodeFile.Gettrigram_vector()).isNull();
//    }
//
//    @Test
//    void gettersAndSetters_Success() {
//        // Assert getters
//        assertThat(codeFile.getId()).isNull(); // ID is not set manually
//        assertThat(codeFile.getFileName()).isEqualTo("test.java");
//        assertThat(codeFile.getContent()).isEqualTo("public class Test {}");
//        assertThat(codeFile.getLanguage()).isEqualTo("JAVA");
//        assertThat(codeFile.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 6, 7, 10, 0));
//        assertThat(codeFile.Gettrigram_vector()).containsEntry("pub", 1).containsEntry("lic", 1);
//
//        // Update with setters
//        codeFile.setId(1L);
//        codeFile.setFileName("new.java");
//        codeFile.setContent("print('Hello')");
//        codeFile.setLanguage("PYTHON");
//        codeFile.setCreatedAt(LocalDateTime.of(2025, 6, 8, 12, 0));
//        Map<String, Integer> newTrigrams = new HashMap<>();
//        newTrigrams.put("pri", 1);
//        newTrigrams.put("int", 1);
//        codeFile.Settrigram_vector(newTrigrams);
//
//        // Assert updated values
//        assertThat(codeFile.getId()).isEqualTo(1L);
//        assertThat(codeFile.getFileName()).isEqualTo("new.java");
//        assertThat(codeFile.getContent()).isEqualTo("print('Hello')");
//        assertThat(codeFile.getLanguage()).isEqualTo("PYTHON");
//        assertThat(codeFile.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 6, 8, 12, 0));
//        assertThat(codeFile.Gettrigram_vector()).containsEntry("pri", 1).containsEntry("int", 1);
//    }
//
//    @Test
//    void validation_Success() {
//        // Act
//        Set<ConstraintViolation<CodeFile>> violations = validator.validate(codeFile);
//
//        // Assert
//        assertThat(violations).isEmpty();
//    }
//
//    @Test
//    void validation_Fail_NullFileName() {
//        // Arrange
//        codeFile.setFileName(null);
//
//        // Act
//        Set<ConstraintViolation<CodeFile>> violations = validator.validate(codeFile);
//
//        // Assert
//        assertThat(violations).hasSize(1);
//        assertThat(violations.iterator().next().getMessage()).isEqualTo("File name cannot be blank");
//    }
//
//    @Test
//    void validation_Fail_BlankFileName() {
//        // Arrange
//        codeFile.setFileName("");
//
//        // Act
//        Set<ConstraintViolation<CodeFile>> violations = validator.validate(codeFile);
//
//        // Assert
//        assertThat(violations).hasSize(1);
//        assertThat(violations.iterator().next().getMessage()).isEqualTo("File name cannot be blank");
//    }
//
//    @Test
//    void validation_Fail_NullContent() {
//        // Arrange
//        codeFile.setContent(null);
//
//        // Act
//        Set<ConstraintViolation<CodeFile>> violations = validator.validate(codeFile);
//
//        // Assert
//        assertThat(violations).hasSize(1);
//        assertThat(violations.iterator().next().getMessage()).isEqualTo("Content cannot be blank");
//    }
//
//    @Test
//    void validation_Fail_BlankContent() {
//        // Arrange
//        codeFile.setContent("");
//
//        // Act
//        Set<ConstraintViolation<CodeFile>> violations = validator.validate(codeFile);
//
//        // Assert
//        assertThat(violations).hasSize(1);
//        assertThat(violations.iterator().next().getMessage()).isEqualTo("Content cannot be blank");
//    }
//
//    @Test
//    void validation_Fail_NullLanguage() {
//        // Arrange
//        codeFile.setLanguage(null);
//
//        // Act
//        Set<ConstraintViolation<CodeFile>> violations = validator.validate(codeFile);
//
//        // Assert
//        assertThat(violations).hasSize(1);
//        assertThat(violations.iterator().next().getMessage()).isEqualTo("Language cannot be blank");
//    }
//
//    @Test
//    void validation_Fail_BlankLanguage() {
//        // Arrange
//        codeFile.setLanguage("");
//
//        // Act
//        Set<ConstraintViolation<CodeFile>> violations = validator.validate(codeFile);
//
//        // Assert
//        assertThat(violations).hasSize(1);
//        assertThat(violations.iterator().next().getMessage()).isEqualTo("Language cannot be blank");
//    }
//
//    @Test
//    void validation_Success_NullTrigramVector() {
//        // Arrange
//        codeFile.Settrigram_vector(null);
//
//        // Act
//        Set<ConstraintViolation<CodeFile>> violations = validator.validate(codeFile);
//
//        // Assert
//        assertThat(violations).isEmpty(); // trigram_vector is not annotated with constraints
//    }
//
//    @Test
//    void toString_Success() {
//        // Act
//        String toString = codeFile.toString();
//
//        // Assert
//        assertThat(toString).contains("CodeFile@"); // Default toString includes class name and hash
//        assertThat(toString).doesNotContain("id"); // No custom toString, so fields not included
//    }
//
//    @Test
//    void equals_SameObject() {
//        // Assert
//        assertThat(codeFile).isEqualTo(codeFile);
//    }
//
//    @Test
//    void equals_DifferentObject_SameFields() {
//        // Arrange
//        CodeFile other = new CodeFile();
//        other.setFileName("test.java");
//        other.setContent("public class Test {}");
//        other.setLanguage("JAVA");
//        other.setCreatedAt(LocalDateTime.of(2025, 6, 7, 10, 0));
//        Map<String, Integer> trigrams = new HashMap<>();
//        trigrams.put("pub", 1);
//        trigrams.put("lic", 1);
//        other.Settrigram_vector(trigrams);
//
//        // Assert
//        assertThat(codeFile).isNotEqualTo(other); // Default equals uses reference equality
//    }
//
//    @Test
//    void equals_Null() {
//        // Assert
//        assertThat(codeFile).isNotEqualTo(null);
//    }
//
//    @Test
//    void hashCode_DifferentObjects() {
//        // Arrange
//        CodeFile other = new CodeFile();
//        other.setFileName("test.java");
//        other.setContent("public class Test {}");
//        other.setLanguage("JAVA");
//        other.setCreatedAt(LocalDateTime.of(2025, 6, 7, 10, 0));
//        Map<String, Integer> trigrams = new HashMap<>();
//        trigrams.put("pub", 1);
//        trigrams.put("lic", 1);
//        other.Settrigram_vector(trigrams);
//
//        // Assert
//        assertThat(codeFile.hashCode()).isNotEqualTo(other.hashCode()); // Default hashCode uses object identity
//    }
//}