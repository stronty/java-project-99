package hexlet.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabelCreateDTO {
    @NotBlank
    @Size(min = 3, max = 1000)
    private String name;
}