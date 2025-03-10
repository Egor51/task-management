package com.dev.taskmanagement.repository;

import com.dev.taskmanagement.model.Task;
import com.dev.taskmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("""
            SELECT DISTINCT t FROM Task t 
            WHERE t.author = :user 
            OR t.assignee = :user
            ORDER BY t.createdAt DESC
            """)
    List<Task> findByAuthorOrAssignee(@Param("user") User user);
}