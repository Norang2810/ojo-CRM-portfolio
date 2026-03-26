package org.backend.domain.member.dto.search;

public class MemberStatusLabel {
    private MemberStatusLabel() {}

    public static String labelOf(String status) {
        return switch (status) {
            case "ACTIVE" -> "활성";
            case "INACTIVE" -> "비활성";
            case "DELETED" -> "삭제";
            default -> status;
        };
    }
}
