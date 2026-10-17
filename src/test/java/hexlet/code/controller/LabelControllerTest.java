package hexlet.code.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hexlet.code.TestcontainersConfig;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import java.time.LocalDateTime;
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
class LabelControllerTest {

    private static final String URL = "/api/labels";

    @Autowired private MockMvc mockMvc;
    @Autowired private LabelRepository labelRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskStatusRepository taskStatusRepository;

    // ============================ POST ============================

    @Test
    void testCreate() throws Exception {
        var body =
                """
                { "name": "backend" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("backend"))
                .andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}")));
    }

    @Test
    void testCreateWithoutTokenReturns401() throws Exception {
        var body =
                """
                { "name": "backend" }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateWithBlankNameReturns400() throws Exception {
        var body =
                """
                { "name": "   " }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void testCreateWithTooShortNameReturns400() throws Exception {
        var body =
                """
                { "name": "ab" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateStripsNameBeforeLengthCheck() throws Exception {
        // после strip длина 2, значит должен быть 400
        var body =
                """
                { "name": "  ab  " }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithMaxLengthName() throws Exception {
        var name = "a".repeat(1000);
        var body =
                """
                { "name": "%s" }
                """
                        .formatted(name);

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    void testCreateWithTooLongNameReturns400() throws Exception {
        var name = "a".repeat(1001);
        var body =
                """
                { "name": "%s" }
                """
                        .formatted(name);

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateTrimsName() throws Exception {
        var body =
                """
                { "name": "  custom-trim \s" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("custom-trim"));
    }

    @Test
    void testCreateDuplicateNameReturns409() throws Exception {
        persistLabel("duplicated");

        var body =
                """
                { "name": "duplicated" }
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
    void testGetAllReturnsTotalCount() throws Exception {
        var before = labelRepository.count();
        persistLabel("first");
        persistLabel("second");

        mockMvc.perform(get(URL).with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", String.valueOf(before + 2)))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(before + 2));
    }

    @Test
    void testGetAllWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void testGetById() throws Exception {
        var label = persistLabel("single");

        mockMvc.perform(get(URL + "/" + label.getId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(label.getId()))
                .andExpect(jsonPath("$.name").value("single"));
    }

    @Test
    void testGetByIdWithoutTokenReturns401() throws Exception {
        var label = persistLabel("single");
        mockMvc.perform(get(URL + "/" + label.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void testGetNonExistentReturns404() throws Exception {
        mockMvc.perform(get(URL + "/999999").with(auth())).andExpect(status().isNotFound());
    }

    // ============================ PUT ============================

    @Test
    void testUpdateName() throws Exception {
        var label = persistLabel("old");

        mockMvc.perform(
                        put(URL + "/" + label.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": "renamed" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("renamed"));

        var after = labelRepository.findById(label.getId()).orElseThrow();
        assertThat(after.getName()).isEqualTo("renamed");
    }

    @Test
    void testUpdateTrimsName() throws Exception {
        var label = persistLabel("old");

        mockMvc.perform(
                        put(URL + "/" + label.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": "  renamed  " }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("renamed"));
    }

    @Test
    void testUpdateSameNameIsNoop() throws Exception {
        var label = persistLabel("same");

        mockMvc.perform(
                        put(URL + "/" + label.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": "same" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("same"));
    }

    @Test
    void testUpdateToDuplicateNameReturns409() throws Exception {
        persistLabel("taken");
        var label = persistLabel("mine");

        mockMvc.perform(
                        put(URL + "/" + label.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": "taken" }"""))
                .andExpect(status().isConflict());
    }

    @Test
    void testUpdateWithTooShortNameReturns400() throws Exception {
        var label = persistLabel("valid");

        mockMvc.perform(
                        put(URL + "/" + label.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": "ab" }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateWithBlankNameReturns400() throws Exception {
        var label = persistLabel("valid");

        mockMvc.perform(
                        put(URL + "/" + label.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": "   " }"""))
                .andExpect(status().isBadRequest());
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
    void testUpdateWithoutTokenReturns401() throws Exception {
        var label = persistLabel("valid");

        mockMvc.perform(
                        put(URL + "/" + label.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": "X" }"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpdateNameToNullReturns400() throws Exception {
        var label = persistLabel("valid");

        mockMvc.perform(
                        put(URL + "/" + label.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "name": null }"""))
                .andExpect(status().isBadRequest());

        var after = labelRepository.findById(label.getId()).orElseThrow();
        assertThat(after.getName()).isEqualTo("valid");
    }

    @Test
    void testUpdateIgnoresUnknownFields() throws Exception {
        var label = persistLabel("keep");

        mockMvc.perform(
                        put(URL + "/" + label.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "foo": 1 }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("keep"));

        var after = labelRepository.findById(label.getId()).orElseThrow();
        assertThat(after.getName()).isEqualTo("keep");
    }

    // ============================ DELETE ============================

    @Test
    void testDelete() throws Exception {
        var label = persistLabel("to-delete");

        mockMvc.perform(delete(URL + "/" + label.getId()).with(auth()))
                .andExpect(status().isNoContent());

        assertThat(labelRepository.findById(label.getId())).isEmpty();
    }

    @Test
    void testDeleteNonExistentReturns404() throws Exception {
        mockMvc.perform(delete(URL + "/999999").with(auth())).andExpect(status().isNotFound());
    }

    @Test
    void testDeleteWithoutTokenReturns401() throws Exception {
        var label = persistLabel("to-delete");
        mockMvc.perform(delete(URL + "/" + label.getId())).andExpect(status().isUnauthorized());
        assertThat(labelRepository.findById(label.getId())).isPresent();
    }

    @Test
    void testDeleteUsedLabelReturns409() throws Exception {
        var label = persistLabel("in-use");
        persistTaskWithLabel(label);

        mockMvc.perform(delete(URL + "/" + label.getId()).with(auth()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("used by tasks")));

        assertThat(labelRepository.findById(label.getId())).isPresent();
    }

    @Test
    void testDeleteAfterUnlinkReturns204() throws Exception {
        var label = persistLabel("in-use");
        var task = persistTaskWithLabel(label);

        mockMvc.perform(delete("/api/tasks/" + task.getId()).with(auth()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete(URL + "/" + label.getId()).with(auth()))
                .andExpect(status().isNoContent());

        assertThat(labelRepository.findById(label.getId())).isEmpty();
    }

    // ============================ helpers ============================

    private Label persistLabel(String name) {
        var label = new Label();
        label.setName(name);
        return labelRepository.saveAndFlush(label);
    }

    private Task persistTaskWithLabel(Label label) {
        var status = taskStatusRepository.findBySlug("draft").orElseThrow();
        var task = new Task();
        task.setName("Task with label");
        task.setTaskStatus(status);
        task.setCreatedAt(LocalDateTime.now());
        task.getLabels().add(label);
        return taskRepository.saveAndFlush(task);
    }

    private RequestPostProcessor auth() {
        return jwt().jwt(j -> j.subject("1"));
    }
}
