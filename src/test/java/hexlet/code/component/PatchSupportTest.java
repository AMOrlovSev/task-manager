package hexlet.code.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import hexlet.code.exception.ValidationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
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

    @Getter
    @Setter
    public static class ListDto {
        private List<Long> ids = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class NestedDto {
        private Nested nested;
    }

    @Getter
    @Setter
    public static class Nested {
        private String a;
        private String b;
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

    @Test
    void arrayIsReplacedNotMerged() {
        var current = new ListDto();
        current.setIds(new ArrayList<>(List.of(1L, 2L)));

        var result =
                patchSupport.apply(
                        current,
                        jsonMapper.readTree(
                                """
                                {"ids":[3]}"""),
                        ListDto.class);

        assertThat(result.getIds()).containsExactly(3L);
    }

    @Test
    void emptyArrayClearsList() {
        var current = new ListDto();
        current.setIds(new ArrayList<>(List.of(1L, 2L)));

        var result =
                patchSupport.apply(
                        current,
                        jsonMapper.readTree(
                                """
                                {"ids":[]}"""),
                        ListDto.class);

        assertThat(result.getIds()).isNotNull().isEmpty();
    }

    @Test
    void nullOnListFieldClears() {
        var current = new ListDto();
        current.setIds(new ArrayList<>(List.of(1L, 2L)));

        var result =
                patchSupport.apply(
                        current,
                        jsonMapper.readTree(
                                """
                                {"ids":null}"""),
                        ListDto.class);

        assertThat(result.getIds()).isNull();
    }

    @Test
    void currentListIsNotMutatedByPatch() {
        var current = new ListDto();
        current.setIds(new ArrayList<>(List.of(1L, 2L)));

        patchSupport.apply(
                current,
                jsonMapper.readTree(
                        """
                        {"ids":[3]}"""),
                ListDto.class);

        assertThat(current.getIds()).containsExactly(1L, 2L);
    }

    @Test
    void nestedObjectIsReplacedNotMerged() {
        var nested = new Nested();
        nested.setA("old-a");
        nested.setB("old-b");
        var current = new NestedDto();
        current.setNested(nested);

        var result =
                patchSupport.apply(
                        current,
                        jsonMapper.readTree(
                                """
                                {"nested":{"b":"new-b"}}"""),
                        NestedDto.class);

        assertThat(result.getNested()).isNotNull();
        assertThat(result.getNested().getA()).isNull();
        assertThat(result.getNested().getB()).isEqualTo("new-b");
    }

    @Test
    void nestedObjectMissingKeyKeepsWholeObject() {
        var nested = new Nested();
        nested.setA("old-a");
        nested.setB("old-b");
        var current = new NestedDto();
        current.setNested(nested);

        var result = patchSupport.apply(current, jsonMapper.readTree("{}"), NestedDto.class);

        assertThat(result.getNested()).isNotNull();
        assertThat(result.getNested().getA()).isEqualTo("old-a");
        assertThat(result.getNested().getB()).isEqualTo("old-b");
    }

    private TestDto dto(String name, String note) {
        var d = new TestDto();
        d.setName(name);
        d.setNote(note);
        return d;
    }
}
