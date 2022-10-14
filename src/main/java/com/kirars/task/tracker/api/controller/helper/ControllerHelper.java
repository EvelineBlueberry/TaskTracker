package com.kirars.task.tracker.api.controller.helper;

import com.kirars.task.tracker.api.exception.NotFoundException;
import com.kirars.task.tracker.store.model.ProjectEntity;
import com.kirars.task.tracker.store.model.TaskEntity;
import com.kirars.task.tracker.store.model.TaskStateEntity;
import com.kirars.task.tracker.store.repository.ProjectRepository;
import com.kirars.task.tracker.store.repository.TaskRepository;
import com.kirars.task.tracker.store.repository.TaskStateRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
@Transactional
public class ControllerHelper {

    ProjectRepository projectRepository;
    TaskStateRepository taskStateRepository;
    TaskRepository taskRepository;

    public ProjectEntity getProjectOrThrowException(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project with id " + projectId + " doesn't exists"));
    }

    public TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() -> new NotFoundException("Task state with " + taskStateId + " id doesn't exists"));
    }

    public TaskEntity getTaskOrThrowException(Long taskId) {
        return taskRepository
                .findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task with " + taskId + " id doesn't exists"));
    }
}
