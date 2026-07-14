package org.hackathon12.shophub.infrastructure.x;

import org.hackathon12.shophub.infrastructure.x.oauth.XOAuthConnectionStatus;
import org.hackathon12.shophub.infrastructure.x.oauth.XOwnerOAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XPublishAdapterTest {

    private static final UUID STORE_ID = UUID.fromString("0f7ed494-8e0e-4c5e-b6f4-5294ee3989d1");

    @Mock
    private XOwnerOAuthService xOwnerOAuthService;

    @Mock
    private XApiClient xApiClient;

    @InjectMocks
    private XPublishAdapter adapter;

    @Test
    void publishPost_fallsBackToTextOnly_whenMediaUploadFails() {
        when(xOwnerOAuthService.getConnectedAccount(STORE_ID)).thenReturn(connectedStatus());
        when(xOwnerOAuthService.getAccessToken(STORE_ID)).thenReturn("access-token");
        when(xApiClient.downloadImage("https://cdn.example.com/a.jpg")).thenReturn(new byte[]{1, 2, 3});
        when(xApiClient.uploadImage(eq("access-token"), any(), any()))
                .thenThrow(new XApiException("X 미디어 업로드 실패: HTTP 403"));
        when(xApiClient.publishTweet("access-token", "hello x", List.of())).thenReturn("tweet-1");

        String tweetId = adapter.publishPost(STORE_ID, "hello x", List.of("https://cdn.example.com/a.jpg"));

        assertThat(tweetId).isEqualTo("tweet-1");
        verify(xApiClient).publishTweet("access-token", "hello x", List.of());
    }

    @Test
    void publishPost_refreshesToken_whenUnauthorizedThenRetries() {
        when(xOwnerOAuthService.getConnectedAccount(STORE_ID)).thenReturn(connectedStatus());
        when(xOwnerOAuthService.getAccessToken(STORE_ID))
                .thenReturn("stale-token")
                .thenReturn("fresh-token");
        when(xApiClient.publishTweet(eq("stale-token"), eq("hello x"), anyList()))
                .thenThrow(new XApiException("X API 호출 실패: HTTP 401 body=Invalid or expired token"));
        when(xApiClient.publishTweet("fresh-token", "hello x", List.of())).thenReturn("tweet-2");

        String tweetId = adapter.publishPost(STORE_ID, "hello x", List.of());

        assertThat(tweetId).isEqualTo("tweet-2");
        verify(xOwnerOAuthService).invalidateAccessToken(STORE_ID);
        verify(xApiClient).publishTweet("fresh-token", "hello x", List.of());
        verify(xApiClient, never()).uploadImage(any(), any(), any());
    }

    private XOAuthConnectionStatus connectedStatus() {
        return new XOAuthConnectionStatus(
                true,
                true,
                "abcd...efgh",
                "123",
                "shop",
                "2026-07-15T00:00:00Z"
        );
    }
}
