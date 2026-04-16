package com.bitetogether.chat_service.enums.vote;

import com.bitetogether.chat_service.dto.vote.VoteSessionDTO;

public record VoteSessionRealtimeEvent(
    String conversationId, String eventType, VoteSessionDTO voteSession) {}
