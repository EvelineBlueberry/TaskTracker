package com.kirars.task.tracker.api.controller;

import com.kirars.task.tracker.api.controller.helper.ControllerHelper;
import com.kirars.task.tracker.api.dto.AckDto;
import com.kirars.task.tracker.api.dto.TaskDto;
import com.kirars.task.tracker.api.exception.BadRequestException;
import com.kirars.task.tracker.api.factory.TaskDtoFactory;
import com.kirars.task.tracker.store.model.TaskEntity;
import com.kirars.task.tracker.store.model.TaskStateEntity;
import com.kirars.task.tracker.store.repository.TaskRepository;
import com.kirars.task.tracker.store.repository.TaskStateRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskController {

    TaskDtoFactory taskDtoFactory;
    TaskRepository taskRepository;
    TaskStateRepository taskStateRepository;
    ControllerHelper controllerHelper;

    public static final String GET_TASKS = "/api/task-states/{task_state_id}/tasks";
    public static final String CREATE_TASK = "/api/task-states/{task_state_id}/tasks/new";
    public static final String UPDATE_TASK = "/api/tasks/{task_id}";
    public static final String SET_COMPLETED = "/api/tasks/{task_id}/completed";
    public static final String DELETE_TASK = "/api/tasks/{task_id}";

    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(@PathVariable(name = "task_state_id") Long taskStateId) {
        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);
        return taskState
                .getTasks()
                .stream()
                .map(taskDtoFactory::makeTaskDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK)
    public TaskDto createTask(@PathVariable(name = "task_state_id") Long taskStateId,
                              @RequestBody TaskDto taskDto) {
        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

       taskState.getTasks()
                .stream()
                .filter(task -> task.getName().equals(taskDto.getName()))
                .findAny()
                .ifPresent(task -> {
                    throw new BadRequestException("Task " + taskDto.getName() + " already exists");
                });

        TaskEntity task = TaskEntity.builder()
                .name(taskDto.getName())
                .description(taskDto.getDescription())
                .createdAt(Instant.now())
                .build();

        taskRepository.saveAndFlush(task);

        taskState.getTasks().add(task);

        taskStateRepository.saveAndFlush(taskState);

        return taskDtoFactory.makeTaskDto(task);
    }

    @PutMapping(UPDATE_TASK)
    public TaskDto updateTask(@PathVariable(name = "task_id") Long taskId,
                              @RequestBody TaskDto taskDto) {

        TaskEntity taskEntity = controllerHelper.getTaskOrThrowException(taskId);

        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskEntity.getTaskState().getId());

        taskState.getTasks()
                .stream()
                .filter(task -> task.getName().equals(taskDto.getName()))
                .findAny()
                .ifPresent(task -> {
                    throw new BadRequestException("Task " + taskDto.getName() + " already exists");
                });

        taskEntity
                .setName(taskDto.getName())
                .setDescription(taskDto.getDescription())
                .setCompleted(taskDto.getCompleted())
                .setUpdatedAt(Instant.now());

        taskRepository.saveAndFlush(taskEntity);

        return taskDtoFactory.makeTaskDto(taskEntity);
    }

    @PatchMapping(SET_COMPLETED)
    public AckDto completeTask(@PathVariable(name = "task_id") Long taskId) {
        TaskEntity taskEntity = controllerHelper.getTaskOrThrowException(taskId);
        taskEntity.setCompleted(true);
        taskRepository.saveAndFlush(taskEntity);
        return AckDto.makeDefault(true);
    }

    @DeleteMapping(DELETE_TASK)
    public AckDto deleteTask(@PathVariable(name = "task_id") Long taskId) {

        TaskEntity taskEntity = controllerHelper.getTaskOrThrowException(taskId);

        taskRepository.delete(taskEntity);

        return AckDto.makeDefault(true);
    }
}
