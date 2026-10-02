package hexlet.code.bootstrap;

import hexlet.code.model.Label;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import hexlet.code.model.Task;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import hexlet.code.repository.TaskRepository;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private static final String ADMIN_EMAIL = "hexlet@example.com";

    private record StatusSeed(String slug, String name) { }

    private static final List<StatusSeed> DEFAULT_STATUSES = List.of(
            new StatusSeed("draft", "Draft"),
            new StatusSeed("to_review", "To Review"),
            new StatusSeed("to_be_fixed", "To BeFixed"),
            new StatusSeed("to_publish", "To Publish"),
            new StatusSeed("published", "Published"));

    private static final List<String> DEFAULT_LABELS = List.of("feature", "bug");

    private final UserRepository userRepository;
    private final TaskStatusRepository statusRepository;
    private final LabelRepository labelRepository;
    private final PasswordEncoder encoder;
    private final TaskRepository taskRepository;

    @Override
    public void run(ApplicationArguments args) {
        createAdmin();
        createStatuses();
        createLabels();
        createTasks();
    }

    private void createTasks() {
        if (taskRepository.count() > 0) {
            return;
        }
        var admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        for (var seed : DEFAULT_STATUSES) {
            var task = new Task();
            task.setName("Task: " + seed.name());
            task.setDescription("Default task for the " + seed.name() + " column");
            task.setTaskStatus(statusRepository.findBySlug(seed.slug()).orElseThrow());
            task.setAssignee(admin);
            taskRepository.save(task);
        }
    }

    private void createAdmin() {
        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            var admin = new User();
            admin.setEmail(ADMIN_EMAIL);
            admin.setPassword(encoder.encode("qwerty"));
            userRepository.save(admin);
        }
    }

    private void createStatuses() {
        for (var seed : DEFAULT_STATUSES) {
            if (statusRepository.findBySlug(seed.slug()).isPresent()
                    || statusRepository.findByName(seed.name()).isPresent()) {
                continue;
            }
            var status = new TaskStatus();
            status.setSlug(seed.slug());
            status.setName(seed.name());
            statusRepository.save(status);
        }
    }

    private void createLabels() {
        for (var name : DEFAULT_LABELS) {
            if (labelRepository.findByName(name).isPresent()) {
                continue;
            }
            var label = new Label();
            label.setName(name);
            labelRepository.save(label);
        }
    }
}