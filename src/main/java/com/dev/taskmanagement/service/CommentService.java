package com.dev.taskmanagement.service;

import com.dev.taskmanagement.dto.comment.CommentRequest;
import com.dev.taskmanagement.dto.comment.CommentResponse;
import com.dev.taskmanagement.exception.ResourceNotFoundException;
import com.dev.taskmanagement.model.Comment;
import com.dev.taskmanagement.model.Task;
import com.dev.taskmanagement.model.User;
import com.dev.taskmanagement.repository.CommentRepository;
import com.dev.taskmanagement.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public CommentResponse addComment(Long taskId, CommentRequest request, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!task.getAuthor().equals(currentUser) && (task.getAssignee() == null || !task.getAssignee().equals(currentUser))) {
            throw new SecurityException("You do not have permission to comment on this task");
        }

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setTask(task);
        comment.setAuthor(currentUser);

        return convertToResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        return task.getComments().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        commentRepository.delete(comment);
    }

    public boolean isCommentAllowed(Long taskId, User currentUser) {
        Task task = taskRepository.findById(taskId).orElse(null);
        return task != null && (task.getAuthor().equals(currentUser) || task.getAssignee().equals(currentUser));
    }

    public boolean isCommentAuthor(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        return comment != null && comment.getAuthor().equals(currentUser);
    }

    private CommentResponse convertToResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor().getEmail(),
                comment.getCreatedAt()
        );
    }
}
