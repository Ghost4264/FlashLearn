package com.flashlearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCreateDeckRequest {
    @NotBlank(message = "Название колоды обязательно")
    private String title;
    private String description;
    private String categoryName;
}
