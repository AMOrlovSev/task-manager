package hexlet.code.dto.common;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.BindParam;

public record ListParams(
        @Parameter(name = "_start", description = "Смещение, начиная с 0", example = "0")
                @BindParam("_start")
                Integer start,
        @Parameter(name = "_end", description = "Конец диапазона (не включая)", example = "10")
                @BindParam("_end")
                Integer end,
        @Parameter(name = "_sort", description = "Поле сортировки", example = "id")
                @BindParam("_sort")
                String sort,
        @Parameter(
                        name = "_order",
                        description = "Направление сортировки (ASC или DESC)",
                        example = "ASC")
                @BindParam("_order")
                String order) {}
