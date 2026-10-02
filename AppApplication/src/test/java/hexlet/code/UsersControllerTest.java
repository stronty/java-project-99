package hexlet.code;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-change-me-32-bytes-minimum-length"
})
@AutoConfigureMockMvc
@Transactional
class UsersControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired PasswordEncoder encoder;

    private User saveUser(String email, String rawPassword) {
        var u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(rawPassword));
        return userRepository.save(u);
    }

    private RequestPostProcessor asUser(String email) {
        return jwt().jwt(j -> j.subject(email));
    }

    // ---------- authentication ----------

    @Test
    void noTokenGives401() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenGives401() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer nonsense"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void welcomeIsPublic() throws Exception {
        mockMvc.perform(get("/welcome")).andExpect(status().isOk());
    }

    // ---------- login ----------

    @Test
    void loginReturnsUsableToken() throws Exception {
        saveUser("ivan@google.com", "some-password");

        var token = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"username":"ivan@google.com","password":"some-password"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(token.isBlank());

        // the real end-to-end check: the issued token is accepted by the decoder
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordGives401() throws Exception {
        saveUser("ivan@google.com", "some-password");
        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"username":"ivan@google.com","password":"wrong"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownEmailGives401() throws Exception {
        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"username":"nobody@google.com","password":"whatever"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyLoginBodyGives401() throws Exception {
        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- authorization (403) ----------

    @Test
    void userCanUpdateSelf() throws Exception {
        var me = saveUser("me@test.com", "secret");
        mockMvc.perform(put("/api/users/" + me.getId())
                        .with(asUser("me@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"firstName":"Changed"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Changed"));
    }

    @Test
    void userCannotUpdateOther() throws Exception {
        saveUser("me@test.com", "secret");
        var other = saveUser("other@test.com", "secret");
        mockMvc.perform(put("/api/users/" + other.getId())
                        .with(asUser("me@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"firstName":"Hacked"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotDeleteOther() throws Exception {
        saveUser("me@test.com", "secret");
        var other = saveUser("other@test.com", "secret");
        mockMvc.perform(delete("/api/users/" + other.getId()).with(asUser("me@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanDeleteSelf() throws Exception {
        var me = saveUser("me@test.com", "secret");
        mockMvc.perform(delete("/api/users/" + me.getId()).with(asUser("me@test.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingUserGives404NotForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/999999").with(asUser("me@test.com")))
                .andExpect(status().isNotFound());
    }

    // ---------- CRUD, now with a token ----------

    @Test
    void createHidesPassword() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(asUser("hexlet@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"email":"jack@google.com","password":"secret"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void invalidEmailGives400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(asUser("hexlet@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"email":"nope","password":"secret"}"""))
                .andExpect(status().isBadRequest());
    }
}