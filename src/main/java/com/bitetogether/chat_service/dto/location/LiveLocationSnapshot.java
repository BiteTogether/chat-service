package com.bitetogether.chat_service.dto.location;

import java.time.Instant;

public record LiveLocationSnapshot(
    String conversationId,
    Long userId,
    String username,
    String avatar,
    Double lat,
    Double lng,
    Double accuracy,
    Double heading,
    Double speed,
    Instant timestamp,
    boolean sharing) {}
