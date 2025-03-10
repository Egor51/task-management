package com.dev.taskmanagement.service;

import com.dev.taskmanagement.dto.task.TaskRequest;
import com.dev.taskmanagement.dto.task.TaskResponse;
import com.dev.taskmanagement.exception.ResourceNotFoundException;
import com.dev.taskmanagement.model.Role;
import com.dev.taskmanagement.model.Task;
import com.dev.taskmanagement.model.TaskStatus;
import com.dev.taskmanagement.model.User;
import com.dev.taskmanagement.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для управления задачами в системе.
 * Реализует основную бизнес-логику работы с задачами, включая создание,
 * обновление, удаление и поиск задач. Также обеспечивает проверку прав доступа
 * и применяет кэширование для оптимизации производительности.
 *
 * <p>Основные возможности:</p>
 * <ul>
 *   <li>Управление жизненным циклом задач (CRUD операции)</li>
 *   <li>Проверка прав доступа для различных операций</li>
 *   <li>Кэширование часто запрашиваемых данных</li>
 *   <li>Пагинация результатов</li>
 * </ul>
 *
 * @see Task
 * @see User
 * @see TaskStatus
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private static final String ADMIN_ONLY_MESSAGE = "Only admins can perform this operation";
    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to access this task";
    private static final String MODIFICATION_DENIED_MESSAGE = "You do not have permission to modify this task";
    private static final String TASK_NOT_FOUND_MESSAGE = "Task not found";

    private final TaskRepository taskRepository;
    private final UserService userService;

    /**
     * Создает новую задачу в системе.
     * Метод создает задачу с указанными параметрами и назначает ей автора.
     * После создания задача сохраняется в базе данных и кэш очищается.
     *
     * @param request Данные для создания задачи
     * @param authorId ID пользователя, создающего задачу
     * @return TaskResponse с данными созданной задачи
     * @throws ResourceNotFoundException если автор не найден
     */
    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse createTask(TaskRequest request, Long authorId) {
        log.debug("Creating task for author ID: {}", authorId);
        
        User author = findUserById(authorId);
        User assignee = Optional.ofNullable(request.getAssigneeId())
                .map(this::findUserById)
                .orElse(null);

        Task task = buildTask(request, author, assignee);
        Task savedTask = taskRepository.save(task);
        log.debug("Task created with ID: {}", savedTask.getId());
        
        return convertToResponse(savedTask);
    }

    /**
     * Получает задачу по её идентификатору.
     * Проверяет права доступа текущего пользователя к задаче.
     * Результат кэшируется для оптимизации производительности.
     *
     * @param taskId ID задачи
     * @param currentUser Текущий пользователь
     * @return TaskResponse с данными задачи
     * @throws ResourceNotFoundException если задача не найдена
     * @throws SecurityException если у пользователя нет прав доступа
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "#taskId")
    public TaskResponse getTaskById(Long taskId, User currentUser) {
        Task task = findTaskById(taskId);
        validateTaskAccess(task, currentUser);
        return convertToResponse(task);
    }

    /**
     * Обновляет существующую задачу.
     * Проверяет права пользователя на модификацию задачи.
     * Обновляет кэш после изменения.
     *
     * @param taskId ID задачи
     * @param request Новые данные задачи
     * @param currentUser Текущий пользователь
     * @return TaskResponse с обновленными данными
     * @throws ResourceNotFoundException если задача не найдена
     * @throws SecurityException если у пользователя нет прав на модификацию
     */
    @Transactional
    @CachePut(value = "tasks", key = "#taskId")
    public TaskResponse updateTask(Long taskId, TaskRequest request, User currentUser) {
        Task task = findTaskById(taskId);
        validateTaskModification(task, currentUser);
        
        updateTaskFields(task, request, currentUser);
        Task updatedTask = taskRepository.save(task);
        log.debug("Task {} updated by user {}", taskId, currentUser.getEmail());
        
        return convertToResponse(updatedTask);
    }

    /**
     * Удаляет задачу из системы.
     * Только администраторы могут удалять задачи.
     * После удаления задача удаляется из кэша.
     *
     * @param taskId ID задачи
     * @param currentUser Текущий пользователь
     * @throws ResourceNotFoundException если задача не найдена
     * @throws SecurityException если пользователь не администратор
     */
    @Transactional
    @CacheEvict(value = "tasks", key = "#taskId")
    public void deleteTask(Long taskId, User currentUser) {
        validateAdminAccess(currentUser);
        Task task = findTaskById(taskId);
        taskRepository.delete(task);
        log.debug("Task {} deleted by admin {}", taskId, currentUser.getEmail());
    }

    /**
     * Получает список задач с пагинацией.
     * Для администраторов возвращает все задачи,
     * для обычных пользователей - только их задачи или задачи, где они исполнители.
     * Результаты кэшируются для каждого пользователя и параметров пагинации.
     *
     * @param page Номер страницы (начиная с 0)
     * @param size Размер страницы
     * @param currentUser Текущий пользователь
     * @return Список TaskResponse с учетом пагинации
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "'user_' + #currentUser.id + '_page_' + #page + '_size_' + #size")
    public List<TaskResponse> getAllTasks(int page, int size, User currentUser) {
        if (page < 0 || size <= 0) {
            return List.of();
        }
        
        log.debug("Fetching tasks for user: {}", currentUser.getEmail());
        List<Task> tasks = fetchTasksForUser(currentUser);
        return applyPagination(tasks, page, size).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Обновляет статус задачи.
     * Проверяет права пользователя на модификацию задачи.
     * Обновляет кэш после изменения.
     *
     * @param taskId ID задачи
     * @param status Новый статус
     * @param currentUser Текущий пользователь
     * @return TaskResponse с обновленными данными
     * @throws ResourceNotFoundException если задача не найдена
     * @throws SecurityException если у пользователя нет прав на модификацию
     * @throws IllegalArgumentException если статус невалидный
     */
    @Transactional
    @CachePut(value = "tasks", key = "#taskId")
    public TaskResponse updateTaskStatus(Long taskId, String status, User currentUser) {
        Task task = findTaskById(taskId);
        validateTaskModification(task, currentUser);
        
        TaskStatus newStatus = TaskStatus.valueOf(status.toUpperCase());
        task.setStatus(newStatus);
        Task updatedTask = taskRepository.save(task);
        log.debug("Task {} status updated to {} by {}", taskId, status, currentUser.getEmail());
        
        return convertToResponse(updatedTask);
    }

    /**
     * Назначает исполнителя задачи.
     * Только администраторы могут назначать исполнителей.
     * Обновляет кэш после изменения.
     *
     * @param taskId ID задачи
     * @param assigneeId ID нового исполнителя
     * @param currentUser Текущий пользователь
     * @return TaskResponse с обновленными данными
     * @throws ResourceNotFoundException если задача или исполнитель не найдены
     * @throws SecurityException если пользователь не администратор
     */
    @Transactional
    @CachePut(value = "tasks", key = "#taskId")
    public TaskResponse assignTask(Long taskId, Long assigneeId, User currentUser) {
        validateAdminAccess(currentUser);
        
        Task task = findTaskById(taskId);
        User assignee = findUserById(assigneeId);
        
        task.setAssignee(assignee);
        Task updatedTask = taskRepository.save(task);
        log.debug("Task {} assigned to user {} by admin {}", taskId, assigneeId, currentUser.getEmail());
        
        return convertToResponse(updatedTask);
    }

    private List<Task> fetchTasksForUser(User user) {
        return user.getRole().equals(Role.ROLE_ADMIN) 
                ? taskRepository.findAll()
                : taskRepository.findByAuthorOrAssignee(user);
    }

    private void updateTaskFields(Task task, TaskRequest request, User currentUser) {
        if (currentUser.getRole().equals(Role.ROLE_ADMIN)) {
            updateTaskAsAdmin(task, request);
        } else {
            task.setStatus(request.getStatus());
        }
    }

    private User findUserById(Long userId) {
        return userService.getUserById(userId);
    }

    private Task findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND_MESSAGE));
    }

    private void validateAdminAccess(User user) {
        if (!user.getRole().equals(Role.ROLE_ADMIN)) {
            throw new SecurityException(ADMIN_ONLY_MESSAGE);
        }
    }

    private void validateTaskAccess(Task task, User user) {
        if (!isTaskAccessibleInternal(task, user)) {
            throw new SecurityException(ACCESS_DENIED_MESSAGE);
        }
    }

    private void validateTaskModification(Task task, User user) {
        if (!user.getRole().equals(Role.ROLE_ADMIN) && 
            (task.getAssignee() == null || !task.getAssignee().equals(user))) {
            throw new SecurityException(MODIFICATION_DENIED_MESSAGE);
        }
    }

    public boolean isTaskAccessible(Long taskId, User user) {
        if (taskId == null || user == null) {
            return false;
        }
        Task task = findTaskById(taskId);
        return isTaskAccessibleInternal(task, user);
    }

    public boolean isTaskEditableByUser(Long taskId, User user) {
        if (taskId == null || user == null) {
            return false;
        }
        Task task = findTaskById(taskId);
        return user.getRole().equals(Role.ROLE_ADMIN) ||
               (task.getAssignee() != null && task.getAssignee().equals(user));
    }

    private boolean isTaskAccessibleInternal(Task task, User user) {
        return user.getRole().equals(Role.ROLE_ADMIN) ||
               task.getAuthor().equals(user) ||
               (task.getAssignee() != null && task.getAssignee().equals(user));
    }

    private List<Task> applyPagination(List<Task> tasks, int page, int size) {
        if (tasks.isEmpty()) {
            return tasks;
        }
        int start = page * size;
        if (start >= tasks.size()) {
            log.debug("Requested page {} is beyond available tasks (total: {})", page, tasks.size());
            return List.of();
        }
        return tasks.subList(start, Math.min(start + size, tasks.size()));
    }

    private Task buildTask(TaskRequest request, User author, User assignee) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setAuthor(author);
        task.setAssignee(assignee);
        return task;
    }

    private void updateTaskAsAdmin(Task task, TaskRequest request) {
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
    }

    private TaskResponse convertToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .authorId(task.getAuthor().getId())
                .authorName(formatUserName(task.getAuthor()))
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? formatUserName(task.getAssignee()) : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .dueDate(task.getDueDate())
                .build();
    }

    private String formatUserName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}

