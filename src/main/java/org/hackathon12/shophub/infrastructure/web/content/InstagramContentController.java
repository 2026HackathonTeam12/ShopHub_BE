package org.hackathon12.shophub.infrastructure.web.content;

import org.hackathon12.shophub.domain.instagram.model.InstagramPublishResult;
import org.hackathon12.shophub.domain.instagram.service.InstagramPublishService;
import org.hackathon12.shophub.infrastructure.storage.LocalImageStorageService;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stores/{storeId}/contents/instagram")
public class InstagramContentController {

    private final LocalImageStorageService localImageStorageService;
    private final InstagramPublishService instagramPublishService;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public InstagramContentController(
            LocalImageStorageService localImageStorageService,
            InstagramPublishService instagramPublishService,
            ShopHubAuthGuard shopHubAuthGuard
    ) {
        this.localImageStorageService = localImageStorageService;
        this.instagramPublishService = instagramPublishService;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @PostMapping(value = "/publish-carousel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InstagramPublishResult publishCarousel(
            @PathVariable UUID storeId,
            @RequestPart("images") List<MultipartFile> images,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        instagramPublishService.ensurePublishReady();
        List<String> imageUrls = localImageStorageService.saveInstagramImages(images);
        return instagramPublishService.generateAndPublish(storeId, imageUrls);
    }
}
