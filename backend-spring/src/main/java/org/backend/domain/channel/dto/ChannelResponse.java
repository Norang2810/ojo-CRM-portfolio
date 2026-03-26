package org.backend.domain.channel.dto;

import lombok.Getter;
import org.backend.domain.channel.entity.Channel;
import org.backend.domain.channel.entity.ChannelStatus;

import java.time.LocalDateTime;

@Getter
public class ChannelResponse {

    private final Long id;
    private final Long adminId;
    private final String name;
    private final String description;
    private final ChannelStatus status;
    private final int memberCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ChannelResponse(Long id, Long adminId, String name, String description,
                            ChannelStatus status, int memberCount,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.adminId = adminId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ChannelResponse from(Channel channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getAdminId(),
                channel.getName(),
                channel.getDescription(),
                channel.getStatus(),
                channel.getChannelMembers().size(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}