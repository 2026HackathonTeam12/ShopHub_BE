package org.hackathon12.shophub.domain.store.service;

import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.global.error.ForbiddenException;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.hackathon12.shophub.infrastructure.persistence.StoreMembershipRole;
import org.hackathon12.shophub.infrastructure.persistence.StoreProfileEntity;
import org.hackathon12.shophub.infrastructure.persistence.StoreProfileJpaRepository;
import org.hackathon12.shophub.infrastructure.persistence.UserAccountEntity;
import org.hackathon12.shophub.infrastructure.persistence.UserAccountJpaRepository;
import org.hackathon12.shophub.infrastructure.persistence.UserStoreMembershipEntity;
import org.hackathon12.shophub.infrastructure.persistence.UserStoreMembershipJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StoreMembershipService {

    private final UserStoreMembershipJpaRepository membershipRepository;
    private final UserAccountJpaRepository userAccountRepository;
    private final StoreProfileJpaRepository storeProfileRepository;

    public StoreMembershipService(
            UserStoreMembershipJpaRepository membershipRepository,
            UserAccountJpaRepository userAccountRepository,
            StoreProfileJpaRepository storeProfileRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.storeProfileRepository = storeProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<StoreProfile> findStoresForUser(UUID userId) {
        return membershipRepository.findByUser_Id(userId).stream()
                .map(membership -> membership.getStore().toDomain())
                .toList();
    }

    @Transactional
    public void ensureOwnerMembership(UUID userId, UUID storeId) {
        if (membershipRepository.existsByUser_IdAndStore_Id(userId, storeId)) {
            return;
        }

        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));
        StoreProfileEntity store = storeProfileRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("가게를 찾을 수 없습니다. storeId=" + storeId));

        membershipRepository.save(UserStoreMembershipEntity.of(
                UUID.randomUUID(),
                user,
                store,
                StoreMembershipRole.OWNER,
                Instant.now()
        ));
    }

    @Transactional(readOnly = true)
    public void requireMembership(UUID userId, UUID storeId) {
        if (!storeProfileRepository.existsById(storeId)) {
            throw new NotFoundException("가게를 찾을 수 없습니다. storeId=" + storeId);
        }
        if (!membershipRepository.existsByUser_IdAndStore_Id(userId, storeId)) {
            throw new ForbiddenException("가게에 대한 접근 권한이 없습니다. storeId=" + storeId);
        }
    }
}
