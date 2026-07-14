package org.hackathon12.shophub.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface PublicImageUrlResolver {

    String resolve(MultipartFile image, String savedFileName, int index);
}
