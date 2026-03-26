package org.backend.domain.channel.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChannelMemberRequest {
    private List<Long> memberIds;   // 한 번에 여러 고객 추가 가능
}