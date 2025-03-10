package com.dev.taskmanagement.controller;

import com.dev.taskmanagement.dto.task.TaskRequest;
import com.dev.taskmanagement.dto.task.TaskResponse;
import com.dev.taskmanagement.model.Role;
import com.dev.taskmanagement.model.TaskPriority;
import com.dev.taskmanagement.model.TaskStatus;
import com.dev.taskmanagement.model.User;
import com.dev.taskmanagement.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    private User adminUser;
    private User regularUser;
    private TaskResponse sampleTask;

    @BeforeEach
    void setup() {
        adminUser = new User(1, "admin@mail.com", "adminpass", "Admin", "User", Role.ROLE_ADMIN);
        regularUser = new User(2, "user@mail.com", "userpass", "Test", "User", Role.ROLE_USER);
        sampleTask = TaskResponse.builder()
                .id(1L)
                .title("Test Task")
                .description("Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .authorId(1L)
                .createdAt(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    void shouldCreateTaskSuccessfully() throws Exception {
        TaskRequest taskRequest = new TaskRequest(
                "New Task", "Task Description", TaskStatus.PENDING, TaskPriority.MEDIUM, LocalDateTime.now().plusDays(5), null);
        TaskResponse sampleTask = TaskResponse.builder()
                .id(1L)
                .title("Test Task")
                .description("Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .authorId(adminUser.getId())
                .assigneeId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();
        Mockito.when(taskService.createTask(Mockito.any(), Mockito.eq(adminUser.getId())))
                .thenReturn(sampleTask);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities());

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest))
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Task"));
    }


    @Test
    void shouldAllowAdminToDeleteTask() throws Exception {
        mockMvc.perform(delete("/api/tasks/1")
                        .with(user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isNoContent());
    }


    @Test
    @WithMockUser(username = "user@mail.com", roles = "USER")
    void shouldNotAllowUserToDeleteTask() throws Exception {
        Mockito.doThrow(new SecurityException("Only admins can delete tasks"))
                .when(taskService).deleteTask(Mockito.eq(1L), Mockito.any(User.class));

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isForbidden());
    }
}
