package com.kirars.task.tracker.api.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AckDto {

    Boolean answer;

    public static AckDto makeDefault(Boolean answer) {
        return builder()
                .answer(answer)
                .build();
    }
}
