package hexlet.code;

import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TasksControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskStatusRepository statusRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LabelRepository labelRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private User admin;
    private TaskStatus draft;
    private User extraUser;
    private TaskStatus extraStatus;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        admin = userRepository.findByEmail("hexlet@example.com").orElseThrow();
        draft = statusRepository.findBySlug("draft").orElseThrow();
        adminToken = login("hexlet@example.com", "qwerty");
    }

    @AfterEach
    void tearDown() {
        taskRepository.deleteAll();
        if (extraUser != null) {
            userRepository.delete(extraUser);
            extraUser = null;
        }
        if (extraStatus != null) {
            statusRepository.delete(extraStatus);
            extraStatus = null;
        }
    }

    private String login(String email, String password) throws Exception {
        var body = """
                {"username": "%s", "password": "%s"}
                """.formatted(email, password);
        return mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Task createTask(String name) {
        var task = new Task();
        task.setName(name);
        task.setDescription("desc");
        task.setIndex(10);
        task.setTaskStatus(draft);
        task.setAssignee(admin);
        return taskRepository.save(task);
    }

    private Task createTaskWith(String name, TaskStatus taskStatus, User assignee, Label... labels) {
        var task = new Task();
        task.setName(name);
        task.setTaskStatus(taskStatus);
        task.setAssignee(assignee);
        for (var label : labels) {
            task.getLabels().add(label);
        }
        return taskRepository.save(task);
    }

    // ---------- auth ----------

    @Test
    void unauthenticatedGets401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- read ----------

    @Test
    void indexReturnsTasks() throws Exception {
        createTask("Task 1");
        createTask("Task 2");

        mockMvc.perform(get("/api/tasks").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Task 1"))
                .andExpect(jsonPath("$[0].status").value("draft"))
                .andExpect(jsonPath("$[0].assignee_id").value(admin.getId()));
    }

    @Test
    void showReturnsTask() throws Exception {
        var task = createTask("Task 1");

        mockMvc.perform(get("/api/tasks/" + task.getId()).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.index").value(10))
                .andExpect(jsonPath("$.title").value("Task 1"))
                .andExpect(jsonPath("$.content").value("desc"))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.assignee_id").value(admin.getId()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void showMissingGives404() throws Exception {
        mockMvc.perform(get("/api/tasks/999999").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    // ---------- create ----------

    @Test
    void createWorks() throws Exception {
        var body = """
                {"index": 12, "assignee_id": %d, "title": "Test title",
                 "content": "Test content", "status": "draft"}
                """.formatted(admin.getId());

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Test title"))
                .andExpect(jsonPath("$.content").value("Test content"))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.index").value(12))
                .andExpect(jsonPath("$.assignee_id").value(admin.getId()));

        Assertions.assertEquals(1, taskRepository.count());
    }

    @Test
    void createWithoutAssigneeWorks() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "No assignee", "status": "draft"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee_id").doesNotExist());
    }

    @Test
    void createWithBlankTitleGives400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "status": "draft"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutStatusGives400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "No status"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithUnknownStatusGives400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Bad status", "status": "no_such_status"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithUnknownAssigneeGives400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Bad assignee", "status": "draft", "assignee_id": 999999}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ---------- update ----------

    @Test
    void partialUpdateKeepsOtherFields() throws Exception {
        var task = createTask("Old title");

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "New title", "content": "New content"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.content").value("New content"))
                .andExpect(jsonPath("$.index").value(10))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.assignee_id").value(admin.getId()));
    }

    @Test
    void updateStatusBySlug() throws Exception {
        var task = createTask("Task");

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "published"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"));
    }

    @Test
    void updateMissingGives404() throws Exception {
        mockMvc.perform(put("/api/tasks/999999")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "x"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ---------- delete ----------

    @Test
    void deleteWorks() throws Exception {
        var task = createTask("Task");

        mockMvc.perform(delete("/api/tasks/" + task.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        Assertions.assertTrue(taskRepository.findById(task.getId()).isEmpty());
    }

    @Test
    void deleteMissingGives404() throws Exception {
        mockMvc.perform(delete("/api/tasks/999999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotDeleteUserWithTasks() throws Exception {
        var user = new User();
        user.setEmail("assignee@example.com");
        user.setPassword(passwordEncoder.encode("secret"));
        extraUser = userRepository.save(user);

        createTaskWith("Assigned", draft, extraUser);

        var userToken = login("assignee@example.com", "secret");

        mockMvc.perform(delete("/api/users/" + extraUser.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isConflict());

        Assertions.assertTrue(userRepository.existsById(extraUser.getId()));
    }

    @Test
    void cannotDeleteStatusWithTasks() throws Exception {
        var newStatus = new TaskStatus();
        newStatus.setName("TestStatus");
        newStatus.setSlug("test_status");
        extraStatus = statusRepository.save(newStatus);

        createTaskWith("With status", extraStatus, null);

        mockMvc.perform(delete("/api/task_statuses/" + extraStatus.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());

        Assertions.assertTrue(statusRepository.existsById(extraStatus.getId()));
    }

    // ---------- filter ----------

    @Test
    void filterByTitleIgnoresCase() throws Exception {
        createTaskWith("Create new version", draft, admin);
        createTaskWith("Fix bug", draft, admin);

        mockMvc.perform(get("/api/tasks?titleCont=CREATE").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Create new version"));
    }

    @Test
    void filterByAssignee() throws Exception {
        createTaskWith("Mine", draft, admin);
        createTaskWith("Unassigned", draft, null);

        mockMvc.perform(get("/api/tasks?assigneeId=" + admin.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Mine"));
    }

    @Test
    void filterByStatus() throws Exception {
        var published = statusRepository.findBySlug("published").orElseThrow();
        createTaskWith("Draft task", draft, admin);
        createTaskWith("Published task", published, admin);

        mockMvc.perform(get("/api/tasks?status=published").header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Published task"));
    }

    @Test
    void filterByLabel() throws Exception {
        var bug = labelRepository.findByName("bug").orElseThrow();
        createTaskWith("Labeled", draft, admin, bug);
        createTaskWith("Plain", draft, admin);

        mockMvc.perform(get("/api/tasks?labelId=" + bug.getId()).header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Labeled"));
    }

    @Test
    void filterCombined() throws Exception {
        var bug = labelRepository.findByName("bug").orElseThrow();
        var published = statusRepository.findBySlug("published").orElseThrow();
        createTaskWith("Create new version", published, admin, bug);
        createTaskWith("Create something else", draft, admin, bug);
        createTaskWith("Unrelated", published, admin);

        mockMvc.perform(get("/api/tasks?titleCont=create&assigneeId=" + admin.getId()
                        + "&status=published&labelId=" + bug.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Create new version"));
    }

    @Test
    void filterWithNoMatchReturnsEmptyList() throws Exception {
        createTaskWith("Something", draft, admin);

        mockMvc.perform(get("/api/tasks?titleCont=nothing").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void updateIndexAndAssignee() throws Exception {
        var user = new User();
        user.setEmail("newassignee@example.com");
        user.setPassword(passwordEncoder.encode("secret"));
        extraUser = userRepository.save(user);
        var task = createTask("Task");

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"index": 99, "assignee_id": %d}
                            """.formatted(extraUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.index").value(99))
                .andExpect(jsonPath("$.assignee_id").value(extraUser.getId()))
                .andExpect(jsonPath("$.title").value("Task"));
    }

    @Test
    void updateWithUnknownStatusGives400() throws Exception {
        var task = createTask("Task");

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"status": "no_such_status"}
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithUnknownAssigneeGives400() throws Exception {
        var task = createTask("Task");

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"assignee_id": 999999}
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithUnknownLabelGives400() throws Exception {
        var task = createTask("Task");

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"taskLabelIds": [999999]}
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithBlankTitleGives400() throws Exception {
        var task = createTask("Task");

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title": ""}
                            """))
                .andExpect(status().isBadRequest());
    }
    @Test
    void noParamsReturnsAll() throws Exception {
        createTaskWith("One", draft, admin);
        createTaskWith("Two", draft, admin);

        mockMvc.perform(get("/api/tasks").header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(2));
    }
}