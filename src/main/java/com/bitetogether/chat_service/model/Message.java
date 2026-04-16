package com.bitetogether.chat_service.model;

import com.bitetogether.chat_service.enums.message.MessageType;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "messages")
@Data
@Builder
@CompoundIndex(
    name = "conversation_sequence_idx",
    def = "{'conversationId': 1, 'sequence': 1}",
    unique = true)
public class Message extends BaseModel {
  @Id private String id;

  @Field("conversation_id")
  private String conversationId;

  @Field("sender_id")
  private Long senderId;

  @Field("sequence")
  private Long sequence;

  @Field("type")
  private MessageType type;

  // AES-GCM encrypted content
  @Field("cipher_text")
  private byte[] ciphertext;

  @Field("iv")
  private byte[] iv;

  @Field("auth_tag")
  private byte[] authTag;
}
