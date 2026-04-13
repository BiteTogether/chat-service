package com.bitetogether.chat_service.enums;

import com.bitetogether.chat_service.dto.location.LiveLocationSnapshot;

public record LocationUpdatedEvent(String conversationId, LiveLocationSnapshot location) {}
