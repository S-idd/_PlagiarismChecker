package com.example.PlagiarismChecker.__CodeFileServiceUnitTestes__;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.PlagiarismChecker.Service.CodeFileService;

public class CodeFileServiceTest {
	@Autowired
	private CodeFileService codeFileService;

	@Test
	void testNormalizeContent() {
		String javaCode = "// Comment\npublic class Test { }";
		String normalized = codeFileService.normalizeContent(javaCode, "JAVA");
		assertFalse(normalized.contains("//"));
		assertTrue(normalized.contains("public class test"));
	}

	@Test
	void testValidExtension() {
		assertTrue(codeFileService.isValidExtension("test.java", "JAVA"));
		assertTrue(codeFileService.isValidExtension("test.cpp", "CPP"));
		assertFalse(codeFileService.isValidExtension("test.txt", "JAVA"));
	}
}
