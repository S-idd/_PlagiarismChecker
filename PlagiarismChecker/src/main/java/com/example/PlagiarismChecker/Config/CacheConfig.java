//package com.example.PlagiarismChecker.config;
//
//import com.example.PlagiarismChecker.Service.CodeFileService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Sort;
//
//@Configuration
//public class CacheConfig {
//
//    @Autowired
//    private CodeFileService codeFileService;
//
//    @Bean
//    public ApplicationRunner cacheWarmer() {
//        return args -> {
//            PageRequest pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
//            codeFileService.findAllFiles(pageable);
//            System.out.println("Cache warmed for first page of code files");
//        };
//    }
//}