package org.backend.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SegmentDataDto {
    private String segment;
    private Integer count;
    private List<SegmentMemberDto> members;
}
