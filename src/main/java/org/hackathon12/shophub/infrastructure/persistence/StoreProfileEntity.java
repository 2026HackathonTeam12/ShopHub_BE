package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.store.model.BusinessHour;
import org.hackathon12.shophub.domain.store.model.MenuItem;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "store_profiles")
public class StoreProfileEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Lob
    @Column(nullable = false)
    private String introduction;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String toneOfVoice;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "store_business_hours",
            joinColumns = @JoinColumn(
                    name = "store_id",
                    foreignKey = @ForeignKey(name = "fk_store_business_hours_store")
            )
    )
    @OrderColumn(name = "display_order")
    private List<BusinessHourValue> businessHours = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "store_menu_items",
            joinColumns = @JoinColumn(
                    name = "store_id",
                    foreignKey = @ForeignKey(name = "fk_store_menu_items_store")
            )
    )
    @OrderColumn(name = "display_order")
    private List<MenuItemValue> menuItems = new ArrayList<>();

    @Column(nullable = false)
    private String googlePlaceId;

    @Column(nullable = false)
    private String googleReviewUrl;

    @Column(nullable = false)
    private int googleTotalReviews;

    @Column(nullable = false)
    private Instant updatedAt;

    protected StoreProfileEntity() {
    }

    public static StoreProfileEntity fromDomain(StoreProfile storeProfile) {
        StoreProfileEntity entity = new StoreProfileEntity();
        entity.applyDomain(storeProfile);
        return entity;
    }

    public static StoreProfileEntity reference(UUID id) {
        StoreProfileEntity entity = new StoreProfileEntity();
        entity.id = id;
        return entity;
    }

    public void applyDomain(StoreProfile storeProfile) {
        this.id = storeProfile.id();
        this.name = storeProfile.name();
        this.phone = storeProfile.phone();
        this.introduction = storeProfile.introduction();
        this.address = storeProfile.address();
        this.category = storeProfile.category();
        this.toneOfVoice = storeProfile.toneOfVoice();
        this.googlePlaceId = storeProfile.googlePlaceId();
        this.googleReviewUrl = storeProfile.googleReviewUrl();
        this.googleTotalReviews = storeProfile.googleTotalReviews();
        this.updatedAt = storeProfile.updatedAt();

        this.businessHours = new ArrayList<>();
        if (storeProfile.businessHours() != null) {
            for (BusinessHour businessHour : storeProfile.businessHours()) {
                this.businessHours.add(new BusinessHourValue(
                        businessHour.dayOfWeek(),
                        businessHour.openTime(),
                        businessHour.closeTime(),
                        businessHour.open()
                ));
            }
        }

        this.menuItems = new ArrayList<>();
        if (storeProfile.menuItems() != null) {
            for (MenuItem menuItem : storeProfile.menuItems()) {
                this.menuItems.add(new MenuItemValue(
                        menuItem.id().toString(),
                        menuItem.name(),
                        menuItem.description()
                ));
            }
        }
    }

    public StoreProfile toDomain() {
        List<BusinessHour> mappedBusinessHours = businessHours.stream()
                .map(value -> new BusinessHour(
                        value.getDayOfWeek(),
                        value.getOpenTime(),
                        value.getCloseTime(),
                        value.isOpen()
                ))
                .toList();

        List<MenuItem> mappedMenuItems = menuItems.stream()
                .map(value -> new MenuItem(
                        UUID.fromString(value.getMenuId()),
                        value.getName(),
                        value.getDescription()
                ))
                .toList();

        return new StoreProfile(
                id,
                name,
                phone,
                introduction,
                address,
                category,
                toneOfVoice,
                mappedBusinessHours,
                mappedMenuItems,
                googlePlaceId,
                googleReviewUrl,
                googleTotalReviews,
                updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    @Embeddable
    public static class BusinessHourValue {

        @Column(nullable = false)
        private String dayOfWeek;

        @Column(nullable = false)
        private String openTime;

        @Column(nullable = false)
        private String closeTime;

        @Column(nullable = false)
        private boolean open;

        protected BusinessHourValue() {
        }

        public BusinessHourValue(String dayOfWeek, String openTime, String closeTime, boolean open) {
            this.dayOfWeek = dayOfWeek;
            this.openTime = openTime;
            this.closeTime = closeTime;
            this.open = open;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        public String getOpenTime() {
            return openTime;
        }

        public String getCloseTime() {
            return closeTime;
        }

        public boolean isOpen() {
            return open;
        }
    }

    @Embeddable
    public static class MenuItemValue {

        @Column(nullable = false)
        private String menuId;

        @Column(nullable = false)
        private String name;

        @Lob
        @Column(nullable = false)
        private String description;

        protected MenuItemValue() {
        }

        public MenuItemValue(String menuId, String name, String description) {
            this.menuId = menuId;
            this.name = name;
            this.description = description;
        }

        public String getMenuId() {
            return menuId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}
