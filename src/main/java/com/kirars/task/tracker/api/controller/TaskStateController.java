package com.kirars.task.tracker.api.controller;

import com.kirars.task.tracker.api.controller.helper.ControllerHelper;
import com.kirars.task.tracker.api.dto.AckDto;
import com.kirars.task.tracker.api.dto.TaskStateDto;
import com.kirars.task.tracker.api.exception.BadRequestException;
import com.kirars.task.tracker.api.factory.TaskStateDtoFactory;
import com.kirars.task.tracker.store.model.ProjectEntity;
import com.kirars.task.tracker.store.model.TaskStateEntity;
import com.kirars.task.tracker.store.repository.TaskStateRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskStateController {

    TaskStateRepository taskStateRepository;
    TaskStateDtoFactory taskStateDtoFactory;
    ControllerHelper controllerHelper;

    public static final String GET_TASK_STATES = "/api/projects/{project_id}/task-states";
    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/task-states/new";
    public static final String UPDATE_TASK_STATE = "/api/task-states/{task_state_id}";
    public static final String CHANGE_TASK_STATE_POSITION = "/api/task-states/{task_state_id}/position/change";
    public static final String DELETE_TASK_STATE = "/api/task-states/{task_state_id}";

    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(@PathVariable(name = "project_id") Long projectId) {

        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        return project
                .getTaskStates()
                .stream()
                .map(taskStateDtoFactory::makeTaskStateDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskState(@PathVariable(name = "project_id") Long projectId,
                                        @RequestParam(name = "task_state_name") String taskStateName) {

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task state name can't be empty");
        }

        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        Optional<TaskStateEntity> optionalAnotherTaskState = Optional.empty();

        for (TaskStateEntity taskState : project.getTaskStates()) {
            if (taskState.getName().equalsIgnoreCase(taskStateName)) {
                throw new BadRequestException("Task state " + taskStateName + " already exists");
            }

            if (taskState.getRightTaskState().isEmpty()) {
                optionalAnotherTaskState = Optional.of(taskState);
                break;
            }
        }


        TaskStateEntity taskState = taskStateRepository.saveAndFlush(
                TaskStateEntity.builder()
                        .name(taskStateName)
                        .project(project)
                        .build()
        );

        optionalAnotherTaskState
                .ifPresent(anotherTaskState -> {
                    taskState.setLeftTaskState(anotherTaskState);
                    anotherTaskState.setRightTaskState(taskState);
                    taskStateRepository.saveAndFlush(anotherTaskState);
                });

        final TaskStateEntity savedTaskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeTaskStateDto(savedTaskState);
    }

    @PatchMapping(UPDATE_TASK_STATE)
    public TaskStateDto updateTaskState(@PathVariable(name = "task_state_id") Long taskStateId,
                                        @RequestParam(name = "task_state_name") String taskStateName) {

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task state name can't be empty");
        }

        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        taskStateRepository
                .findTaskStateEntityByIdAndNameContainsIgnoreCase(taskState.getProject().getId(), taskStateName)
                .filter(anotherTaskState -> !anotherTaskState.getId().equals(taskStateId))
                .ifPresent(anotherTaskState -> {
                    throw new BadRequestException("Task state " + taskStateName + " already exists");
                });

        taskState.setName(taskStateName);

        taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeTaskStateDto(taskState);
    }

    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskStatePosition(@PathVariable(name = "task_state_id") Long taskStateId,
                                                @RequestParam(name = "left_task_state_id", required = false) Optional<Long> optionalLeftTaskStateId) {

        TaskStateEntity changeTaskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        ProjectEntity project = changeTaskState.getProject();

        Optional<Long> optionalOldLeftTaskStateId = changeTaskState
                .getLeftTaskState()
                .map(TaskStateEntity::getId);

        if (optionalOldLeftTaskStateId.equals(optionalLeftTaskStateId)) {
            taskStateDtoFactory.makeTaskStateDto(changeTaskState);
        }

        Optional<TaskStateEntity> optionalNewLeftTaskState = optionalLeftTaskStateId
                .map(leftTaskStateId -> {

                    if (taskStateId.equals(leftTaskStateId)) {
                        throw new BadRequestException("Left task state id equals changed task state id");
                    }

                    TaskStateEntity leftTaskStateEntity = controllerHelper.getTaskStateOrThrowException(leftTaskStateId);

                    if (!project.getId().equals(leftTaskStateEntity.getProject().getId())) {
                        throw new BadRequestException("Task state position can be changed within the same project");
                    }

                    return leftTaskStateEntity;
                });

        Optional<TaskStateEntity> optionalNewRightTaskState;

        if (optionalNewLeftTaskState.isEmpty()) {

            optionalNewRightTaskState = project
                    .getTaskStates()
                    .stream()
                    .filter(anotherTaskState -> anotherTaskState.getLeftTaskState().isEmpty())
                    .findFirst();
        } else {

            optionalNewRightTaskState = optionalNewLeftTaskState
                    .get()
                    .getRightTaskState();

        }

        replaceOldTaskStates(changeTaskState);

        if (optionalNewLeftTaskState.isPresent()) {
            TaskStateEntity newLeftTaskState = optionalNewLeftTaskState.get();

            newLeftTaskState.setRightTaskState(changeTaskState);

            changeTaskState.setLeftTaskState(newLeftTaskState);
        } else {
            changeTaskState.setLeftTaskState(null);
        }

        if (optionalNewRightTaskState.isPresent()) {
            TaskStateEntity newRightTaskState = optionalNewRightTaskState.get();

            newRightTaskState.setLeftTaskState(changeTaskState);

            changeTaskState.setRightTaskState(newRightTaskState);
        } else {
            changeTaskState.setRightTaskState(null);
        }

        taskStateRepository.saveAndFlush(changeTaskState);

        optionalNewLeftTaskState
                .ifPresent(taskStateRepository::saveAndFlush);

        optionalNewRightTaskState
                .ifPresent(taskStateRepository::saveAndFlush);

        return taskStateDtoFactory.makeTaskStateDto(changeTaskState);
    }

    @DeleteMapping(DELETE_TASK_STATE)
    public AckDto deleteTaskState(@PathVariable(name = "task_state_id") Long taskStateId) {
        TaskStateEntity changeTaskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        replaceOldTaskStates(changeTaskState);

        taskStateRepository.delete(changeTaskState);
        return AckDto.makeDefault(true);
    }

    private void replaceOldTaskStates(TaskStateEntity changeTaskState) {
        Optional<TaskStateEntity> optionalOldLeftTaskState = changeTaskState.getLeftTaskState();
        Optional<TaskStateEntity> optionalOldRightTaskState = changeTaskState.getRightTaskState();

        optionalOldLeftTaskState.ifPresent(it -> {
            it.setRightTaskState(optionalOldRightTaskState.orElse(null));
            taskStateRepository.saveAndFlush(it);
        });

        optionalOldRightTaskState.ifPresent(it -> {
            it.setLeftTaskState(optionalOldLeftTaskState.orElse(null));
            taskStateRepository.saveAndFlush(it);
        });
    }

}