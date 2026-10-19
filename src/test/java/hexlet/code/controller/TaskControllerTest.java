package hexlet.code.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import jakarta.persistence.EntityManager;
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
    @Autowired private EntityManager entityManager;

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

    // ============================ GET list — filters ============================

    @Test
    void filterByTitleContMatchesSubstringAndIgnoresCase() throws Exception {
        persistTask("Create new version", null, null, draft(), null);
        persistTask("Another task", null, null, draft(), null);
        persistTask("Unrelated", null, null, draft(), null);

        mockMvc.perform(get(URL).param("titleCont", "create").with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Create new version"));

        mockMvc.perform(get(URL).param("titleCont", "NEW").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Create new version"));

        mockMvc.perform(get(URL).param("titleCont", "eate").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Create new version"));
    }

    @Test
    void filterByTitleContEscapesPercent() throws Exception {
        persistTask("100% done", null, null, draft(), null);
        persistTask("100 done", null, null, draft(), null);
        persistTask("1000 done", null, null, draft(), null);

        mockMvc.perform(get(URL).param("titleCont", "100%").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("100% done"));
    }

    @Test
    void filterByTitleContEscapesUnderscore() throws Exception {
        persistTask("a_b", null, null, draft(), null);
        persistTask("axb", null, null, draft(), null);
        persistTask("aXXb", null, null, draft(), null);

        mockMvc.perform(get(URL).param("titleCont", "a_b").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("a_b"));
    }

    @Test
    void filterByTitleContMatchesCyrillicSubstringIgnoringCase() throws Exception {
        persistTask("Создать отчёт", null, null, draft(), null);
        persistTask("Другая задача", null, null, draft(), null);
        persistTask("Отчёт за квартал", null, null, draft(), null);

        // нижний регистр в запросе → находит
        mockMvc.perform(get(URL).param("titleCont", "создать").with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Создать отчёт"));

        // верхний регистр в запросе → тоже находит
        mockMvc.perform(get(URL).param("titleCont", "СОЗДАТЬ").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Создать отчёт"));

        // подстрока из середины слова
        mockMvc.perform(get(URL).param("titleCont", "здат").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Создать отчёт"));
    }

    @Test
    void filterByTitleContEscapesBackslash() throws Exception {
        persistTask("path\\to\\file", null, null, draft(), null);
        // Контроль: без экранирования шаблон '%path\to\file%' ESCAPE '\'
        // схлопнулся бы в '%pathtofile%' и нашёл бы эту задачу, а не искомую.
        persistTask("pathtofile", null, null, draft(), null);

        mockMvc.perform(get(URL).param("titleCont", "path\\to\\file").with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("path\\to\\file"));
    }

    @Test
    void filterByAssigneeId() throws Exception {
        var user = persistUser("u-f1@x.com", "qwerty123");
        persistTask("Assigned", null, null, draft(), user);
        persistTask("Unassigned", null, null, draft(), null);

        mockMvc.perform(get(URL).param("assigneeId", user.getId().toString()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Assigned"))
                .andExpect(jsonPath("$[0].assignee_id").value(user.getId()));
    }

    @Test
    void filterByStatus() throws Exception {
        var other = persistStatus("FilterStatus", "filter-status");
        persistTask("Draft", null, null, draft(), null);
        persistTask("Other", null, null, other, null);

        mockMvc.perform(get(URL).param("status", "draft").with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Draft"))
                .andExpect(jsonPath("$[0].status").value("draft"));
    }

    @Test
    void filterByLabelId() throws Exception {
        var label = persistLabel("filter-lbl");
        var withLabel = persistTask("With label", null, null, draft(), null);
        withLabel.getLabels().add(label);
        taskRepository.save(withLabel);
        persistTask("Without label", null, null, draft(), null);

        mockMvc.perform(get(URL).param("labelId", label.getId().toString()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("With label"));
    }

    @Test
    void filterByAllParamsCombinedWithAnd() throws Exception {
        var user = persistUser("u-combo@x.com", "qwerty123");
        var label = persistLabel("combo-lbl");
        var otherStatus = persistStatus("OtherCombo", "other-combo");

        var matching = persistTask("match-target", null, null, draft(), user);
        matching.getLabels().add(label);
        taskRepository.save(matching);

        var wrongTitle = persistTask("different", null, null, draft(), user);
        wrongTitle.getLabels().add(label);
        taskRepository.save(wrongTitle);

        var wrongStatus = persistTask("match-target", null, null, otherStatus, user);
        wrongStatus.getLabels().add(label);
        taskRepository.save(wrongStatus);

        var wrongAssignee = persistTask("match-target", null, null, draft(), null);
        wrongAssignee.getLabels().add(label);
        taskRepository.save(wrongAssignee);

        persistTask("match-target", null, null, draft(), user);

        mockMvc.perform(
                        get(URL).param("titleCont", "match")
                                .param("status", "draft")
                                .param("assigneeId", user.getId().toString())
                                .param("labelId", label.getId().toString())
                                .with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("match-target"));
    }

    @Test
    void filterEmptyAndBlankValuesAreIgnored() throws Exception {
        persistTask("A", null, null, draft(), null);
        persistTask("B", null, null, draft(), null);

        mockMvc.perform(get(URL).param("titleCont", "").param("status", "   ").with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void filterUnknownStatusReturnsEmptyList() throws Exception {
        persistTask("Any", null, null, draft(), null);

        mockMvc.perform(get(URL).param("status", "no-such-status").with(auth()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void filterUnknownLabelIdReturnsEmptyList() throws Exception {
        persistTask("Any", null, null, draft(), null);

        mockMvc.perform(get(URL).param("labelId", "999999").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void filterUnknownAssigneeIdReturnsEmptyList() throws Exception {
        persistTask("Any", null, null, draft(), null);

        mockMvc.perform(get(URL).param("assigneeId", "999999").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void filterWithNonNumericAssigneeIdReturns400() throws Exception {
        mockMvc.perform(get(URL).param("assigneeId", "abc").with(auth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filterWithNonNumericLabelIdReturns400() throws Exception {
        mockMvc.perform(get(URL).param("labelId", "abc").with(auth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filterLabelIdDoesNotTrimOtherLabels() throws Exception {
        var l1 = persistLabel("trim-lbl-1");
        var l2 = persistLabel("trim-lbl-2");
        var task = persistTask("Multi-label", null, null, draft(), null);
        task.getLabels().add(l1);
        task.getLabels().add(l2);
        taskRepository.save(task);

        // Выгружаем persistence context, чтобы findAll реально шёл в БД,
        // а не возвращал задачу с коллекцией labels из памяти.
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(URL).param("labelId", l1.getId().toString()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(
                        jsonPath(
                                "$[0].taskLabelIds",
                                containsInAnyOrder(l1.getId().intValue(), l2.getId().intValue())));
    }

    @Test
    void filterWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(URL).param("titleCont", "x")).andExpect(status().isUnauthorized());

        mockMvc.perform(get(URL).param("labelId", "1")).andExpect(status().isUnauthorized());
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
    void createTrimsSpacesAroundTitleInDatabase() throws Exception {
        var body =
                """
                { "title": "   Создать отчёт   ", "status": "draft" }
                """;

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Создать отчёт"));

        // Дополнительно проверяем, что и в БД сохранено обрезанное значение:
        // createTrimsTitle смотрит только на ответ, а тут — состояние после save().
        assertThat(taskRepository.findAll().stream().map(Task::getName).toList())
                .containsExactly("Создать отчёт");
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

    @Test
    void createWithTooManyLabelIdsReturns400() throws Exception {
        var ids = new StringBuilder();
        for (int i = 0; i < 101; i++) {
            if (i > 0) {
                ids.append(",");
            }
            ids.append(i + 1);
        }
        var body =
                """
                { "title": "T", "status": "draft", "taskLabelIds": [%s] }"""
                        .formatted(ids);

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithNonNumericLabelIdReturns400() throws Exception {
        var body =
                """
                { "title": "T", "status": "draft", "taskLabelIds": ["a"] }""";

        mockMvc.perform(
                        post(URL)
                                .with(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
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

    // ============================ helpers ============================

    private Label persistLabel(String name) {
        var l = new Label();
        l.setName(name);
        return labelRepository.save(l);
    }

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
