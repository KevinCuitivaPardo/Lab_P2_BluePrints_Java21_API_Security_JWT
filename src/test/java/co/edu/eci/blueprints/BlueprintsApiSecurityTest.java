package co.edu.eci.blueprints;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BlueprintsApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("access_token").asText();
    }

    @Test
    void loginWithInvalidCredentialsReturns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "student", "password", "wrong"));
        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousRequestToBlueprintsIsRejected() throws Exception {
        mockMvc.perform(get("/api/blueprints"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCanReadButNotWrite() throws Exception {
        String token = login("student", "student123");

        mockMvc.perform(get("/api/blueprints").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String newBlueprint = objectMapper.writeValueAsString(Map.of("author", "student", "name", "casa"));
        mockMvc.perform(post("/api/blueprints")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(newBlueprint))
                .andExpect(status().isForbidden());
    }

    @Test
    void assistantCanReadAndWrite() throws Exception {
        String token = login("assistant", "assistant123");

        String newBlueprint = objectMapper.writeValueAsString(Map.of("author", "assistant", "name", "torre"));
        mockMvc.perform(post("/api/blueprints")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(newBlueprint))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/blueprints").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
