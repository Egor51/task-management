package com.dev.taskmanagement.service.task;

import com.dev.taskmanagement.dto.task.TaskRequest;
import com.dev.taskmanagement.dto.task.TaskResponse;
import com.dev.taskmanagement.exception.ResourceNotFoundException;
import com.dev.taskmanagement.model.Task;
import com.dev.taskmanagement.model.TaskPriority;
import com.dev.taskmanagement.model.TaskStatus;
import com.dev.taskmanagement.model.User;
import com.dev.taskmanagement.repository.TaskRepository;
import com.dev.taskmanagement.repository.UserRepository;
import com.dev.taskmanagement.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    TaskRepository taskRepository;
    private User adminUser;
    private User regularUser;
    private Task testTask;
    @BeforeEach
    void setup() {
        adminUser = userRepository.findById(1L).orElseThrow();
        regularUser = userRepository.findById(2L).orElseThrow();
        testTask = new Task();
        testTask.setTitle("Test Task");
        testTask.setDescription("Task Description");
        testTask.setStatus(TaskStatus.PENDING);
        testTask.setPriority(TaskPriority.MEDIUM);
        testTask.setDueDate(LocalDateTime.now().plusDays(5));
        testTask.setAuthor(adminUser);
        taskRepository.save(testTask);
    }

    @Test
    void shouldCreateTaskSuccessfully() {
        TaskRequest request = new TaskRequest(
                "New Test Task",
                "Task Description",
                TaskStatus.PENDING,
                TaskPriority.MEDIUM,
                LocalDateTime.now().plusDays(5),
                null);
        TaskResponse response = taskService.createTask(request, regularUser.getId());
        assertNotNull(response);
        assertEquals("New Test Task", response.getTitle());
        assertEquals(TaskStatus.PENDING, response.getStatus());
        assertEquals(regularUser.getId(), response.getAuthorId());
    }

    @Test
    void shouldAllowAdminToDeleteTask() {
        taskService.deleteTask(testTask.getId(), adminUser);
        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(testTask.getId(),adminUser));
    }

    @Test
    void shouldNotAllowUserToDeleteTask() {
        assertThrows(SecurityException.class, () -> taskService.deleteTask(testTask.getId(), regularUser));
    }

    @Test
    void shouldAllowUserToSeeOnlyTheirTasks() {
        List<TaskResponse> tasks = taskService.getAllTasks(0, 10, regularUser);
        assertEquals(3, tasks.size()); // Потому что у regularUser нет задач
    }

    @Test
    void shouldAllowAdminToUpdateTaskStatus() {
        TaskResponse response = taskService.updateTaskStatus(testTask.getId(), "COMPLETED", adminUser);
        assertEquals(TaskStatus.COMPLETED, response.getStatus());
    }

    @Test
    void shouldNotAllowUserToUpdateTaskStatus() {
        assertThrows(SecurityException.class, () -> taskService.updateTaskStatus(testTask.getId(), "COMPLETED", regularUser));
    }

    @Test
    void shouldAllowAdminToAssignTask() {
        TaskResponse response = taskService.assignTask(testTask.getId(), regularUser.getId(), adminUser);
        assertEquals(regularUser.getId(), response.getAssigneeId());
    }

    @Test
    void shouldNotAllowUserToAssignTask() {
        assertThrows(SecurityException.class, () -> taskService.assignTask(testTask.getId(), adminUser.getId(), regularUser));
    }

}
