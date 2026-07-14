package org.hackathon12.shophub.infrastructure.web.content;

import org.hackathon12.shophub.infrastructure.storage.LocalImageStorageService;
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

    private final LocalImageStorageService localImageStorageService;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public ImageUploadController(
            LocalImageStorageService localImageStorageService,
            ShopHubAuthGuard shopHubAuthGuard
    ) {
        this.localImageStorageService = localImageStorageService;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse uploadImages(
            @PathVariable UUID storeId,
            @RequestParam("images") List<MultipartFile> images,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        List<String> imageUrls = localImageStorageService.saveInstagramImages(images);
        return new ImageUploadResponse(imageUrls);
    }

    public record ImageUploadResponse(List<String> imageUrls) {
    }
}
