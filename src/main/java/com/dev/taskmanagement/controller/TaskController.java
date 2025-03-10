package com.dev.taskmanagement.controller;

import com.dev.taskmanagement.dto.AssignTaskRequest;
import com.dev.taskmanagement.dto.task.TaskRequest;
import com.dev.taskmanagement.dto.task.TaskResponse;
import com.dev.taskmanagement.model.User;
import com.dev.taskmanagement.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Task Management", description = "APIs for managing tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(
        summary = "Create a new task",
        description = "Creates a new task with the provided details. The task will be assigned to the current user as the author."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user doesn't have required role")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
    public ResponseEntity<TaskResponse> createTask(
            @Parameter(description = "Task details", required = true)
            @Valid @RequestBody TaskRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.createTask(request, currentUser.getId()));
    }

    @GetMapping("/{taskId}")
    @Operation(
        summary = "Get task by ID",
        description = "Retrieves task details by ID. User must be either an admin, the task author, or the assignee."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task found successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user doesn't have access to this task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN') or @taskService.isTaskAccessible(#taskId, authentication.principal)")
    public ResponseEntity<TaskResponse> getTaskById(
            @Parameter(description = "ID of the task to retrieve", required = true)
            @PathVariable Long taskId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.getTaskById(taskId, currentUser));
    }

    @PutMapping("/{taskId}")
    @Operation(
        summary = "Update a task",
        description = "Updates task details. Admin can update all fields, assignee can only update status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user doesn't have permission to edit this task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN') or @taskService.isTaskEditableByUser(#taskId, authentication.principal)")
    public ResponseEntity<TaskResponse> updateTask(
            @Parameter(description = "ID of the task to update", required = true)
            @PathVariable Long taskId,
            @Parameter(description = "Updated task details", required = true)
            @Valid @RequestBody TaskRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request, currentUser));
    }

    @DeleteMapping("/{taskId}")
    @Operation(
        summary = "Delete a task",
        description = "Deletes a task by ID. Only administrators can delete tasks."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - only admins can delete tasks"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "ID of the task to delete", required = true)
            @PathVariable Long taskId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {
        taskService.deleteTask(taskId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
        summary = "Get all tasks",
        description = "Retrieves a paginated list of tasks. Admins see all tasks, users see only their tasks (as author or assignee)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user doesn't have required role")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
    public ResponseEntity<List<TaskResponse>> getAllTasks(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.getAllTasks(page, size, currentUser));
    }

    @PatchMapping("/{taskId}/status")
    @Operation(
        summary = "Update task status",
        description = "Updates the status of a task. Available statuses: PENDING, IN_PROGRESS, COMPLETED, CANCELLED"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user doesn't have permission to update this task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN') or @taskService.isTaskAccessible(#taskId, authentication.principal)")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @Parameter(description = "ID of the task to update", required = true)
            @PathVariable Long taskId,
            @Parameter(description = "New status value (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)", required = true)
            @RequestParam String status,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.updateTaskStatus(taskId, status, currentUser));
    }

    @PatchMapping("/{taskId}/assign")
    @Operation(
        summary = "Assign a user to a task",
        description = "Assigns a user as the task assignee. Only administrators can assign tasks."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid assignee ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - only admins can assign tasks"),
        @ApiResponse(responseCode = "404", description = "Task or assignee not found")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<TaskResponse> assignTask(
            @Parameter(description = "ID of the task to assign", required = true)
            @PathVariable Long taskId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID of the user to assign to the task", required = true)
            @Valid @RequestBody AssignTaskRequest request) {
        return ResponseEntity.ok(taskService.assignTask(taskId, request.assigneeId(), currentUser));
    }
}
