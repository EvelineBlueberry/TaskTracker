package com.kirars.task.tracker.store.repository;

import com.kirars.task.tracker.store.model.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
}
