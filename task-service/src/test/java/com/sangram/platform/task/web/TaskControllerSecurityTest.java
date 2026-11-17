package com.sangram.platform.task.web;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sangram.platform.task.security.JwtAuthFilter;
import com.sangram.platform.task.security.JwtService;
import com.sangram.platform.task.security.SecurityConfig;
import com.sangram.platform.task.service.TaskService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies the HTTP security rules end to end without needing MongoDB. */
@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=controller-test-secret-0123456789-abcdefghijklmnop",
        "app.jwt.ttl-minutes=5"})
class TaskControllerSecurityTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwtService;

    @MockitoBean
    TaskService taskService;

    private String bearer(String user, String... roles) {
        return "Bearer " + jwtService.issue(user, List.of(roles));
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithGarbageTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/tasks").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanListTasks() throws Exception {
        Page<com.sangram.platform.task.model.Task> empty = new PageImpl<>(List.of());
        org.mockito.Mockito.when(taskService.search(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(empty);

        mvc.perform(get("/api/tasks").header("Authorization", bearer("alice", "USER")))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotDeleteTasks() throws Exception {
        mvc.perform(delete("/api/tasks/1").header("Authorization", bearer("alice", "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteTasks() throws Exception {
        mvc.perform(delete("/api/tasks/1").header("Authorization", bearer("root", "ADMIN", "USER")))
                .andExpect(status().isNoContent());
        verify(taskService).delete("1");
    }
}
