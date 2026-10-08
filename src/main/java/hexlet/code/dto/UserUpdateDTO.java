package hexlet.code.dto;

import lombok.Getter;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;

@Getter
@Setter
public class UserUpdateDTO {
    private JsonNullable<String> email = JsonNullable.undefined();
    private JsonNullable<String> password = JsonNullable.undefined();
    private JsonNullable<String> firstName = JsonNullable.undefined();
    private JsonNullable<String> lastName = JsonNullable.undefined();
}