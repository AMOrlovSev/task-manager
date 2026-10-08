package hexlet.code.validation;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;
import org.openapitools.jackson.nullable.JsonNullable;

public class JsonNullableValueExtractor implements ValueExtractor<JsonNullable<@ExtractedValue ?>> {

    @Override
    public void extractValues(JsonNullable<?> value, ValueReceiver receiver) {
        if (value == null || !value.isPresent()) {
            return;
        }
        receiver.value(null, value.get());
    }
}
