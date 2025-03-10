package com.dev.taskmanagement.controller;

import com.dev.taskmanagement.dto.comment.CommentRequest;
import com.dev.taskmanagement.dto.comment.CommentResponse;
import com.dev.taskmanagement.model.User;
import com.dev.taskmanagement.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Comments", description = "APIs for managing task comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{taskId}/comments")
    @Operation(
        summary = "Add a comment to a task",
        description = "Creates a new comment for a specific task. Only task author, assignee, or admin can comment."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comment added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user doesn't have permission to comment on this task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN') or @taskService.isTaskAccessible(#taskId, authentication.principal)")
    public ResponseEntity<Void> addComment(
            @Parameter(description = "ID of the task to comment on", required = true)
            @PathVariable Long taskId,
            @Parameter(description = "Comment details", required = true)
            @Valid @RequestBody CommentRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser) {
        commentService.addComment(taskId, request, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{taskId}")
    @Operation(
        summary = "Get task comments",
        description = "Retrieves all comments for a specific task. Only accessible by task author, assignee, or admin."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Comments retrieved successfully",
            content = @Content(schema = @Schema(implementation = CommentResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user doesn't have access to this task's comments"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN') or @commentService.isCommentAllowed(#taskId, authentication.principal)")
    public ResponseEntity<List<CommentResponse>> getComments(
            @Parameter(description = "ID of the task to get comments for", required = true)
            @PathVariable Long taskId) {
        return ResponseEntity.ok(commentService.getComments(taskId));
    }

    @DeleteMapping("/{commentId}")
    @Operation(
        summary = "Delete a comment",
        description = "Deletes a specific comment. Only the comment author or admin can delete the comment."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user doesn't have permission to delete this comment"),
        @ApiResponse(responseCode = "404", description = "Comment not found")
    })
    @PreAuthorize("hasRole('ROLE_ADMIN') or @commentService.isCommentAuthor(#commentId, authentication.principal)")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "ID of the comment to delete", required = true)
            @PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
