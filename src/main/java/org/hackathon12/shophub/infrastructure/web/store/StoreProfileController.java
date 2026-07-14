package org.hackathon12.shophub.infrastructure.web.store;

import org.hackathon12.shophub.domain.store.model.BusinessHour;
import org.hackathon12.shophub.domain.store.model.MenuItem;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreMembershipService;
import org.hackathon12.shophub.domain.store.service.StoreProfileService;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/v1/stores")
public class StoreProfileController {

    private static final Pattern HOURS_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2})\\s*[-~]\\s*(\\d{1,2}:\\d{2})");

    private final StoreProfileService storeProfileService;
    private final StoreMembershipService storeMembershipService;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public StoreProfileController(
            StoreProfileService storeProfileService,
            StoreMembershipService storeMembershipService,
            ShopHubAuthGuard shopHubAuthGuard
    ) {
        this.storeProfileService = storeProfileService;
        this.storeMembershipService = storeMembershipService;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @GetMapping
    @Operation(summary = "가게 목록 조회", description = "로그인한 사용자가 소속된 가게만 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public List<StoreProfile> getStores(HttpServletRequest request) {
        UUID userId = shopHubAuthGuard.requireUserId(request);
        return storeMembershipService.findStoresForUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "가게 생성",
            description = """
                    로그인한 사용자를 해당 가게 OWNER로 자동 등록합니다.

                    필수: name, category, (address 또는 neighborhood), (toneOfVoice 또는 tone)
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public StoreProfile createStore(@RequestBody CreateStoreRequest request, HttpServletRequest httpRequest) {
        UUID userId = shopHubAuthGuard.requireUserId(httpRequest);

        String introduction = firstNonBlank(request.introduction(), request.description());
        String address = firstNonBlank(request.address(), request.neighborhood());
        String toneOfVoice = firstNonBlank(request.toneOfVoice(), request.tone());

        List<BusinessHour> businessHours = request.businessHours();
        if ((businessHours == null || businessHours.isEmpty()) && request.hours() != null && !request.hours().isBlank()) {
            businessHours = parseDailyBusinessHours(request.hours());
        }

        List<MenuItem> menuItems = new ArrayList<>();
        if (request.menuItems() != null) {
            for (CreateStoreMenuItemRequest menuItem : request.menuItems()) {
                if (menuItem == null) {
                    continue;
                }
                menuItems.add(new MenuItem(
                        UUID.randomUUID(),
                        menuItem.name(),
                        menuItem.description()
                ));
            }
        }
        if (menuItems.isEmpty() && request.menu() != null) {
            for (String menuName : request.menu()) {
                if (menuName == null || menuName.isBlank()) {
                    continue;
                }
                menuItems.add(new MenuItem(UUID.randomUUID(), menuName, ""));
            }
        }

        StoreProfile created = storeProfileService.createStore(
                request.name(),
                request.phone(),
                introduction,
                address,
                request.category(),
                toneOfVoice,
                businessHours,
                menuItems,
                request.googlePlaceId(),
                request.googleReviewUrl()
        );

        storeMembershipService.ensureOwnerMembership(userId, created.id());
        return created;
    }

    @GetMapping("/{storeId}/profile")
    public StoreProfile getProfile(@PathVariable UUID storeId, HttpServletRequest request) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return storeProfileService.getStore(storeId);
    }

    @PutMapping("/{storeId}/profile/basic")
    public StoreProfile updateBasic(
            @PathVariable UUID storeId,
            @RequestBody UpdateBasicRequest requestBody,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return storeProfileService.updateBasicInfo(
                storeId,
                requestBody.name(),
                requestBody.phone(),
                requestBody.introduction(),
                requestBody.address(),
                requestBody.category(),
                requestBody.toneOfVoice()
        );
    }

    @PutMapping("/{storeId}/profile/hours")
    public StoreProfile updateHours(
            @PathVariable UUID storeId,
            @RequestBody UpdateHoursRequest requestBody,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return storeProfileService.updateBusinessHours(storeId, requestBody.businessHours());
    }

    @PostMapping("/{storeId}/profile/menus")
    public StoreProfile addMenu(
            @PathVariable UUID storeId,
            @RequestBody AddMenuRequest requestBody,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return storeProfileService.addMenu(storeId, requestBody.name(), requestBody.description());
    }

    @DeleteMapping("/{storeId}/profile/menus/{menuId}")
    public StoreProfile removeMenu(
            @PathVariable UUID storeId,
            @PathVariable UUID menuId,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return storeProfileService.removeMenu(storeId, menuId);
    }

    public record UpdateBasicRequest(
            String name,
            String phone,
            String introduction,
            String address,
            String category,
            String toneOfVoice
    ) {
    }

    public record UpdateHoursRequest(
            List<BusinessHour> businessHours
    ) {
    }

    public record AddMenuRequest(
            String name,
            String description
    ) {
    }

    @Schema(description = """
            가게 생성 요청.

            Swagger는 모든 필드를 표시하지만 실제 필수값은 name, category,
            address|neighborhood, toneOfVoice|tone 뿐입니다.
            """)
    public record CreateStoreRequest(
            @Schema(description = "가게명 (필수)", example = "모모커피 연남", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,
            @Schema(description = "전화번호", example = "02-335-0426")
            String phone,
            @Schema(description = "소개글. 없으면 description 사용")
            String introduction,
            @Schema(description = "introduction 프론트 호환 필드")
            String description,
            @Schema(description = "주소. 없으면 neighborhood 사용", example = "서울 마포구 동교로 242")
            String address,
            @Schema(description = "address 프론트 호환 필드", example = "서울 마포구 연남동")
            String neighborhood,
            @Schema(description = "업종 (필수)", example = "카페 · 디저트", requiredMode = Schema.RequiredMode.REQUIRED)
            String category,
            @Schema(description = "말투. 없으면 tone 사용", example = "따뜻하고 담백한")
            String toneOfVoice,
            @Schema(description = "toneOfVoice 프론트 호환 필드", example = "따뜻하고 담백한 동네 카페 말투")
            String tone,
            @Schema(description = "영업시간 요약 문자열", example = "매일 11:00 - 21:00")
            String hours,
            @Schema(description = "요일별 영업시간. 없으면 hours 파싱 또는 기본값")
            List<BusinessHour> businessHours,
            @Schema(description = "메뉴 이름 배열(프론트 호환)", example = "[\"모모 라떼\", \"버터 취향시에\"]")
            List<String> menu,
            @Schema(description = "메뉴 상세 배열. 없으면 menu 사용")
            List<CreateStoreMenuItemRequest> menuItems,
            @Schema(description = "MockMap/Google place_id")
            String googlePlaceId,
            @Schema(description = "리뷰 외부 URL")
            String googleReviewUrl
    ) {
    }

    public record CreateStoreMenuItemRequest(
            String name,
            String description
    ) {
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private List<BusinessHour> parseDailyBusinessHours(String hoursText) {
        Matcher matcher = HOURS_PATTERN.matcher(hoursText);
        if (!matcher.find()) {
            return null;
        }

        String openTime = matcher.group(1);
        String closeTime = matcher.group(2);
        return List.of(
                new BusinessHour("MON", openTime, closeTime, true),
                new BusinessHour("TUE", openTime, closeTime, true),
                new BusinessHour("WED", openTime, closeTime, true),
                new BusinessHour("THU", openTime, closeTime, true),
                new BusinessHour("FRI", openTime, closeTime, true),
                new BusinessHour("SAT", openTime, closeTime, true),
                new BusinessHour("SUN", openTime, closeTime, true)
        );
    }
}
