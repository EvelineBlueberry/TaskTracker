package com.kirars.task.tracker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskDto {

    @NonNull
    Long id;

    @NonNull
    String name;

    @NonNull
    String description;

    @NonNull
    Boolean completed;

    @NonNull
    @JsonProperty("created_at")
    Instant createdAt;

    @NonNull
    @JsonProperty("updated_at")
    Instant updatedAt;
}
