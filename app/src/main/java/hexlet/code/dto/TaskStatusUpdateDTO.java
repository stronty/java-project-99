package hexlet.code.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusUpdateDTO {
    @Pattern(regexp = ".*\\S.*")
    private String name;
    @Pattern(regexp = ".*\\S.*")
    private String slug;
}