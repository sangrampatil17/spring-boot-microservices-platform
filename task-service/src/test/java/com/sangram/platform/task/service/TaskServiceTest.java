package com.sangram.platform.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sangram.platform.task.model.Task;
import com.sangram.platform.task.model.TaskRequest;
import com.sangram.platform.task.model.TaskStatus;
import com.sangram.platform.task.repo.TaskRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository repository;

    @InjectMocks
    TaskService service;

    @Test
    void createDefaultsStatusToTodoAndSetsOwner() {
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task created = service.create(new TaskRequest("Write docs", null, null, null), "alice");

        assertEquals(TaskStatus.TODO, created.getStatus());
        assertEquals("alice", created.getOwner());
        assertEquals(List.of(), created.getTags());
    }

    @Test
    void getThrowsWhenTaskMissing() {
        when(repository.findById("42")).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> service.get("42"));
    }

    @Test
    void updateChangesFieldsAndKeepsOwner() {
        Task existing = new Task();
        existing.setId("1");
        existing.setOwner("bob");
        existing.setTitle("old");
        when(repository.findById("1")).thenReturn(Optional.of(existing));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = service.update("1", new TaskRequest("new", "desc", TaskStatus.DONE, List.of("a")));

        assertEquals("new", updated.getTitle());
        assertEquals(TaskStatus.DONE, updated.getStatus());
        assertEquals("bob", updated.getOwner());
    }

    @Test
    void searchPrefersTextQueryOverStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(new Task()));
        when(repository.findByTitleContainingIgnoreCase("doc", pageable)).thenReturn(page);

        assertEquals(page, service.search("doc", TaskStatus.DONE, pageable));
        verify(repository, never()).findByStatus(any(), any());
    }
}
