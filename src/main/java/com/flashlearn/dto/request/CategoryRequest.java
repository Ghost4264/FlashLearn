package com.flashlearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Название категории обязательно")
    @Size(max = 100, message = "Название не может быть длиннее 100 символов")
    private String name;
}
