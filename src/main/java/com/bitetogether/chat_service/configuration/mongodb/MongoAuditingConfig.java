package com.bitetogether.chat_service.configuration.mongodb;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing(auditorAwareRef = "mongoAuditorProvider")
public class MongoAuditingConfig {}
