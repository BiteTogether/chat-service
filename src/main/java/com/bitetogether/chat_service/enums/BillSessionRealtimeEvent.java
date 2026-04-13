package com.bitetogether.chat_service.enums;

import com.bitetogether.chat_service.dto.bill.BillSessionDTO;

public record BillSessionRealtimeEvent(
    String conversationId, String eventType, BillSessionDTO billSession) {}
