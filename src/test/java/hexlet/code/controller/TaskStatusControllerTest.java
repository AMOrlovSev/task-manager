package hexlet.code.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hexlet.code.TestcontainersConfig;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.TaskStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class TaskStatusControllerTest {

    private static final String URL = "/api/task_statuses";

    @Autowired private MockMvc mockMvc;
    @Autowired private TaskStatusRepository repository;

    // ============================ POST ============================

    @Test
    void testCreate() throws Exception {
        var body =
                """
                { "name": "New", "slug": "new" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.slug").value("new"))
                .andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}")));
    }

    @Test
    void testCreateWithoutTokenReturns401() throws Exception {
        var body =
                """
                { "name": "New", "slug": "new" }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateWithBlankNameReturns400() throws Exception {
        var body =
                """
                { "name": "", "slug": "new" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithBlankSlugReturns400() throws Exception {
        var body =
                """
                { "name": "New", "slug": "" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithMissingNameReturns400() throws Exception {
        var body =
                """
                { "slug": "new" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateDuplicateSlugReturns409() throws Exception {
        persist("First", "unique-slug");

        var body =
                """
                { "name": "Second", "slug": "unique-slug" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void testCreateDuplicateNameReturns409() throws Exception {
        persist("Taken", "first-slug");

        var body =
                """
                { "name": "Taken", "slug": "second-slug" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict());
    }

    // ============================ GET ============================

    @Test
    void testGetByIdWithoutTokenIsAllowed() throws Exception {
        var status = persist("TestDraft", "draft-1");

        mockMvc.perform(get(URL + "/" + status.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(status.getId()))
                .andExpect(jsonPath("$.name").value("TestDraft"))
                .andExpect(jsonPath("$.slug").value("draft-1"));
    }

    @Test
    void testGetAllWithoutTokenIsAllowed() throws Exception {
        var before = repository.count();
        persist("TestDraft", "draft-x");
        persist("TestToReview", "to_review-x");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", String.valueOf(before + 2)))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(before + 2));
    }

    @Test
    void testGetNonExistentReturns404() throws Exception {
        mockMvc.perform(get(URL + "/999999")).andExpect(status().isNotFound());
    }

    // ============================ PUT ============================

    @Test
    void testUpdateName() throws Exception {
        var status = persist("TestDraft", "draft-y");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "name": "newStatus" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("newStatus"))
                .andExpect(jsonPath("$.slug").value("draft-y"));

        var after = repository.findById(status.getId()).orElseThrow();
        assertThat(after.getName()).isEqualTo("newStatus");
        assertThat(after.getSlug()).isEqualTo("draft-y");
    }

    @Test
    void testUpdateSlug() throws Exception {
        var status = persist("TestDraft", "draft-z");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "slug": "renamed" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TestDraft"))
                .andExpect(jsonPath("$.slug").value("renamed"));
    }

    @Test
    void testUpdateEmptyBodyKeepsEverything() throws Exception {
        var status = persist("TestDraft", "draft-keep");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TestDraft"))
                .andExpect(jsonPath("$.slug").value("draft-keep"));
    }

    @Test
    void testUpdateWithBlankNameReturns400() throws Exception {
        var status = persist("TestDraft", "draft-blank");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "name": "" }"""))
                .andExpect(status().isBadRequest());

        var after = repository.findById(status.getId()).orElseThrow();
        assertThat(after.getName()).isEqualTo("TestDraft");
    }

    @Test
    void testUpdateToDuplicateSlugReturns409() throws Exception {
        persist("Other", "taken-slug");
        var status = persist("TestDraft", "draft-dup");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "slug": "taken-slug" }"""))
                .andExpect(status().isConflict());
    }

    @Test
    void testUpdateToDuplicateNameReturns409() throws Exception {
        persist("Taken", "unique-slug-2");
        var status = persist("TestDraft", "draft-dup-2");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "name": "Taken" }"""))
                .andExpect(status().isConflict());
    }

    @Test
    void testUpdateSameSlugDifferentIsNoop() throws Exception {
        var status = persist("TestDraft", "draft-noop");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "slug": "draft-noop" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("draft-noop"));
    }

    @Test
    void testUpdateNonExistentReturns404() throws Exception {
        mockMvc.perform(
                        put(URL + "/999999")
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "name": "X" }"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateArrayBodyReturns400() throws Exception {
        var status = persist("TestDraft", "draft-arr");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[1,2,3]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateWithoutTokenReturns401() throws Exception {
        var status = persist("TestDraft", "draft-noauth");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "name": "X" }"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdateNameToNullReturns400() throws Exception {
        var status = persist("TestDraft", "draft-null-name");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": null }"""))
                .andExpect(status().isBadRequest());

        var after = repository.findById(status.getId()).orElseThrow();
        assertThat(after.getName()).isEqualTo("TestDraft");
    }

    @Test
    void testUpdateSlugToNullReturns400() throws Exception {
        var status = persist("TestDraft", "draft-null-slug");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "slug": null }"""))
                .andExpect(status().isBadRequest());

        var after = repository.findById(status.getId()).orElseThrow();
        assertThat(after.getSlug()).isEqualTo("draft-null-slug");
    }

    @Test
    void testUpdateNameToBlankReturns400() throws Exception {
        var status = persist("TestDraft", "draft-blank-name");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": "   " }"""))
                .andExpect(status().isBadRequest());

        var after = repository.findById(status.getId()).orElseThrow();
        assertThat(after.getName()).isEqualTo("TestDraft");
    }

    @Test
    void testUpdateSlugToBlankReturns400() throws Exception {
        var status = persist("TestDraft", "draft-blank-slug");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "slug": "   " }"""))
                .andExpect(status().isBadRequest());

        var after = repository.findById(status.getId()).orElseThrow();
        assertThat(after.getSlug()).isEqualTo("draft-blank-slug");
    }

    @Test
    void testCreateTrimsNameAndSlug() throws Exception {
        var body =
                """
                { "name": "  Padded  ", "slug": "  padded  " }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Padded"))
                .andExpect(jsonPath("$.slug").value("padded"));
    }

    @Test
    void testUpdateTrimsNameAndSlug() throws Exception {
        var status = persist("TestDraft", "draft-trim");

        mockMvc.perform(
                        put(URL + "/" + status.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": "  Renamed  ", "slug": "  renamed  " }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.slug").value("renamed"));
    }

    // ============================ DELETE ============================

    @Test
    void testDelete() throws Exception {
        var status = persist("TestDraft", "draft-del");

        mockMvc.perform(delete(URL + "/" + status.getId()).with(auth()))
                .andExpect(status().isNoContent());

        assertThat(repository.findById(status.getId())).isEmpty();
    }

    @Test
    void testDeleteNonExistentReturns404() throws Exception {
        mockMvc.perform(delete(URL + "/999999").with(auth())).andExpect(status().isNotFound());
    }

    @Test
    void testDeleteWithoutTokenReturns401() throws Exception {
        var status = persist("TestDraft", "draft-del-noauth");

        mockMvc.perform(delete(URL + "/" + status.getId())).andExpect(status().isUnauthorized());

        assertThat(repository.findById(status.getId())).isPresent();
    }

    // ============================ helpers ============================

    private TaskStatus persist(String name, String slug) {
        var status = new TaskStatus();
        status.setName(name);
        status.setSlug(slug);
        return repository.save(status);
    }

    private RequestPostProcessor auth() {
        return jwt().jwt(j -> j.subject("1"));
    }
}
