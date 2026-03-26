package org.backend.common.page;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private PageInfo page;

    public static <T> PageResponse<T> from(Page<T> pageData) {
        return new PageResponse<>(
                pageData.getContent(),
                new PageInfo(
                        pageData.getNumber(),
                        pageData.getSize(),
                        pageData.getTotalElements(),
                        pageData.getTotalPages(),
                        pageData.hasNext(),
                        pageData.hasPrevious()
                )
        );
    }
}
