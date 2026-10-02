package hexlet.code.mapper;

import hexlet.code.dto.TaskCreateDTO;
import hexlet.code.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TaskMapperTest {

    @Autowired
    private TaskMapper mapper;

    @Test
    void nullInputsGiveNull() {
        assertNull(mapper.map((Task) null));
        assertNull(mapper.map((TaskCreateDTO) null));
    }

    @Test
    void updateWithNullDtoChangesNothing() {
        var task = new Task();
        task.setName("Same");

        mapper.update(null, task);

        assertEquals("Same", task.getName());
    }

    @Test
    void taskWithoutStatusAndAssigneeMapsToNulls() {
        var task = new Task();
        task.setName("Bare");

        var dto = mapper.map(task);

        assertEquals("Bare", dto.getTitle());
        assertNull(dto.getStatus());
        assertNull(dto.getAssigneeId());
        assertTrue(dto.getTaskLabelIds().isEmpty());
    }
}