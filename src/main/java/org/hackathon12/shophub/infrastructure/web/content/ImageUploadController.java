package org.hackathon12.shophub.infrastructure.web.content;

import org.hackathon12.shophub.domain.content.port.ContentImageStoragePort;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stores/{storeId}/contents")
public class ImageUploadController {

    private final ContentImageStoragePort contentImageStoragePort;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public ImageUploadController(
            ContentImageStoragePort contentImageStoragePort,
            ShopHubAuthGuard shopHubAuthGuard
    ) {
        this.contentImageStoragePort = contentImageStoragePort;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse uploadImages(
            @PathVariable UUID storeId,
            @RequestParam("images") List<MultipartFile> images,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        List<String> imgUrls = contentImageStoragePort.uploadImages(images);
        return new ImageUploadResponse(imgUrls);
    }

    public record ImageUploadResponse(List<String> img_urls) {
    }
}
