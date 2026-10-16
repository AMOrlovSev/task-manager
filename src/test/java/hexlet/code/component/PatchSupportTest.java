package hexlet.code.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import hexlet.code.exception.ValidationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class PatchSupportTest {

    @Getter
    @Setter
    public static class TestDto {
        @NotBlank private String name;
        private String note;
    }

    private JsonMapper jsonMapper;
    private PatchSupport patchSupport;

    @BeforeEach
    void setUp() {
        jsonMapper =
                JsonMapper.builder()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .build();
        patchSupport =
                new PatchSupport(
                        jsonMapper, Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Test
    void missingKeyKeepsCurrentValue() {
        var current = dto("Draft", "n");
        var result = patchSupport.apply(current, jsonMapper.readTree("{}"), TestDto.class);
        assertThat(result.getName()).isEqualTo("Draft");
        assertThat(result.getNote()).isEqualTo("n");
    }

    @Test
    void presentKeyReplacesValue() {
        var current = dto("Draft", "n");
        var result =
                patchSupport.apply(
                        current,
                        jsonMapper.readTree(
                                """
                                {"name":"New"}"""),
                        TestDto.class);
        assertThat(result.getName()).isEqualTo("New");
    }

    @Test
    void nullOnNotBlankFieldFails() {
        var current = dto("Draft", "n");
        assertThatThrownBy(
                        () ->
                                patchSupport.apply(
                                        current,
                                        jsonMapper.readTree(
                                                """
                                        {"name":null}"""),
                                        TestDto.class))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("name"));
    }

    @Test
    void nullOnOptionalFieldClears() {
        var current = dto("Draft", "n");
        var result =
                patchSupport.apply(
                        current,
                        jsonMapper.readTree(
                                """
                                {"note":null}"""),
                        TestDto.class);
        assertThat(result.getNote()).isNull();
    }

    @Test
    void blankStringOnNotBlankFails() {
        var current = dto("Draft", "n");
        assertThatThrownBy(
                        () ->
                                patchSupport.apply(
                                        current,
                                        jsonMapper.readTree(
                                                """
                                        {"name":"   "}"""),
                                        TestDto.class))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void unknownFieldIgnored() {
        var current = dto("Draft", "n");
        var result =
                patchSupport.apply(
                        current,
                        jsonMapper.readTree(
                                """
                                {"unknown":1}"""),
                        TestDto.class);
        assertThat(result.getName()).isEqualTo("Draft");
    }

    @Test
    void nonObjectBodyFails() {
        var current = dto("Draft", "n");
        for (String body : new String[] {"[]", "\"x\"", "1", "null"}) {
            assertThatThrownBy(
                            () ->
                                    patchSupport.apply(
                                            current, jsonMapper.readTree(body), TestDto.class))
                    .isInstanceOf(ValidationException.class)
                    .extracting(ex -> ((ValidationException) ex).getErrors())
                    .satisfies(errors -> assertThat(errors).containsKey("body"));
        }
    }

    @Test
    void wrongTypeValueFails() {
        var current = dto("Draft", "n");
        assertThatThrownBy(
                        () ->
                                patchSupport.apply(
                                        current,
                                        jsonMapper.readTree(
                                                """
                                        {"name":{"x":1}}"""),
                                        TestDto.class))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void currentIsNotMutated() {
        var current = dto("Draft", "n");
        patchSupport.apply(
                current,
                jsonMapper.readTree(
                        """
                {"name":"New"}"""),
                TestDto.class);
        assertThat(current.getName()).isEqualTo("Draft");
    }

    private TestDto dto(String name, String note) {
        var d = new TestDto();
        d.setName(name);
        d.setNote(note);
        return d;
    }
}
