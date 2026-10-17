package hexlet.code.service;

import hexlet.code.component.PatchSupport;
import hexlet.code.dto.label.LabelCreateDTO;
import hexlet.code.dto.label.LabelDTO;
import hexlet.code.dto.label.LabelUpdateDTO;
import hexlet.code.exception.DuplicateResourceException;
import hexlet.code.exception.ResourceInUseException;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.mapper.LabelMapper;
import hexlet.code.model.Label;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LabelService {

    private static final Logger log = LoggerFactory.getLogger(LabelService.class);
    private static final List<String> DEFAULT_LABEL_NAMES = List.of("feature", "bug");

    private final LabelRepository labelRepository;
    private final TaskRepository taskRepository;
    private final LabelMapper labelMapper;
    private final PatchSupport patchSupport;

    public List<LabelDTO> findAll() {
        return labelRepository.findAll(Sort.by("id")).stream().map(labelMapper::toDto).toList();
    }

    public LabelDTO findById(Long id) {
        return labelMapper.toDto(getOrThrow(id));
    }

    @Transactional
    public LabelDTO create(LabelCreateDTO dto) {
        if (labelRepository.existsByName(dto.name())) {
            throw new DuplicateResourceException(
                    "Label with name '%s' already exists".formatted(dto.name()));
        }
        var label = labelMapper.toEntity(dto);
        var saved = labelRepository.save(label);
        return labelMapper.toDto(saved);
    }

    @Transactional
    public LabelDTO update(Long id, JsonNode patch) {
        patchSupport.requireObject(patch);
        var current = getOrThrow(id);
        var dto = patchSupport.apply(labelMapper.toUpdateDto(current), patch, LabelUpdateDTO.class);

        if (dto.getName() != null
                && !Objects.equals(dto.getName(), current.getName())
                && labelRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException(
                    "Label with name '%s' already exists".formatted(dto.getName()));
        }

        labelMapper.update(dto, current);
        return labelMapper.toDto(labelRepository.save(current));
    }

    @Transactional
    public void delete(Long id) {
        var label = getOrThrow(id);
        if (taskRepository.existsByLabelsId(label.getId())) {
            throw new ResourceInUseException(
                    "Label with id %d is used by tasks and cannot be deleted"
                            .formatted(label.getId()));
        }
        labelRepository.delete(label);
    }

    @Transactional
    public void createDefaultsIfMissing() {
        for (String name : DEFAULT_LABEL_NAMES) {
            if (labelRepository.existsByName(name)) {
                log.info("Label already exists, skipping: {}", name);
                continue;
            }
            create(new LabelCreateDTO(name));
            log.info("Label created: {}", name);
        }
    }

    private Label getOrThrow(Long id) {
        return labelRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Label with id %d not found".formatted(id)));
    }
}
