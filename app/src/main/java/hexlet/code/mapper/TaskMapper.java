package hexlet.code.mapper;

import hexlet.code.dto.TaskCreateDTO;
import hexlet.code.dto.TaskDTO;
import hexlet.code.dto.TaskUpdateDTO;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class TaskMapper {

    @Autowired
    private TaskStatusRepository statusRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LabelRepository labelRepository;

    @Mapping(target = "title", source = "name")
    @Mapping(target = "content", source = "description")
    @Mapping(target = "status", source = "taskStatus.slug")
    @Mapping(target = "assigneeId", source = "assignee.id")
    @Mapping(target = "taskLabelIds", source = "labels", qualifiedByName = "labelsToIds")
    public abstract TaskDTO map(Task task);

    @Mapping(target = "name", source = "title")
    @Mapping(target = "description", source = "content")
    @Mapping(target = "taskStatus", source = "status", qualifiedByName = "slugToStatus")
    @Mapping(target = "assignee", source = "assigneeId", qualifiedByName = "idToUser")
    @Mapping(target = "labels", source = "taskLabelIds", qualifiedByName = "idsToLabels")
    public abstract Task map(TaskCreateDTO dto);

    @Mapping(target = "name", source = "title")
    @Mapping(target = "description", source = "content")
    @Mapping(target = "taskStatus", source = "status", qualifiedByName = "slugToStatus")
    @Mapping(target = "assignee", source = "assigneeId", qualifiedByName = "idToUser")
    @Mapping(target = "labels", source = "taskLabelIds", qualifiedByName = "idsToLabels")
    public abstract void update(TaskUpdateDTO dto, @MappingTarget Task task);

    @Named("slugToStatus")
    TaskStatus slugToStatus(String slug) {
        return statusRepository.findBySlug(slug).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: " + slug));
    }

    @Named("idToUser")
    User idToUser(Long id) {
        return userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown assignee: " + id));
    }

    @Named("idsToLabels")
    Set<Label> idsToLabels(Set<Long> ids) {
        if (ids == null) {
            return null;
        }
        var labels = new HashSet<>(labelRepository.findAllById(ids));
        if (labels.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown label id in " + ids);
        }
        return labels;
    }

    @Named("labelsToIds")
    Set<Long> labelsToIds(Set<Label> labels) {
        if (labels == null) {
            return Set.of();
        }
        return labels.stream().map(Label::getId).collect(Collectors.toSet());
    }
}