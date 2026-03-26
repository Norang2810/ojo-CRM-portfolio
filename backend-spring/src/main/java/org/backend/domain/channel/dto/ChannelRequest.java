package org.backend.domain.channel.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChannelRequest {
    private String name;
    private String description;
}