package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.content.model.ContentChannelPublishStatus;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentPlatformState;
import org.hackathon12.shophub.domain.content.model.ContentPlatformStatusItem;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "content_items")
public class ContentItemEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_items_store")
    )
    private StoreProfileEntity store;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "content_channels",
            joinColumns = @JoinColumn(
                    name = "content_id",
                    foreignKey = @ForeignKey(name = "fk_content_channels_content")
            )
    )
    @OrderColumn(name = "display_order")
    private List<ContentChannelEmbeddable> channels = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ContentItemEntity() {
    }

    public static ContentItemEntity fromDomain(ContentItem contentItem) {
        return fromDomain(contentItem, null);
    }

    public static ContentItemEntity fromDomain(ContentItem contentItem, List<ContentPlatformState> platformStates) {
        ContentItemEntity entity = new ContentItemEntity();
        entity.id = contentItem.id();
        entity.store = StoreProfileEntity.reference(contentItem.storeId());
        entity.title = contentItem.title();
        entity.body = contentItem.body();
        entity.channels = toEmbeddables(
                contentItem.channels(),
                platformStates != null ? platformStates : contentItem.platforms()
        );
        entity.status = contentItem.status();
        entity.updatedAt = contentItem.updatedAt();
        return entity;
    }

    public ContentItem toDomain() {
        return new ContentItem(
                id,
                store.getId(),
                title,
                body,
                channels.stream()
                        .map(ContentChannelEmbeddable::getChannelName)
                        .toList(),
                status,
                updatedAt,
                channels.stream()
                        .map(ContentChannelEmbeddable::toDomain)
                        .toList()
        );
    }

    public ContentPlatformStatusItem toPlatformStatusDomain() {
        return new ContentPlatformStatusItem(
                id,
                store.getId(),
                title,
                body,
                channels.stream()
                        .map(ContentChannelEmbeddable::toDomain)
                        .toList(),
                updatedAt
        );
    }

    public void mergePlatformStatuses(ContentItemEntity existing) {
        Map<String, ContentChannelPublishStatus> existingStatuses = existing.channels.stream()
                .collect(Collectors.toMap(
                        ContentChannelEmbeddable::getChannelName,
                        ContentChannelEmbeddable::getPublishStatus,
                        (left, right) -> left
                ));

        for (ContentChannelEmbeddable channel : channels) {
            ContentChannelPublishStatus preservedStatus = existingStatuses.get(channel.getChannelName());
            if (preservedStatus != null) {
                channel.setPublishStatus(preservedStatus);
            }
        }
    }

    public void resetAllPlatformStatusesToPending() {
        for (ContentChannelEmbeddable channel : channels) {
            channel.setPublishStatus(ContentChannelPublishStatus.PENDING);
        }
    }

    public void updateChannelPublishStatus(String channelName, ContentChannelPublishStatus publishStatus) {
        for (ContentChannelEmbeddable channel : channels) {
            if (channel.getChannelName().equals(channelName)) {
                channel.setPublishStatus(publishStatus);
                return;
            }
        }
        throw new IllegalArgumentException("콘텐츠 채널을 찾을 수 없습니다: " + channelName);
    }

    public void setStatus(ContentStatus status) {
        this.status = status;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    private static List<ContentChannelEmbeddable> toEmbeddables(
            List<String> channels,
            List<ContentPlatformState> platformStates
    ) {
        Map<String, ContentChannelPublishStatus> statusByChannel = platformStates == null
                ? Map.of()
                : platformStates.stream()
                        .collect(Collectors.toMap(
                                state -> state.platform().name(),
                                ContentPlatformState::status,
                                (left, right) -> left
                        ));

        List<String> normalizedChannels = channels == null ? List.of() : channels;
        return normalizedChannels.stream()
                .map(channelName -> new ContentChannelEmbeddable(
                        channelName,
                        statusByChannel.getOrDefault(channelName, ContentChannelPublishStatus.PENDING)
                ))
                .toList();
    }
}
