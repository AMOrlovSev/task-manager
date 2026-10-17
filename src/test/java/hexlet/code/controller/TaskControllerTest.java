package hexlet.code.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class TaskControllerTest {

    private static final String URL = "/api/tasks";

    @Autowired private MockMvc mockMvc;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskStatusRepository taskStatusRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LabelRepository labelRepository;

    // ============================ GET list ============================

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsArrayAndTotalCount() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        persistTask("Task1", "content1", null, draft(), user);
        persistTask("Task2", null, 5, draft(), null);

        mockMvc.perform(get(URL).with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").value("Task1"))
                .andExpect(jsonPath("$[0].content").value("content1"))
                .andExpect(jsonPath("$[0].status").value("draft"))
                .andExpect(jsonPath("$[0].assignee_id").value(user.getId()))
                .andExpect(jsonPath("$[0].createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}")))
                .andExpect(jsonPath("$[1].index").value(5))
                .andExpect(jsonPath("$[1].assignee_id").value(nullValue()));
    }

    @Test
    void listSortedById() throws Exception {
        persistTask("A", null, null, draft(), null);
        persistTask("B", null, null, draft(), null);

        mockMvc.perform(get(URL).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("A"))
                .andExpect(jsonPath("$[1].title").value("B"));
    }

    // ============================ GET by id ============================

    @Test
    void getByIdReturnsTask() throws Exception {
        var t = persistTask("Task", "c", 3, draft(), null);

        mockMvc.perform(get(URL + "/" + t.getId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(t.getId()))
                .andExpect(jsonPath("$.title").value("Task"))
                .andExpect(jsonPath("$.index").value(3));
    }

    @Test
    void getByIdNotFound() throws Exception {
        mockMvc.perform(get(URL + "/999999").with(auth())).andExpect(status().isNotFound());
    }

    @Test
    void getByIdWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(URL + "/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void getByIdNonNumericReturns400() throws Exception {
        mockMvc.perform(get(URL + "/abc").with(auth())).andExpect(status().isBadRequest());
    }

    // ============================ POST ============================

    @Test
    void createReturns201AndLocation() throws Exception {
        var body =
                """
                { "title": "New", "status": "draft" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("New"))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.assignee_id").value(nullValue()))
                .andExpect(jsonPath("$.index").value(nullValue()));
    }

    @Test
    void createWithAssigneeAndIndex() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        var body =
                """
                { "title": "New", "status": "to_review", "assignee_id": %d, "index": 7 }
                """
                        .formatted(user.getId());

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee_id").value(user.getId()))
                .andExpect(jsonPath("$.index").value(7));
    }

    @Test
    void createTrimsTitle() throws Exception {
        var body =
                """
                { "title": "  Padded  ", "status": "draft" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Padded"));
    }

    @Test
    void createWithUnknownStatusReturns400() throws Exception {
        var body =
                """
                { "title": "X", "status": "nope" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void createWithUnknownAssigneeReturns400() throws Exception {
        var body =
                """
                { "title": "X", "status": "draft", "assignee_id": 999999 }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.assignee_id").exists());
    }

    @Test
    void createWithBlankTitleReturns400() throws Exception {
        var body =
                """
                { "title": " ", "status": "draft" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithTooLongTitleReturns400() throws Exception {
        var title = "a".repeat(256);
        var body =
                """
                { "title": "%s", "status": "draft" }
                """
                        .formatted(title);

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithTooLongContentReturns400() throws Exception {
        var contentStr = "a".repeat(4001);
        var body =
                """
                { "title": "X", "status": "draft", "content": "%s" }
                """
                        .formatted(contentStr);

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithIndexAsStringReturns400() throws Exception {
        var body =
                """
                { "title": "X", "status": "draft", "index": "abc" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutStatusReturns400() throws Exception {
        var body =
                """
                { "title": "X" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createIgnoresIdAndCreatedAt() throws Exception {
        var body =
                """
                { "id": 999, "createdAt": "2020-01-01", "title": "X", "status": "draft" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(999)))
                .andExpect(jsonPath("$.createdAt").value(not("2020-01-01")));
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        var body =
                """
                { "title": "X", "status": "draft" }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    // ============================ PUT ============================

    @Test
    void updateOnlyTitle() throws Exception {
        var t = persistTask("Old", "c", 1, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "title": "New" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"))
                .andExpect(jsonPath("$.content").value("c"))
                .andExpect(jsonPath("$.status").value("draft"));
    }

    @Test
    void updateContentToNullClears() throws Exception {
        var t = persistTask("X", "content", null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "content": null }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(nullValue()));
    }

    @Test
    void updateAssigneeToNullClears() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        var t = persistTask("X", null, null, draft(), user);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "assignee_id": null }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee_id").value(nullValue()));
    }

    @Test
    void updateChangeStatus() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "status": "to_review" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("to_review"));
    }

    @Test
    void updateKanbanStyleIndexOnly() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "index": 2 }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.index").value(2));
    }

    @Test
    void updateKanbanStyleIndexAndStatus() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "index": 3, "status": "published" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.index").value(3))
                .andExpect(jsonPath("$.status").value("published"));
    }

    @Test
    void updateIgnoresUnknownFields() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "title": "Y", "labels": [1, 2], "assigneeId": 99 }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Y"));
    }

    @Test
    void updateTitleToNullReturns400() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "title": null }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBlankTitleReturns400() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "title": "   " }"""))
                .andExpect(status().isBadRequest());

        var after = taskRepository.findById(t.getId()).orElseThrow();
        assertThat(after.getName()).isEqualTo("X");
    }

    @Test
    void updateStatusToNullReturns400() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "status": null }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUnknownStatusReturns400() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "status": "nope" }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void updateUnknownAssigneeReturns400() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "assignee_id": 999999 }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.assignee_id").exists());
    }

    @Test
    void updateIndexAsStringReturns400() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "index": "abc" }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateNestedObjectReturns400() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "title": { "a": 1 } }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateArrayBodyReturns400() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[1,2]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateIgnoresIdAndCreatedAt() throws Exception {
        var t = persistTask("X", null, null, draft(), null);
        var originalId = t.getId();

        mockMvc.perform(
                        put(URL + "/" + originalId)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "id": 999, "createdAt": "2020-01-01", "title": "Y" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(originalId))
                .andExpect(jsonPath("$.createdAt").value(not("2020-01-01")));
    }

    @Test
    void updateNonExistentReturns404() throws Exception {
        mockMvc.perform(
                        put(URL + "/999999")
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "title": "X" }"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateArrayBodyOnNonExistentIdReturns400() throws Exception {
        // 400 должен выиграть у 404: сначала проверяется тело, потом ищется задача
        mockMvc.perform(
                        put(URL + "/999999")
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[1, 2, 3]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithoutTokenReturns401() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "title": "Y" }"""))
                .andExpect(status().isUnauthorized());
    }

    // ============================ DELETE ============================

    @Test
    void deleteReturns204() throws Exception {
        var t = persistTask("X", null, null, draft(), null);

        mockMvc.perform(delete(URL + "/" + t.getId()).with(auth()))
                .andExpect(status().isNoContent());

        assertThat(taskRepository.findById(t.getId())).isEmpty();
    }

    @Test
    void deleteNonExistentReturns404() throws Exception {
        mockMvc.perform(delete(URL + "/999999").with(auth())).andExpect(status().isNotFound());
    }

    @Test
    void deleteWithoutTokenReturns401() throws Exception {
        var t = persistTask("X", null, null, draft(), null);
        mockMvc.perform(delete(URL + "/" + t.getId())).andExpect(status().isUnauthorized());
    }

    // ============================ cross-resource (409) ============================

    @Test
    void deleteUserAssignedToTaskReturns409() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        persistTask("X", null, null, draft(), user);

        mockMvc.perform(delete("/api/users/" + user.getId()).with(authAs(user)))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("assigned to tasks")));

        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void deleteUserWithTwoTasksReturns409() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        persistTask("X", null, null, draft(), user);
        persistTask("Y", null, null, draft(), user);

        mockMvc.perform(delete("/api/users/" + user.getId()).with(authAs(user)))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("assigned to tasks")));

        assertThat(userRepository.findById(user.getId())).isPresent();
        assertThat(taskRepository.findAll()).hasSize(2);
    }

    @Test
    void deleteStatusUsedByTaskReturns409() throws Exception {
        var status = persistStatus("InUse", "in-use");
        persistTask("X", null, null, status, null);

        mockMvc.perform(delete("/api/task_statuses/" + status.getId()).with(auth()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("used by tasks")));

        assertThat(taskStatusRepository.findById(status.getId())).isPresent();
    }

    @Test
    void deleteUserAfterTaskRemovedReturns204() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        var task = persistTask("X", null, null, draft(), user);

        mockMvc.perform(delete(URL + "/" + task.getId()).with(auth()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/users/" + user.getId()).with(authAs(user)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    void deleteStatusAfterTaskRemovedReturns204() throws Exception {
        var status = persistStatus("TemporaryStatus", "temporary-status");
        var task = persistTask("X", null, null, status, null);

        mockMvc.perform(delete(URL + "/" + task.getId()).with(auth()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/task_statuses/" + status.getId()).with(auth()))
                .andExpect(status().isNoContent());

        assertThat(taskStatusRepository.findById(status.getId())).isEmpty();
    }

    // ============================ labels ============================

    @Test
    void createWithLabels() throws Exception {
        var label1 = persistLabel("lbl-bug");
        var label2 = persistLabel("lbl-feature");
        var body =
                """
                { "title": "T", "status": "draft", "taskLabelIds": [%d, %d] }"""
                        .formatted(label2.getId(), label1.getId());

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskLabelIds", hasSize(2)))
                .andExpect(jsonPath("$.taskLabelIds[0]").value(label1.getId()))
                .andExpect(jsonPath("$.taskLabelIds[1]").value(label2.getId()));
    }

    @Test
    void createWithoutTaskLabelIdsGivesEmptyArray() throws Exception {
        var body =
                """
                { "title": "T", "status": "draft" }""";

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskLabelIds").isArray())
                .andExpect(jsonPath("$.taskLabelIds").isEmpty());
    }

    @Test
    void createWithNullTaskLabelIdsGivesEmptyArray() throws Exception {
        var body =
                """
                { "title": "T", "status": "draft", "taskLabelIds": null }""";

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskLabelIds").isEmpty());
    }

    @Test
    void createWithDuplicateLabelIdsCollapses() throws Exception {
        var label = persistLabel("dup");
        var body =
                """
                { "title": "T", "status": "draft", "taskLabelIds": [%d, %d] }"""
                        .formatted(label.getId(), label.getId());

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskLabelIds", hasSize(1)))
                .andExpect(jsonPath("$.taskLabelIds[0]").value(label.getId()));
    }

    @Test
    void createWithUnknownLabelReturns400() throws Exception {
        var body =
                """
                { "title": "T", "status": "draft", "taskLabelIds": [999999] }""";

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.taskLabelIds").exists());
    }

    @Test
    void createWithNullInTaskLabelIdsReturns400() throws Exception {
        var body =
                """
                { "title": "T", "status": "draft", "taskLabelIds": [null] }""";

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putReplacesLabels() throws Exception {
        var label1 = persistLabel("l1");
        var label2 = persistLabel("l2");
        var t = persistTask("T", null, null, draft(), null);
        t.getLabels().add(label1);
        taskRepository.save(t);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "taskLabelIds": [%d] }"""
                                                .formatted(label2.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskLabelIds", hasSize(1)))
                .andExpect(jsonPath("$.taskLabelIds[0]").value(label2.getId()));
    }

    @Test
    void putEmptyArrayClearsLabels() throws Exception {
        var label = persistLabel("x");
        var t = persistTask("T", null, null, draft(), null);
        t.getLabels().add(label);
        taskRepository.save(t);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "taskLabelIds": [] }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskLabelIds").isEmpty());
    }

    @Test
    void putNullClearsLabels() throws Exception {
        var label = persistLabel("x");
        var t = persistTask("T", null, null, draft(), null);
        t.getLabels().add(label);
        taskRepository.save(t);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "taskLabelIds": null }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskLabelIds").isEmpty());
    }

    @Test
    void putIndexOnlyKeepsLabels() throws Exception {
        var label = persistLabel("keep");
        var t = persistTask("T", null, null, draft(), null);
        t.getLabels().add(label);
        taskRepository.save(t);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "index": 42 }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.index").value(42))
                .andExpect(jsonPath("$.taskLabelIds", hasSize(1)))
                .andExpect(jsonPath("$.taskLabelIds[0]").value(label.getId()));
    }

    @Test
    void putUnknownLabelReturns400() throws Exception {
        var t = persistTask("T", null, null, draft(), null);

        mockMvc.perform(
                        put(URL + "/" + t.getId())
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "taskLabelIds": [999999] }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.taskLabelIds").exists());
    }

    @Test
    void deleteTaskKeepsLabels() throws Exception {
        var label = persistLabel("survivor");
        var t = persistTask("T", null, null, draft(), null);
        t.getLabels().add(label);
        taskRepository.save(t);

        mockMvc.perform(delete(URL + "/" + t.getId()).with(auth()))
                .andExpect(status().isNoContent());

        assertThat(labelRepository.findById(label.getId())).isPresent();
    }

    private Label persistLabel(String name) {
        var l = new Label();
        l.setName(name);
        return labelRepository.save(l);
    }

    // ============================ helpers ============================

    private TaskStatus draft() {
        return taskStatusRepository.findBySlug("draft").orElseThrow();
    }

    private TaskStatus persistStatus(String name, String slug) {
        var s = new TaskStatus();
        s.setName(name);
        s.setSlug(slug);
        return taskStatusRepository.save(s);
    }

    private User persistUser(String email, String rawPassword) {
        var u = new User();
        u.setEmail(email);
        u.setPasswordDigest(passwordEncoder.encode(rawPassword));
        return userRepository.save(u);
    }

    private Task persistTask(
            String name, String description, Integer index, TaskStatus status, User assignee) {
        var t = new Task();
        t.setName(name);
        t.setDescription(description);
        t.setIndex(index);
        t.setTaskStatus(status);
        t.setAssignee(assignee);
        t.setCreatedAt(LocalDateTime.now());
        return taskRepository.save(t);
    }

    private RequestPostProcessor auth() {
        return jwt().jwt(j -> j.subject("1"));
    }

    private RequestPostProcessor authAs(User user) {
        return jwt().jwt(j -> j.subject(String.valueOf(user.getId())));
    }
}
