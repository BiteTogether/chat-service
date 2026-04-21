package com.bitetogether.chat_service.service.impl;

import com.bitetogether.chat_service.configuration.firebase.FirebaseProperties;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.service.FirebaseStorageService;
import com.bitetogether.common.exception.AppException;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FirebaseStorageServiceImpl implements FirebaseStorageService {

  Storage storage;
  FirebaseProperties firebaseProperties;

  // String literal constants
  private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
  private static final String IMAGE_JPEG = "image/jpeg";
  private static final String IMAGE_JPG = "image/jpg";
  private static final String IMAGE_PNG = "image/png";
  private static final String IMAGE_GIF = "image/gif";
  private static final String IMAGE_WEBP = "image/webp";
  private static final String EXT_JPG = "jpg";
  private static final String EXT_JPEG = "jpeg";
  private static final String EXT_PNG = "png";
  private static final String EXT_GIF = "gif";
  private static final String EXT_WEBP = "webp";

  // Content type constants
  private static final List<String> ALLOWED_IMAGE_TYPES =
      Arrays.asList(IMAGE_JPEG, IMAGE_PNG, IMAGE_JPG, IMAGE_GIF, IMAGE_WEBP);
  private static final List<String> ALLOWED_EXTENSIONS =
      Arrays.asList(EXT_JPG, EXT_JPEG, EXT_PNG, EXT_GIF, EXT_WEBP);

  private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
  private static final String FIREBASE_STORAGE_URL_TEMPLATE =
      "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media";
  private static final String URL_DELIMITER_O = "/o/";
  private static final String URL_DELIMITER_QUERY = "\\?";
  private static final String APPSPOT_DOMAIN = ".appspot.com";
  private static final String FIREBASESTORAGE_DOMAIN = ".firebasestorage.app";
  private static final String FOLDER_SEPARATOR = "/";
  private static final String FILE_EXTENSION_DOT = ".";
  private static final String CACHE_CONTROL = "public, max-age=86400";

  @Override
  public Mono<String> uploadFile(FilePart file, String folder) {
    return validateFile(file)
        .then(collectFileBytes(file))
        .flatMap(
            bytes -> {
              String fileName = generateFileName(file, folder);
              String bucketName = firebaseProperties.getStorageBucket();
              String contentType = determineContentType(file);

              logUploadInfo(file, bucketName, contentType);

              BlobInfo blobInfo = createBlobInfo(bucketName, fileName, contentType);

              return Mono.fromCallable(
                      () -> {
                        storage.create(blobInfo, bytes);
                        return generatePublicUrl(bucketName, fileName);
                      })
                  .doOnSuccess(url -> log.info("File uploaded successfully: {}", url))
                  .onErrorMap(
                      com.google.cloud.storage.StorageException.class,
                      e -> {
                        handleStorageException(e);
                        return new AppException(ErrorCode.FILE_UPLOAD_ERROR);
                      })
                  .onErrorMap(
                      Exception.class,
                      e -> {
                        log.error("Error uploading file to Firebase Storage", e);
                        return new AppException(ErrorCode.FILE_UPLOAD_ERROR);
                      });
            });
  }

  @Override
  public Mono<Boolean> deleteFile(String fileUrl) {
    return Mono.fromCallable(
            () -> {
              try {
                String fileName = extractFileNameFromUrl(fileUrl);
                String bucketName = firebaseProperties.getStorageBucket();

                BlobId blobId = BlobId.of(bucketName, fileName);
                boolean deleted = storage.delete(blobId);

                logDeletionResult(deleted, fileName);
                return deleted;
              } catch (Exception e) {
                log.error("Error deleting file from Firebase Storage", e);
                return false;
              }
            })
        .onErrorReturn(false);
  }

  @Override
  public Mono<String> uploadConversationAvatar(FilePart file, String conversationId) {
    return uploadFile(file, "avatars/conversation_" + conversationId);
  }

  private Mono<Void> validateFile(FilePart file) {
    log.info("Starting file validation...");

    return Mono.defer(
        () -> {
          validateFileExists(file);
          logFileDetails(file);
          validateFileType(file);
          log.info("File validation passed successfully");
          return Mono.empty();
        });
  }

  private void validateFileExists(FilePart file) {
    if (file == null || file.filename().isEmpty()) {
      log.error("File is null or empty");
      throw new AppException(ErrorCode.FILE_EMPTY);
    }
  }

  private void logFileDetails(FilePart file) {
    log.info(
        "File details - Name: {}, Content-Type: '{}'",
        file.filename(),
        file.headers().getContentType());
  }

  private void validateFileType(FilePart file) {
    var mediaType = file.headers().getContentType();
    String contentType = mediaType != null ? mediaType.toString() : "";

    if (isGenericContentType(contentType)) {
      validateByFileExtension(file.filename(), contentType);
    } else {
      validateByContentType(contentType);
    }
  }

  private boolean isGenericContentType(String contentType) {
    return contentType == null
        || contentType.isEmpty()
        || CONTENT_TYPE_OCTET_STREAM.equalsIgnoreCase(contentType);
  }

  private void validateByFileExtension(String filename, String contentType) {
    log.warn(
        "Content type is null, empty, or generic ({}). Falling back to file extension validation",
        contentType);

    String extension = extractFileExtension(filename);
    log.info("Validating by file extension: '{}'", extension);

    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      log.error(
          "Invalid file extension: '{}'. Allowed extensions: {}", extension, ALLOWED_EXTENSIONS);
      throw new AppException(ErrorCode.INVALID_FILE_TYPE);
    }

    log.info("File extension '{}' is valid", extension);
  }

  private void validateByContentType(String contentType) {
    log.info(
        "Validating content type: '{}' against allowed types: {}",
        contentType,
        ALLOWED_IMAGE_TYPES);

    String normalizedContentType = contentType.trim().toLowerCase();

    if (ALLOWED_IMAGE_TYPES.stream()
        .noneMatch(type -> type.equalsIgnoreCase(normalizedContentType))) {
      log.error("Invalid file type: '{}'. Allowed types are: {}", contentType, ALLOWED_IMAGE_TYPES);
      throw new AppException(ErrorCode.INVALID_FILE_TYPE);
    }

    log.info("Content type '{}' is valid", contentType);
  }

  private String extractFileExtension(String filename) {
    if (filename == null || !filename.contains(FILE_EXTENSION_DOT)) {
      log.error("Cannot determine file type from extension");
      throw new AppException(ErrorCode.INVALID_FILE_TYPE);
    }
    return filename.substring(filename.lastIndexOf(FILE_EXTENSION_DOT) + 1).toLowerCase();
  }

  private String generateFileName(FilePart file, String folder) {
    String originalFileName = file.filename();
    String extension = "";
    if (originalFileName.contains(FILE_EXTENSION_DOT)) {
      extension = originalFileName.substring(originalFileName.lastIndexOf(FILE_EXTENSION_DOT));
    }
    String uniqueId = UUID.randomUUID().toString();
    return folder + FOLDER_SEPARATOR + uniqueId + extension;
  }

  private String determineContentType(FilePart file) {
    var mediaType = file.headers().getContentType();
    String contentType = mediaType != null ? mediaType.toString() : "";

    if (!contentType.isEmpty() && !CONTENT_TYPE_OCTET_STREAM.equalsIgnoreCase(contentType)) {
      return contentType;
    }

    return determineContentTypeFromExtension(file.filename());
  }

  private String determineContentTypeFromExtension(String filename) {
    if (filename == null || !filename.contains(FILE_EXTENSION_DOT)) {
      return CONTENT_TYPE_OCTET_STREAM;
    }

    String extension =
        filename.substring(filename.lastIndexOf(FILE_EXTENSION_DOT) + 1).toLowerCase();

    return switch (extension) {
      case EXT_JPG, EXT_JPEG -> IMAGE_JPEG;
      case EXT_PNG -> IMAGE_PNG;
      case EXT_GIF -> IMAGE_GIF;
      case EXT_WEBP -> IMAGE_WEBP;
      default -> CONTENT_TYPE_OCTET_STREAM;
    };
  }

  private BlobInfo createBlobInfo(String bucketName, String fileName, String contentType) {
    BlobId blobId = BlobId.of(bucketName, fileName);
    return BlobInfo.newBuilder(blobId)
        .setContentType(contentType)
        .setCacheControl(CACHE_CONTROL)
        .build();
  }

  private String generatePublicUrl(String bucketName, String fileName) {
    String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    return String.format(FIREBASE_STORAGE_URL_TEMPLATE, bucketName, encodedFileName);
  }

  private void logUploadInfo(FilePart file, String bucketName, String contentType) {
    log.info("Using content type: {} for file: {}", contentType, file.filename());
    log.info("Uploading to Firebase Storage bucket: {}", bucketName);
  }

  private void handleStorageException(com.google.cloud.storage.StorageException e) {
    log.error("Firebase Storage error - Code: {}, Message: {}", e.getCode(), e.getMessage());
    if (e.getCode() == 404) {
      String projectId =
          firebaseProperties
              .getStorageBucket()
              .replace(APPSPOT_DOMAIN, "")
              .replace(FIREBASESTORAGE_DOMAIN, "");
      log.error(
          "Firebase Storage bucket '{}' does not exist. Please create it in Firebase Console: https://console.firebase.google.com/project/{}/storage",
          firebaseProperties.getStorageBucket(),
          projectId);
    }
  }

  private void logDeletionResult(boolean deleted, String fileName) {
    if (deleted) {
      log.info("File deleted successfully: {}", fileName);
    } else {
      log.warn("File not found for deletion: {}", fileName);
    }
  }

  private String extractFileNameFromUrl(String fileUrl) {
    String[] parts = fileUrl.split(URL_DELIMITER_O);
    if (parts.length > 1) {
      String encodedFileName = parts[1].split(URL_DELIMITER_QUERY)[0];
      return java.net.URLDecoder.decode(encodedFileName, StandardCharsets.UTF_8);
    }
    return "";
  }

  private Mono<byte[]> collectFileBytes(FilePart file) {
    return DataBufferUtils.join(file.content())
        .map(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              DataBufferUtils.release(dataBuffer);

              // Validate file size after collecting bytes
              if (bytes.length > MAX_FILE_SIZE) {
                log.error("File size {} exceeds maximum limit {}", bytes.length, MAX_FILE_SIZE);
                throw new AppException(ErrorCode.FILE_TOO_LARGE);
              }

              log.info("File size: {} bytes", bytes.length);
              return bytes;
            });
  }
}
