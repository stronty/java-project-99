package hexlet.code.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class TaskUpdateDTO {
    private Integer index;
    @JsonProperty("assignee_id")
    private Long assigneeId;
    @Size(min = 1)
    private String title;
    private String content;
    private String status;
    private Set<Long> taskLabelIds;
}