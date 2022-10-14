package com.kirars.task.tracker.store.model;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Accessors(chain = true)
@Table(name = "task")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long id;

    String name;

    String description;

    @Builder.Default
    Boolean completed = false;

    @ManyToOne
    TaskStateEntity taskState;

    @Builder.Default
    Instant createdAt = Instant.now();

    @Builder.Default
    Instant updatedAt = Instant.now();
}
