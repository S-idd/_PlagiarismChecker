package com.example.PlagiarismChecker.__CodeFileConfigUnitTestes__;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void testUserDetailsServiceBean() {
        assertNotNull(userDetailsService, "UserDetailsService bean should be created");
        UserDetails user = userDetailsService.loadUserByUsername("admin");
        assertNotNull(user, "Admin user should exist");
        assertEquals("admin", user.getUsername(), "Username should be admin");
        assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
                "User should have ADMIN role");
    }

    @Test
    void testUserDetailsServiceInvalidUser() {
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("invalid"),
                "Invalid user should throw UsernameNotFoundException");
    }

    @Test
    @WithAnonymousUser
    void testUnauthenticatedAccessToPublicEndpoint() throws Exception {
        mockMvc.perform(get("/api/public"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testUnauthenticatedAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/code-files/files"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Realm\""));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAuthenticatedAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/code-files/files"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessProtectedEndpointWithValidCredentials() throws Exception {
        mockMvc.perform(get("/api/code-files/files")
                        .with(httpBasic("admin", "root")))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessProtectedEndpointWithInvalidCredentials() throws Exception {
        mockMvc.perform(get("/api/code-files/files")
                        .with(httpBasic("admin", "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCsrfDisabledForPostRequest() throws Exception {
        mockMvc.perform(post("/api/code-files/upload")
                        .with(httpBasic("admin", "root"))
                        .contentType("multipart/form-data")
                        .param("language", "java"))
                .andExpect(status().isBadRequest()); // Expect 400 due to missing file, not 403 CSRF
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAccessBatchCompareWithAdminRole() throws Exception {
        mockMvc.perform(post("/api/code-files/compare/batch")
                        .contentType("application/json")
                        .content("{\"targetFileId\":1,\"fileIds\":[2,3],\"languageFilter\":\"java\",\"minSimilarity\":0.5}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testAccessBatchCompareWithoutAdminRole() throws Exception {
        mockMvc.perform(post("/api/code-files/compare/batch")
                        .contentType("application/json")
                        .content("{\"targetFileId\":1,\"fileIds\":[2,3],\"languageFilter\":\"java\",\"minSimilarity\":0.5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOptionsRequestForCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/code-files/files")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"));
    }
}