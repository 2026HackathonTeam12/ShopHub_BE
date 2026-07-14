package org.hackathon12.shophub.domain.content.port;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ContentImageStoragePort {

    List<String> uploadImages(List<MultipartFile> images);
}
