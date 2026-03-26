package org.backend.domain.member.dto;

import lombok.Getter;

@Getter
public class MemberSortRequest {
    private String field;
    private String order;
}
