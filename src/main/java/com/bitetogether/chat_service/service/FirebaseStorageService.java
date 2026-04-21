package com.bitetogether.chat_service.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface FirebaseStorageService {
  Mono<String> uploadFile(FilePart file, String folder);

  Mono<Boolean> deleteFile(String fileUrl);

  Mono<String> uploadConversationAvatar(FilePart file, String conversationId);
}
