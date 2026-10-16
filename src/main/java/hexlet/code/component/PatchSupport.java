package hexlet.code.component;

import hexlet.code.exception.ValidationException;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PatchSupport {

    private final JsonMapper jsonMapper;
    private final Validator validator;

    public PatchSupport(JsonMapper jsonMapper, Validator validator) {
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    public void requireObject(JsonNode patch) {
        if (patch == null || !patch.isObject()) {
            throw new ValidationException(Map.of("body", "Patch body must be a JSON object"));
        }
    }

    public <T> T apply(T current, JsonNode patch, Class<T> type) {
        requireObject(patch);
        T result;
        try {
            JsonNode merged = jsonMapper.updateValue(jsonMapper.valueToTree(current), patch);
            result = jsonMapper.treeToValue(merged, type);
        } catch (JacksonException e) {
            throw new ValidationException(Map.of("body", "Invalid field value"));
        }
        validate(result);
        return result;
    }

    private <T> void validate(T target) {
        var violations = validator.validate(target);
        if (violations.isEmpty()) {
            return;
        }
        Map<String, String> errors = new HashMap<>();
        for (var v : violations) {
            errors.put(v.getPropertyPath().toString(), v.getMessage());
        }
        throw new ValidationException(errors);
    }
}
