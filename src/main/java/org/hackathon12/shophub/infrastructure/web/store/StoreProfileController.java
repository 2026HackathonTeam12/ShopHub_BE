package org.hackathon12.shophub.infrastructure.web.store;

import org.hackathon12.shophub.domain.store.model.BusinessHour;
import org.hackathon12.shophub.domain.store.model.MenuItem;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreMembershipService;
import org.hackathon12.shophub.domain.store.service.StoreProfileService;
import org.hackathon12.shophub.infrastructure.web.auth.AuthContext;
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
    private final AuthContext authContext;

    public StoreProfileController(
            StoreProfileService storeProfileService,
            StoreMembershipService storeMembershipService,
            AuthContext authContext
    ) {
        this.storeProfileService = storeProfileService;
        this.storeMembershipService = storeMembershipService;
        this.authContext = authContext;
    }

    @GetMapping
    public List<StoreProfile> getStores(HttpServletRequest request) {
        return authContext.resolveUserId(request)
                .map(storeMembershipService::findStoresForUser)
                .orElseGet(storeProfileService::getStores);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreProfile createStore(@RequestBody CreateStoreRequest request, HttpServletRequest httpRequest) {
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

        authContext.resolveUserId(httpRequest)
                .ifPresent(userId -> storeMembershipService.ensureOwnerMembership(userId, created.id()));

        return created;
    }

    @GetMapping("/{storeId}/profile")
    public StoreProfile getProfile(@PathVariable UUID storeId) {
        return storeProfileService.getStore(storeId);
    }

    @PutMapping("/{storeId}/profile/basic")
    public StoreProfile updateBasic(@PathVariable UUID storeId, @RequestBody UpdateBasicRequest request) {
        return storeProfileService.updateBasicInfo(
                storeId,
                request.name(),
                request.phone(),
                request.introduction(),
                request.address(),
                request.category(),
                request.toneOfVoice()
        );
    }

    @PutMapping("/{storeId}/profile/hours")
    public StoreProfile updateHours(@PathVariable UUID storeId, @RequestBody UpdateHoursRequest request) {
        return storeProfileService.updateBusinessHours(storeId, request.businessHours());
    }

    @PostMapping("/{storeId}/profile/menus")
    public StoreProfile addMenu(@PathVariable UUID storeId, @RequestBody AddMenuRequest request) {
        return storeProfileService.addMenu(storeId, request.name(), request.description());
    }

    @DeleteMapping("/{storeId}/profile/menus/{menuId}")
    public StoreProfile removeMenu(@PathVariable UUID storeId, @PathVariable UUID menuId) {
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

    public record CreateStoreRequest(
            String name,
            String phone,
            String introduction,
            String description,
            String address,
            String neighborhood,
            String category,
            String toneOfVoice,
            String tone,
            String hours,
            List<BusinessHour> businessHours,
            List<String> menu,
            List<CreateStoreMenuItemRequest> menuItems,
            String googlePlaceId,
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
