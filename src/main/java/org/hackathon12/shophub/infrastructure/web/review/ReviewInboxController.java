package org.hackathon12.shophub.infrastructure.web.review;

import org.hackathon12.shophub.domain.review.model.ReviewInbox;
import org.hackathon12.shophub.domain.review.service.ReviewInboxService;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/v1/reviews")
public class ReviewInboxController {

    private final ReviewInboxService reviewInboxService;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public ReviewInboxController(ReviewInboxService reviewInboxService, ShopHubAuthGuard shopHubAuthGuard) {
        this.reviewInboxService = reviewInboxService;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @GetMapping("/inbox")
    public ReviewInbox getInbox(@RequestParam List<String> placeIds, HttpServletRequest request) {
        if (CollectionUtils.isEmpty(placeIds)) {
            throw new IllegalArgumentException(
                    "최소 1개 이상의 placeIds가 필요합니다. MockMap GET /api/reviews/?place_id= 와 동일한 place_id를 사용합니다."
            );
        }
        shopHubAuthGuard.requirePlaceIdsMember(request, placeIds);
        return reviewInboxService.getUnifiedInbox(placeIds);
    }
}
