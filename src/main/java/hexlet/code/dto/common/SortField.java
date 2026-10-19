package hexlet.code.dto.common;

public record SortField(String property, boolean ignoreCase) {
    public static SortField of(String property) {
        return new SortField(property, false);
    }

    public static SortField text(String property) {
        return new SortField(property, true);
    }
}
