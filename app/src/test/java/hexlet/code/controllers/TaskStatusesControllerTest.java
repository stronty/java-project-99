package hexlet.code;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import hexlet.code.model.TaskStatus;
import hexlet.code.repository.TaskStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskStatusesControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired
    TaskStatusRepository repository;

    private RequestPostProcessor auth() {
        return jwt().jwt(j -> j.subject("hexlet@example.com"));
    }

    private TaskStatus saveStatus(String name, String slug) {
        var s = new TaskStatus();
        s.setName(name);
        s.setSlug(slug);
        return repository.save(s);
    }

    // ---------- authentication ----------

    @Test
    void listWithoutTokenGives401() throws Exception {
        mockMvc.perform(get("/api/task_statuses")).andExpect(status().isUnauthorized());
    }

    @Test
    void createWithoutTokenGives401() throws Exception {
        mockMvc.perform(post("/api/task_statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"New","slug":"new"}"""))
                .andExpect(status().isUnauthorized());
    }

    // ---------- read ----------

    @Test
    void listContainsDefaults() throws Exception {
        mockMvc.perform(get("/api/task_statuses").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", hasItems(
                        "draft", "to_review", "to_be_fixed", "to_publish", "published")));
    }

    @Test
    void showReturnsAllFields() throws Exception {
        var s = saveStatus("MyStatus", "my_status");
        mockMvc.perform(get("/api/task_statuses/" + s.getId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(s.getId()))
                .andExpect(jsonPath("$.name").value("MyStatus"))
                .andExpect(jsonPath("$.slug").value("my_status"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void showMissingGives404() throws Exception {
        mockMvc.perform(get("/api/task_statuses/999999").with(auth()))
                .andExpect(status().isNotFound());
    }

    // ---------- create ----------

    @Test
    void createReturns201AndPersists() throws Exception {
        mockMvc.perform(post("/api/task_statuses").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"New","slug":"new"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.slug").value("new"))
                .andExpect(jsonPath("$.createdAt").exists());

        assertTrue(repository.findBySlug("new").isPresent());
    }

    @Test
    void createWithBlankNameGives400() throws Exception {
        mockMvc.perform(post("/api/task_statuses").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"","slug":"x"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutSlugGives400() throws Exception {
        mockMvc.perform(post("/api/task_statuses").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"NoSlug"}"""))
                .andExpect(status().isBadRequest());
    }

    // ---------- update ----------

    @Test
    void partialUpdateKeepsSlug() throws Exception {
        var s = saveStatus("Old", "old_slug");
        mockMvc.perform(put("/api/task_statuses/" + s.getId()).with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"newStatus"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("newStatus"))
                .andExpect(jsonPath("$.slug").value("old_slug"));
    }

    @Test
    void updateWithBlankNameGives400() throws Exception {
        var s = saveStatus("Old", "old_slug");
        mockMvc.perform(put("/api/task_statuses/" + s.getId()).with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"  "}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMissingGives404() throws Exception {
        mockMvc.perform(put("/api/task_statuses/999999").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"x"}"""))
                .andExpect(status().isNotFound());
    }

    // ---------- delete ----------

    @Test
    void deleteThenGetGives404() throws Exception {
        var s = saveStatus("Temp", "temp");
        mockMvc.perform(delete("/api/task_statuses/" + s.getId()).with(auth()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/task_statuses/" + s.getId()).with(auth()))
                .andExpect(status().isNotFound());
    }

    // ---------- uniqueness (keep these last in their own tests) ----------

    @Test
    void duplicateSlugGives409() throws Exception {
        saveStatus("First", "dup");
        mockMvc.perform(post("/api/task_statuses").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"Second","slug":"dup"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void duplicateNameGives409() throws Exception {
        saveStatus("SameName", "one");
        mockMvc.perform(post("/api/task_statuses").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"SameName","slug":"two"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void defaultsExist() {
        for (var slug : new String[] {"draft", "to_review", "to_be_fixed", "to_publish", "published"}) {
            assertTrue(repository.findBySlug(slug).isPresent(), slug);
        }
        assertEquals("Draft", repository.findBySlug("draft").orElseThrow().getName());
    }
}
