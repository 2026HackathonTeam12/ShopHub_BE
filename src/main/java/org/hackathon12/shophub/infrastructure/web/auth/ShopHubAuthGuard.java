package org.hackathon12.shophub.infrastructure.web.auth;

import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.port.StoreReviewPort;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreMembershipService;
import org.hackathon12.shophub.global.error.ForbiddenException;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.hackathon12.shophub.global.error.UnauthorizedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@Component
public class ShopHubAuthGuard {

    private final AuthContext authContext;
    private final StoreMembershipService storeMembershipService;
    private final StoreReviewPort storeReviewPort;

    public ShopHubAuthGuard(
            AuthContext authContext,
            StoreMembershipService storeMembershipService,
            StoreReviewPort storeReviewPort
    ) {
        this.authContext = authContext;
        this.storeMembershipService = storeMembershipService;
        this.storeReviewPort = storeReviewPort;
    }

    public UUID requireUserId(HttpServletRequest request) {
        return authContext.resolveUserId(request)
                .orElseThrow(() -> new UnauthorizedException("ShopHub 로그인이 필요합니다."));
    }

    public UUID requireStoreMember(HttpServletRequest request, UUID storeId) {
        UUID userId = requireUserId(request);
        storeMembershipService.requireMembership(userId, storeId);
        return userId;
    }

    public UUID requireReviewMember(HttpServletRequest request, UUID reviewId) {
        UUID userId = requireUserId(request);
        StoreReview review = storeReviewPort.findById(reviewId);
        if (review == null) {
            throw new NotFoundException("리뷰를 찾을 수 없습니다. reviewId=" + reviewId);
        }
        storeMembershipService.requireMembership(userId, review.storeId());
        return userId;
    }

    public UUID requirePlaceIdsMember(HttpServletRequest request, List<String> placeIds) {
        UUID userId = requireUserId(request);
        List<StoreProfile> stores = storeMembershipService.findStoresForUser(userId);
        for (String placeId : placeIds) {
            if (!StringUtils.hasText(placeId)) {
                throw new IllegalArgumentException("placeIds에는 유효한 place_id가 필요합니다.");
            }
            boolean accessible = stores.stream()
                    .anyMatch(store -> placeId.equals(store.googlePlaceId()));
            if (!accessible) {
                throw new ForbiddenException("place_id에 대한 접근 권한이 없습니다. placeId=" + placeId);
            }
        }
        return userId;
    }
}
