package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.auth.model.UserAccount;
import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.hackathon12.shophub.domain.content.model.ContentChannelPublishStatus;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentPlatformState;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.store.model.BusinessHour;
import org.hackathon12.shophub.domain.store.model.MenuItem;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class JpaSeedDataInitializer implements ApplicationRunner {

    private static final UUID STORE_ID = UUID.fromString("0f7ed494-8e0e-4c5e-b6f4-5294ee3989d1");

    private final StoreProfileJpaRepository storeProfileJpaRepository;
    private final UserAccountJpaRepository userAccountJpaRepository;
    private final ContentItemJpaRepository contentItemJpaRepository;
    private final StoreReviewJpaRepository storeReviewJpaRepository;
    private final UserStoreMembershipJpaRepository userStoreMembershipJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public JpaSeedDataInitializer(
            StoreProfileJpaRepository storeProfileJpaRepository,
            UserAccountJpaRepository userAccountJpaRepository,
            ContentItemJpaRepository contentItemJpaRepository,
            StoreReviewJpaRepository storeReviewJpaRepository,
            UserStoreMembershipJpaRepository userStoreMembershipJpaRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.storeProfileJpaRepository = storeProfileJpaRepository;
        this.userAccountJpaRepository = userAccountJpaRepository;
        this.contentItemJpaRepository = contentItemJpaRepository;
        this.storeReviewJpaRepository = storeReviewJpaRepository;
        this.userStoreMembershipJpaRepository = userStoreMembershipJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (storeProfileJpaRepository.existsById(STORE_ID)) {
            return;
        }

        StoreProfile profile = new StoreProfile(
                STORE_ID,
                "모모커피 연남",
                "02-335-0426",
                "연남동 골목의 느린 오후를 위한 커피와 디저트.",
                "서울 마포구 동교로 242",
                "카페 · 디저트",
                "따뜻하고 담백한 동네 카페의 말투",
                List.of(
                        new BusinessHour("MON", "10:00", "21:00", true),
                        new BusinessHour("TUE", "10:00", "21:00", true),
                        new BusinessHour("WED", "10:00", "21:00", true),
                        new BusinessHour("THU", "10:00", "21:00", true),
                        new BusinessHour("FRI", "10:00", "22:00", true),
                        new BusinessHour("SAT", "11:00", "22:00", true),
                        new BusinessHour("SUN", "11:00", "20:00", true)
                ),
                List.of(
                        new MenuItem(UUID.fromString("5944f95a-b4f5-4de6-84f2-74c2e90d1ef2"), "모모 라떼", "버터 취향시에"),
                        new MenuItem(UUID.fromString("43057841-6f42-48d9-b6e7-6b215f4de4f7"), "디카페인 플랫화이트", "AI 불향")
                ),
                null,
                "https://maps.google.com",
                128,
                Instant.now()
        );
        storeProfileJpaRepository.save(StoreProfileEntity.fromDomain(profile));

        UserAccount user = new UserAccount(
                UUID.fromString("f833ee9d-ee1c-4f44-ab8f-6f5b6f61f3f2"),
                "name@business.kr",
                passwordEncoder.encode("Passw0rd!"),
                "모모커피 운영자"
        );
        UserAccountEntity savedUser = userAccountJpaRepository.save(UserAccountEntity.fromDomain(user));
        StoreProfileEntity savedStore = storeProfileJpaRepository.findById(STORE_ID).orElseThrow();

        userStoreMembershipJpaRepository.save(UserStoreMembershipEntity.of(
                UUID.randomUUID(),
                savedUser,
                savedStore,
                StoreMembershipRole.OWNER,
                Instant.now()
        ));

        Instant now = Instant.now();
        contentItemJpaRepository.saveAll(List.of(
                contentWithPlatformStates(
                        new ContentItem(
                                UUID.fromString("3034b816-757d-487f-b4c7-d1f7bea4f78f"),
                                STORE_ID,
                                "비 오는 날, 따뜻한 라떼",
                                "비 오는 오늘, 따뜻한 라떼 한 잔으로 잠깐 쉬어가세요.",
                                List.of(ContentChannel.INSTAGRAM.name()),
                                ContentStatus.PUBLISHED,
                                now.minusSeconds(3600),
                                null
                        ),
                        List.of(new ContentPlatformState(ContentChannel.INSTAGRAM, ContentChannelPublishStatus.SUCCESS))
                ),
                contentWithPlatformStates(
                        new ContentItem(
                                UUID.fromString("c68f0630-c40e-41cc-9335-39b82773f931"),
                                STORE_ID,
                                "연남동 산책 후 들르는 커피",
                                "산책 후엔 버터 취향시에와 모모 라떼가 좋아요.",
                                List.of(ContentChannel.INSTAGRAM.name(), ContentChannel.FACEBOOK.name()),
                                ContentStatus.DRAFT,
                                now.minusSeconds(7200),
                                null
                        ),
                        List.of(
                                new ContentPlatformState(ContentChannel.INSTAGRAM, ContentChannelPublishStatus.PENDING),
                                new ContentPlatformState(ContentChannel.FACEBOOK, ContentChannelPublishStatus.PENDING)
                        )
                ),
                contentWithPlatformStates(
                        new ContentItem(
                                UUID.fromString("c781ac92-ef95-4cdb-ad87-0379a826f576"),
                                STORE_ID,
                                "여름 시즌 라떼 출시",
                                "시즌 한정 라떼를 이번 주부터 제공합니다.",
                                List.of(ContentChannel.NAVER_BLOG.name()),
                                ContentStatus.FAILED,
                                now.minusSeconds(86400),
                                null
                        ),
                        List.of(new ContentPlatformState(ContentChannel.NAVER_BLOG, ContentChannelPublishStatus.FAILED))
                ),
                contentWithPlatformStates(
                        new ContentItem(
                                UUID.fromString("df7a0f74-6fc1-4af0-9f65-930d19a6ce1d"),
                                STORE_ID,
                                "7월 휴무일 안내",
                                "휴무일을 미리 확인해주세요.",
                                List.of(
                                        ContentChannel.INSTAGRAM.name(),
                                        ContentChannel.NAVER_BLOG.name(),
                                        ContentChannel.FACEBOOK.name()
                                ),
                                ContentStatus.SCHEDULED,
                                now.minusSeconds(172800),
                                null
                        ),
                        List.of(
                                new ContentPlatformState(ContentChannel.INSTAGRAM, ContentChannelPublishStatus.PENDING),
                                new ContentPlatformState(ContentChannel.NAVER_BLOG, ContentChannelPublishStatus.PENDING),
                                new ContentPlatformState(ContentChannel.FACEBOOK, ContentChannelPublishStatus.PENDING)
                        )
                )
        ));

        List<StoreReview> reviews = List.of(
                new StoreReview(
                        UUID.fromString("d2c39a88-6461-4f93-9f7f-f40103f5205c"),
                        STORE_ID,
                        "MOCK_MAP",
                        "1",
                        "하온 서",
                        5,
                        "커피도 정말 맛있고, 직원분이 메뉴를 친절하게 설명해주셨어요.",
                        now.minusSeconds(32 * 60L),
                        null,
                        null
                ),
                new StoreReview(
                        UUID.fromString("74867f6f-27f8-47dd-a838-b6fd4daa618f"),
                        STORE_ID,
                        "MOCK_MAP",
                        "2",
                        "민준 김",
                        4,
                        "조용히 일하기 좋은 카페입니다. 디카페인 옵션이 더 많아지면 좋겠어요.",
                        now.minusSeconds(2 * 3600L),
                        null,
                        null
                ),
                new StoreReview(
                        UUID.fromString("b9cfef29-b5d9-4fbb-a3f3-c4ccbb1fdcde"),
                        STORE_ID,
                        "MOCK_MAP",
                        "3",
                        "예린 박",
                        5,
                        "취향시에 선물 포장도 너무 예뻐요. 다음에도 올게요!",
                        now.minusSeconds(20 * 3600L),
                        null,
                        null
                ),
                new StoreReview(
                        UUID.fromString("66e81006-bf6f-473b-9c0f-7d4f88f56dd3"),
                        STORE_ID,
                        "MOCK_MAP",
                        "4",
                        "도현 이",
                        4,
                        "연남동에서 가장 편안한 카페 분위기예요. 라떼가 특히 좋아요.",
                        now.minusSeconds(36 * 3600L),
                        null,
                        null
                ),
                new StoreReview(
                        UUID.fromString("2d59f447-f73c-482b-a8f9-17a1af832d7b"),
                        STORE_ID,
                        "MOCK_MAP",
                        "5",
                        "서진 윤",
                        5,
                        "친절하고 분위기가 좋아요. 주말에는 조금 붐비네요.",
                        now.minusSeconds(72 * 3600L),
                        null,
                        null
                )
        );
        storeReviewJpaRepository.saveAll(reviews.stream().map(StoreReviewEntity::fromDomain).toList());
    }

    private ContentItemEntity contentWithPlatformStates(
            ContentItem contentItem,
            List<ContentPlatformState> platformStates
    ) {
        return ContentItemEntity.fromDomain(contentItem, platformStates);
    }
}
