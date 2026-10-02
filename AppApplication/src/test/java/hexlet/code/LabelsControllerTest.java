package hexlet.code;

import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LabelsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LabelRepository labelRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskStatusRepository statusRepository;

    private String token;
    private TaskStatus draft;
    private final List<Label> created = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        draft = statusRepository.findBySlug("draft").orElseThrow();
        token = login();
    }

    @AfterEach
    void tearDown() {
        taskRepository.deleteAll();
        for (var label : created) {
            labelRepository.findById(label.getId()).ifPresent(labelRepository::delete);
        }
        created.clear();
        // labels created through the API (not via createLabel) are cleaned by name
        for (var name : List.of("api label", "api label renamed", "dup label")) {
            labelRepository.findByName(name).ifPresent(labelRepository::delete);
        }
    }

    private String login() throws Exception {
        var body = """
                {"username": "hexlet@example.com", "password": "qwerty"}
                """;
        return mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String auth() {
        return "Bearer " + token;
    }

    private Label createLabel(String name) {
        var label = new Label();
        label.setName(name);
        var saved = labelRepository.save(label);
        created.add(saved);
        return saved;
    }

    private Task createTaskWith(Label... labels) {
        var task = new Task();
        task.setName("Task with labels");
        task.setTaskStatus(draft);
        for (var label : labels) {
            task.getLabels().add(label);
        }
        return taskRepository.save(task);
    }

    // ---------- labels CRUD ----------

    @Test
    void unauthenticatedGets401() throws Exception {
        mockMvc.perform(get("/api/labels")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"abc\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void indexReturnsLabels() throws Exception {
        createLabel("test index label");

        mockMvc.perform(get("/api/labels").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("test index label")));
    }

    @Test
    void seededLabelsExist() throws Exception {
        mockMvc.perform(get("/api/labels").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("feature")))
                .andExpect(jsonPath("$[*].name", hasItem("bug")));
    }

    @Test
    void showReturnsLabel() throws Exception {
        var label = createLabel("test show label");

        mockMvc.perform(get("/api/labels/" + label.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(label.getId()))
                .andExpect(jsonPath("$.name").value("test show label"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void showMissingGives404() throws Exception {
        mockMvc.perform(get("/api/labels/999999").header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWorks() throws Exception {
        mockMvc.perform(post("/api/labels")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"api label\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("api label"))
                .andExpect(jsonPath("$.createdAt").exists());

        Assertions.assertTrue(labelRepository.findByName("api label").isPresent());
    }

    @Test
    void createWithShortNameGives400() throws Exception {
        mockMvc.perform(post("/api/labels")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"ab\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutNameGives400() throws Exception {
        mockMvc.perform(post("/api/labels")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDuplicateGives409() throws Exception {
        createLabel("dup label");

        mockMvc.perform(post("/api/labels")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"dup label\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateWorks() throws Exception {
        var label = createLabel("api label");

        mockMvc.perform(put("/api/labels/" + label.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"api label renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(label.getId()))
                .andExpect(jsonPath("$.name").value("api label renamed"));
    }

    @Test
    void updateWithSameNameWorks() throws Exception {
        var label = createLabel("api label");

        mockMvc.perform(put("/api/labels/" + label.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"api label\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateToExistingNameGives409() throws Exception {
        createLabel("dup label");
        var other = createLabel("api label");

        mockMvc.perform(put("/api/labels/" + other.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"dup label\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateWithShortNameGives400() throws Exception {
        var label = createLabel("api label");

        mockMvc.perform(put("/api/labels/" + label.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"ab\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMissingGives404() throws Exception {
        mockMvc.perform(put("/api/labels/999999")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"whatever\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWorks() throws Exception {
        var label = createLabel("api label");

        mockMvc.perform(delete("/api/labels/" + label.getId()).header("Authorization", auth()))
                .andExpect(status().isNoContent());

        Assertions.assertTrue(labelRepository.findById(label.getId()).isEmpty());
    }

    @Test
    void deleteMissingGives404() throws Exception {
        mockMvc.perform(delete("/api/labels/999999").header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotDeleteLabelUsedByTask() throws Exception {
        var label = createLabel("api label");
        createTaskWith(label);

        mockMvc.perform(delete("/api/labels/" + label.getId()).header("Authorization", auth()))
                .andExpect(status().isConflict());

        Assertions.assertTrue(labelRepository.existsById(label.getId()));
    }

    // ---------- labels inside tasks ----------

    @Test
    void createTaskWithLabels() throws Exception {
        var first = createLabel("label one");
        var second = createLabel("label two");
        var body = """
                {"title": "Labeled", "status": "draft", "taskLabelIds": [%d, %d]}
                """.formatted(first.getId(), second.getId());

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskLabelIds.length()").value(2))
                .andExpect(jsonPath("$.taskLabelIds", hasItem(first.getId().intValue())))
                .andExpect(jsonPath("$.taskLabelIds", hasItem(second.getId().intValue())));
    }

    @Test
    void createTaskWithoutLabelsReturnsEmptyList() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"No labels\", \"status\": \"draft\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskLabelIds.length()").value(0));
    }

    @Test
    void createTaskWithUnknownLabelGives400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Bad\", \"status\": \"draft\", \"taskLabelIds\": [999999]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTaskLabels() throws Exception {
        var first = createLabel("label one");
        var second = createLabel("label two");
        var task = createTaskWith(first);

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskLabelIds\": [" + second.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskLabelIds.length()").value(1))
                .andExpect(jsonPath("$.taskLabelIds[0]").value(second.getId()));
    }

    @Test
    void updateTaskWithoutLabelsFieldKeepsLabels() throws Exception {
        var label = createLabel("label one");
        var task = createTaskWith(label);

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.taskLabelIds.length()").value(1));
    }

    @Test
    void updateTaskWithEmptyLabelsClearsThem() throws Exception {
        var label = createLabel("label one");
        var task = createTaskWith(label);

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskLabelIds\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskLabelIds.length()").value(0));
    }

    @Test
    void deletingTaskKeepsLabel() throws Exception {
        var label = createLabel("label one");
        var task = createTaskWith(label);

        mockMvc.perform(delete("/api/tasks/" + task.getId()).header("Authorization", auth()))
                .andExpect(status().isNoContent());

        Assertions.assertTrue(labelRepository.existsById(label.getId()));
    }
}