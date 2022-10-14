package com.kirars.task.tracker.api.factory;

import com.kirars.task.tracker.api.dto.ProjectDto;
import com.kirars.task.tracker.store.model.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectDtoFactory {

    public ProjectDto makeProjectDto(ProjectEntity entity) {
        return ProjectDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
