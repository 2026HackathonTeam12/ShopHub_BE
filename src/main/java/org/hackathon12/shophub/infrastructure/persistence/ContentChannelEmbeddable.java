package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.hackathon12.shophub.domain.content.model.ContentChannelPublishStatus;
import org.hackathon12.shophub.domain.content.model.ContentPlatformState;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class ContentChannelEmbeddable {

    @Column(name = "channel_name", nullable = false)
    private String channelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false)
    private ContentChannelPublishStatus publishStatus;

    protected ContentChannelEmbeddable() {
    }

    public ContentChannelEmbeddable(String channelName, ContentChannelPublishStatus publishStatus) {
        this.channelName = channelName;
        this.publishStatus = publishStatus;
    }

    public static ContentChannelEmbeddable pending(String channelName) {
        return new ContentChannelEmbeddable(channelName, ContentChannelPublishStatus.PENDING);
    }

    public ContentPlatformState toDomain() {
        return new ContentPlatformState(
                ContentChannel.fromValue(channelName),
                publishStatus
        );
    }

    public String getChannelName() {
        return channelName;
    }

    public ContentChannelPublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(ContentChannelPublishStatus publishStatus) {
        this.publishStatus = publishStatus;
    }
}
