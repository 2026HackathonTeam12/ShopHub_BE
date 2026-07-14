package org.hackathon12.shophub.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface PublicImageUploadPort {

    String upload(MultipartFile image);
}
