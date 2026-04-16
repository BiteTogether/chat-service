package com.bitetogether.chat_service.enums.location;

import com.bitetogether.chat_service.dto.location.LiveLocationSnapshot;

public record LocationUpdatedEvent(String conversationId, LiveLocationSnapshot location) {}
