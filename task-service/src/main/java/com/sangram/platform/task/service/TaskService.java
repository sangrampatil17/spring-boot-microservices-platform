package com.sangram.platform.task.service;

import com.sangram.platform.task.model.Task;
import com.sangram.platform.task.model.TaskRequest;
import com.sangram.platform.task.model.TaskStatus;
import com.sangram.platform.task.repo.TaskRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public Task create(TaskRequest request, String owner) {
        Task task = new Task();
        apply(task, request);
        task.setOwner(owner);
        return repository.save(task);
    }

    public Task get(String id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task update(String id, TaskRequest request) {
        Task task = get(id);
        apply(task, request);
        task.setUpdatedAt(Instant.now());
        return repository.save(task);
    }

    public void delete(String id) {
        repository.delete(get(id));
    }

    public Page<Task> search(String query, TaskStatus status, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return repository.findByTitleContainingIgnoreCase(query, pageable);
        }
        if (status != null) {
            return repository.findByStatus(status, pageable);
        }
        return repository.findAll(pageable);
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status() == null ? TaskStatus.TODO : request.status());
        task.setTags(request.tags() == null ? List.of() : request.tags());
    }
}
