package org.hackathon12.shophub.domain.store.service;

import org.hackathon12.shophub.domain.store.model.BusinessHour;
import org.hackathon12.shophub.domain.store.model.MenuItem;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.port.StoreProfilePort;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StoreProfileService {

    private final StoreProfilePort storeProfilePort;

    public StoreProfileService(StoreProfilePort storeProfilePort) {
        this.storeProfilePort = storeProfilePort;
    }

    public List<StoreProfile> getStores() {
        return storeProfilePort.findAll();
    }

    public StoreProfile createStore(
            String name,
            String phone,
            String introduction,
            String address,
            String category,
            String toneOfVoice,
            List<BusinessHour> businessHours,
            List<MenuItem> menuItems,
            String googlePlaceId,
            String googleReviewUrl
    ) {
        String normalizedName = requiredText(name, "name");
        String normalizedAddress = requiredText(address, "address");
        String normalizedCategory = requiredText(category, "category");
        String normalizedToneOfVoice = requiredText(toneOfVoice, "toneOfVoice");

        StoreProfile created = new StoreProfile(
                UUID.randomUUID(),
                normalizedName,
                optionalText(phone),
                optionalText(introduction),
                normalizedAddress,
                normalizedCategory,
                normalizedToneOfVoice,
                normalizeBusinessHours(businessHours),
                normalizeMenuItems(menuItems),
                optionalText(googlePlaceId),
                blankToDefault(googleReviewUrl, "https://maps.google.com"),
                0,
                Instant.now()
        );
        return storeProfilePort.save(created);
    }

    public StoreProfile getStore(UUID storeId) {
        StoreProfile storeProfile = storeProfilePort.findById(storeId);
        if (storeProfile == null) {
            throw new NotFoundException("가게를 찾을 수 없습니다. storeId=" + storeId);
        }
        return storeProfile;
    }

    public StoreProfile updateBasicInfo(
            UUID storeId,
            String name,
            String phone,
            String introduction,
            String address,
            String category,
            String toneOfVoice
    ) {
        StoreProfile current = getStore(storeId);

        StoreProfile updated = new StoreProfile(
                current.id(),
                name,
                phone,
                introduction,
                address,
                category,
                toneOfVoice,
                current.businessHours(),
                current.menuItems(),
                current.googlePlaceId(),
                current.googleReviewUrl(),
                current.googleTotalReviews(),
                Instant.now()
        );
        return storeProfilePort.save(updated);
    }

    public StoreProfile updateBusinessHours(UUID storeId, List<BusinessHour> businessHours) {
        StoreProfile current = getStore(storeId);
        StoreProfile updated = new StoreProfile(
                current.id(),
                current.name(),
                current.phone(),
                current.introduction(),
                current.address(),
                current.category(),
                current.toneOfVoice(),
                businessHours,
                current.menuItems(),
                current.googlePlaceId(),
                current.googleReviewUrl(),
                current.googleTotalReviews(),
                Instant.now()
        );
        return storeProfilePort.save(updated);
    }

    public StoreProfile addMenu(UUID storeId, String name, String description) {
        StoreProfile current = getStore(storeId);
        List<MenuItem> nextMenus = new ArrayList<>(current.menuItems());
        nextMenus.add(new MenuItem(UUID.randomUUID(), name, description));

        StoreProfile updated = new StoreProfile(
                current.id(),
                current.name(),
                current.phone(),
                current.introduction(),
                current.address(),
                current.category(),
                current.toneOfVoice(),
                current.businessHours(),
                nextMenus,
                current.googlePlaceId(),
                current.googleReviewUrl(),
                current.googleTotalReviews(),
                Instant.now()
        );
        return storeProfilePort.save(updated);
    }

    public StoreProfile removeMenu(UUID storeId, UUID menuId) {
        StoreProfile current = getStore(storeId);
        List<MenuItem> nextMenus = current.menuItems().stream()
                .filter(menu -> !menu.id().equals(menuId))
                .toList();

        StoreProfile updated = new StoreProfile(
                current.id(),
                current.name(),
                current.phone(),
                current.introduction(),
                current.address(),
                current.category(),
                current.toneOfVoice(),
                current.businessHours(),
                nextMenus,
                current.googlePlaceId(),
                current.googleReviewUrl(),
                current.googleTotalReviews(),
                Instant.now()
        );
        return storeProfilePort.save(updated);
    }

    public StoreProfile updateGoogleReviewMeta(UUID storeId, int totalReviews) {
        StoreProfile current = getStore(storeId);
        StoreProfile updated = new StoreProfile(
                current.id(),
                current.name(),
                current.phone(),
                current.introduction(),
                current.address(),
                current.category(),
                current.toneOfVoice(),
                current.businessHours(),
                current.menuItems(),
                current.googlePlaceId(),
                current.googleReviewUrl(),
                totalReviews,
                Instant.now()
        );
        return storeProfilePort.save(updated);
    }

    private List<BusinessHour> normalizeBusinessHours(List<BusinessHour> businessHours) {
        if (businessHours == null || businessHours.isEmpty()) {
            return List.of(
                    new BusinessHour("MON", "10:00", "20:00", true),
                    new BusinessHour("TUE", "10:00", "20:00", true),
                    new BusinessHour("WED", "10:00", "20:00", true),
                    new BusinessHour("THU", "10:00", "20:00", true),
                    new BusinessHour("FRI", "10:00", "20:00", true),
                    new BusinessHour("SAT", "10:00", "20:00", true),
                    new BusinessHour("SUN", "10:00", "20:00", true)
            );
        }
        return businessHours;
    }

    private List<MenuItem> normalizeMenuItems(List<MenuItem> menuItems) {
        if (menuItems == null || menuItems.isEmpty()) {
            return List.of();
        }

        List<MenuItem> normalized = new ArrayList<>();
        for (MenuItem menuItem : menuItems) {
            if (menuItem == null || menuItem.name() == null || menuItem.name().isBlank()) {
                continue;
            }
            normalized.add(new MenuItem(
                    menuItem.id() == null ? UUID.randomUUID() : menuItem.id(),
                    menuItem.name().trim(),
                    optionalText(menuItem.description())
            ));
        }
        return normalized;
    }

    private String requiredText(String value, String fieldName) {
        String normalized = optionalText(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 값은 필수입니다.");
        }
        return normalized;
    }

    private String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        String normalized = optionalText(value);
        if (normalized.isBlank()) {
            return defaultValue;
        }
        return normalized;
    }
}
