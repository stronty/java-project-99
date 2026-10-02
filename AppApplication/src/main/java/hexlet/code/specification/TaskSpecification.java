package hexlet.code.specification;

import hexlet.code.dto.TaskParamsDTO;
import hexlet.code.model.Task;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class TaskSpecification {

    public Specification<Task> build(TaskParamsDTO params) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (params.getTitleCont() != null && !params.getTitleCont().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + params.getTitleCont().toLowerCase() + "%"));
            }
            if (params.getAssigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), params.getAssigneeId()));
            }
            if (params.getStatus() != null && !params.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("taskStatus").get("slug"), params.getStatus()));
            }
            if (params.getLabelId() != null) {
                predicates.add(cb.equal(root.join("labels").get("id"), params.getLabelId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}