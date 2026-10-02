package hexlet.code.service;

import hexlet.code.dto.TaskStatusCreateDTO;
import hexlet.code.dto.TaskStatusDTO;
import hexlet.code.dto.TaskStatusUpdateDTO;
import hexlet.code.mapper.TaskStatusMapper;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskStatusService {

    private final TaskStatusRepository statusRepository;
    private final TaskRepository taskRepository;
    private final TaskStatusMapper statusMapper;

    @Transactional(readOnly = true)
    public List<TaskStatusDTO> getAll() {
        return statusRepository.findAll().stream().map(statusMapper::map).toList();
    }

    @Transactional(readOnly = true)
    public TaskStatusDTO get(Long id) {
        return statusMapper.map(find(id));
    }

    @Transactional
    public TaskStatusDTO create(TaskStatusCreateDTO dto) {
        checkUnique(dto.getName(), dto.getSlug(), null);
        var status = statusMapper.map(dto);
        return statusMapper.map(statusRepository.save(status));
    }

    @Transactional
    public TaskStatusDTO update(Long id, TaskStatusUpdateDTO dto) {
        var status = find(id);
        checkUnique(dto.getName(), dto.getSlug(), id);
        statusMapper.update(dto, status);
        return statusMapper.map(statusRepository.save(status));
    }

    @Transactional
    public void delete(Long id) {
        if (!statusRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found: " + id);
        }
        if (taskRepository.existsByTaskStatusId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Status has tasks");
        }
        statusRepository.deleteById(id);
    }

    private TaskStatus find(Long id) {
        return statusRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found: " + id));
    }

    private void checkUnique(String name, String slug, Long selfId) {
        if (name != null) {
            statusRepository.findByName(name).ifPresent(existing -> {
                if (!existing.getId().equals(selfId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Status name already exists");
                }
            });
        }
        if (slug != null) {
            statusRepository.findBySlug(slug).ifPresent(existing -> {
                if (!existing.getId().equals(selfId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Status slug already exists");
                }
            });
        }
    }
}