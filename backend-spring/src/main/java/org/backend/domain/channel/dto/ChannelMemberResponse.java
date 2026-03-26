package org.backend.domain.channel.dto;

import lombok.Getter;
import org.backend.domain.channel.entity.ChannelMember;

import java.time.LocalDateTime;

@Getter
public class ChannelMemberResponse {

    private final Long id;
    private final Long channelId;
    private final Long memberId;
    private final LocalDateTime createdAt;

    private ChannelMemberResponse(Long id, Long channelId, Long memberId, LocalDateTime createdAt) {
        this.id = id;
        this.channelId = channelId;
        this.memberId = memberId;
        this.createdAt = createdAt;
    }

    public static ChannelMemberResponse from(ChannelMember channelMember) {
        return new ChannelMemberResponse(
                channelMember.getId(),
                channelMember.getChannel().getId(),
                channelMember.getMemberId(),
                channelMember.getCreatedAt()
        );
    }
}