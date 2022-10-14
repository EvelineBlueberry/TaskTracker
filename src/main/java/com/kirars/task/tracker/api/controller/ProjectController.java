package com.kirars.task.tracker.api.controller;

import com.kirars.task.tracker.api.controller.helper.ControllerHelper;
import com.kirars.task.tracker.api.dto.AckDto;
import com.kirars.task.tracker.api.dto.ProjectDto;
import com.kirars.task.tracker.api.exception.BadRequestException;
import com.kirars.task.tracker.api.factory.ProjectDtoFactory;
import com.kirars.task.tracker.store.model.ProjectEntity;
import com.kirars.task.tracker.store.repository.ProjectRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectController {

    ProjectRepository projectRepository;
    ProjectDtoFactory projectDtoFactory;
    ControllerHelper controllerHelper;

    public static final String FETCH_PROJECTS = "/api/projects";
    public static final String DELETE_PROJECT = "/api/projects/{project_id}";
    public static final String CREATE_OR_UPDATE_PROJECT = "/api/projects/new";

    @GetMapping(FETCH_PROJECTS)
    public List<ProjectDto> fetchProjects(
            @RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName) {
        optionalPrefixName = optionalPrefixName.filter(prefixName -> !prefixName.trim().isEmpty());

        Stream<ProjectEntity> projectStream = optionalPrefixName
                .map(projectRepository::streamAllByNameStartsWithIgnoreCase)
                .orElseGet(projectRepository::streamAllBy);

        return projectStream.map(projectDtoFactory::makeProjectDto).collect(Collectors.toList());
    }

    @PutMapping(CREATE_OR_UPDATE_PROJECT)
    public ProjectDto createOrUpdateProject(
            @RequestParam(value = "project_id", required = false) Optional<Long> optionalProjectId,
            @RequestParam(value = "project_name", required = false) Optional<String> optionalProjectName
            // Another params...
    ) {

        optionalProjectName = optionalProjectName.filter(projectName -> !projectName.trim().isEmpty());

        boolean isCreate = optionalProjectId.isEmpty();

        if (isCreate && optionalProjectName.isEmpty()) {
            throw new BadRequestException("Project name can't be empty");
        }

        final ProjectEntity project = optionalProjectId
                .map(controllerHelper::getProjectOrThrowException)
                .orElseGet(() -> ProjectEntity.builder().build());

        optionalProjectName
                .ifPresent(projectName -> {

                    projectRepository
                            .findByName(projectName)
                            .filter(anotherProject -> !Objects.equals(anotherProject.getId(), project.getId()))
                            .ifPresent(anotherProject -> {
                                throw new BadRequestException("Project with name " + projectName + " already exists");
                            });

                    project.setName(projectName);
                });

        final ProjectEntity savedProject = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(savedProject);
    }

    @DeleteMapping(DELETE_PROJECT)
    public AckDto deleteProject(@PathVariable("project_id") Long projectId) {
        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        projectRepository.deleteById(projectId);
        return AckDto.makeDefault(true);
    }

}
