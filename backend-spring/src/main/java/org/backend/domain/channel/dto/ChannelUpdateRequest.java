package org.backend.domain.channel.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.backend.domain.channel.entity.ChannelStatus;

@Getter
@NoArgsConstructor
public class ChannelUpdateRequest {
    private String name;
    private String description;
    private ChannelStatus status;
}