package hexlet.code.service;

import hexlet.code.dto.TaskCreateDTO;
import hexlet.code.dto.TaskDTO;
import hexlet.code.dto.TaskParamsDTO;
import hexlet.code.dto.TaskUpdateDTO;
import hexlet.code.mapper.TaskMapper;
import hexlet.code.model.Task;
import hexlet.code.repository.TaskRepository;
import hexlet.code.specification.TaskSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    private final TaskSpecification specification;

    @Transactional(readOnly = true)
    public List<TaskDTO> getAll(TaskParamsDTO params) {
        return taskRepository.findAll(specification.build(params)).stream()
                .map(taskMapper::map)
                .toList();
    }
    @Transactional(readOnly = true)
    public TaskDTO get(Long id) {
        return taskMapper.map(find(id));
    }

    @Transactional
    public TaskDTO create(TaskCreateDTO dto) {
        var task = taskMapper.map(dto);
        return taskMapper.map(taskRepository.save(task));
    }

    @Transactional
    public TaskDTO update(Long id, TaskUpdateDTO dto) {
        var task = find(id);
        taskMapper.update(dto, task);
        return taskMapper.map(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id);
        }
        taskRepository.deleteById(id);
    }

    private Task find(Long id) {
        return taskRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
    }
}
