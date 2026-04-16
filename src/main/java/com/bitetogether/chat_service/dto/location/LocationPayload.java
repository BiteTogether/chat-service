package com.bitetogether.chat_service.dto.location;

import java.time.Instant;

public record LocationPayload(
    Double lat, Double lng, Double accuracy, Double heading, Double speed, Instant timestamp) {}
