package com.kirars.task.tracker.api.factory;

import com.kirars.task.tracker.api.dto.TaskDto;
import com.kirars.task.tracker.store.model.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskDtoFactory {

    public TaskDto makeTaskDto(TaskEntity entity) {
        return TaskDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .completed(entity.getCompleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
