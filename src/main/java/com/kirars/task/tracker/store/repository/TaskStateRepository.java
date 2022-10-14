package com.kirars.task.tracker.store.repository;

import com.kirars.task.tracker.store.model.TaskStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskStateRepository extends JpaRepository<TaskStateEntity, Long> {
    Optional<TaskStateEntity> findTaskStateEntityByIdAndNameContainsIgnoreCase(Long projectId, String taskStateName);
}
