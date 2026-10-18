package hexlet.code;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void apiDocsShouldBeAvailableWithoutAuth() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/tasks']").exists())
                .andExpect(jsonPath("$.paths['/api/labels']").exists())
                .andExpect(jsonPath("$.paths['/api/login']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());
    }

    @Test
    void yamlSpecShouldBeAvailableWithoutAuth() throws Exception {
        mockMvc.perform(get("/api-docs.yaml")).andExpect(status().isOk());
    }

    @Test
    void swaggerUiRedirectShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/swagger-ui/index.html*"));
    }

    @Test
    void swaggerUiStaticShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void loginOperationShouldHaveNoBearerRequirement() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.paths['/api/login'].post.security").isEmpty());
    }

    @Test
    void tasksOperationShouldRequireBearer() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/tasks'].get.security").doesNotExist());
    }

    @Test
    void oldApiDocsPathShouldReturn404WithValidToken() throws Exception {
        mockMvc.perform(
                        get("/v3/api-docs")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(j -> j.subject("1"))))
                .andExpect(status().isNotFound());
    }
}
