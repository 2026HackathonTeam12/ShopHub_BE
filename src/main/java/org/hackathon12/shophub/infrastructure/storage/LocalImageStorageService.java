package org.hackathon12.shophub.infrastructure.storage;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LocalImageStorageService {

    private final StorageProperties storageProperties;
    private final PublicImageUploadPort publicImageUploadPort;

    public LocalImageStorageService(
            StorageProperties storageProperties,
            PublicImageUploadPort publicImageUploadPort
    ) {
        this.storageProperties = storageProperties;
        this.publicImageUploadPort = publicImageUploadPort;
    }

    public List<String> saveInstagramImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("Instagram 게시에는 최소 1개 이상의 이미지가 필요합니다.");
        }
        if (images.size() > 10) {
            throw new IllegalArgumentException("Instagram 게시 이미지는 최대 10장까지 지원합니다.");
        }

        Path instagramDir = Path.of(storageProperties.uploadDir(), "instagram");
        try {
            Files.createDirectories(instagramDir);
        } catch (IOException exception) {
            throw new IllegalArgumentException("업로드 디렉토리를 생성할 수 없습니다.");
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile image : images) {
            validateImage(image);
            saveLocalCopy(image, instagramDir);
            imageUrls.add(publicImageUploadPort.upload(image));
        }

        return imageUrls;
    }

    private void saveLocalCopy(MultipartFile image, Path instagramDir) {
        String extension = extractExtension(image.getOriginalFilename());
        String fileName = UUID.randomUUID() + extension;
        Path destination = instagramDir.resolve(fileName);
        try {
            Files.copy(image.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalArgumentException("이미지 저장에 실패했습니다: " + image.getOriginalFilename());
        }
    }

    public String fileStorageLocation() {
        return Path.of(storageProperties.uploadDir()).toAbsolutePath().toUri().toString();
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("업로드 이미지가 비어 있습니다.");
        }
        String contentType = image.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return ".jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
