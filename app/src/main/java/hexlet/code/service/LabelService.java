package hexlet.code.service;

import hexlet.code.dto.LabelCreateDTO;
import hexlet.code.dto.LabelDTO;
import hexlet.code.dto.LabelUpdateDTO;
import hexlet.code.mapper.LabelMapper;
import hexlet.code.model.Label;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;
    private final TaskRepository taskRepository;
    private final LabelMapper labelMapper;

    @Transactional(readOnly = true)
    public List<LabelDTO> getAll() {
        return labelRepository.findAll().stream().map(labelMapper::map).toList();
    }

    @Transactional(readOnly = true)
    public LabelDTO get(Long id) {
        return labelMapper.map(find(id));
    }

    @Transactional
    public LabelDTO create(LabelCreateDTO dto) {
        checkNameFree(dto.getName(), null);
        var label = labelMapper.map(dto);
        return labelMapper.map(labelRepository.save(label));
    }

    @Transactional
    public LabelDTO update(Long id, LabelUpdateDTO dto) {
        var label = find(id);
        if (dto.getName() != null) {
            checkNameFree(dto.getName(), id);
        }
        labelMapper.update(dto, label);
        return labelMapper.map(labelRepository.save(label));
    }

    @Transactional
    public void delete(Long id) {
        if (!labelRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Label not found: " + id);
        }
        if (taskRepository.existsByLabelsId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Label is used by tasks");
        }
        labelRepository.deleteById(id);
    }

    private Label find(Long id) {
        return labelRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Label not found: " + id));
    }

    private void checkNameFree(String name, Long selfId) {
        labelRepository.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(selfId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Label name already exists");
            }
        });
    }
}