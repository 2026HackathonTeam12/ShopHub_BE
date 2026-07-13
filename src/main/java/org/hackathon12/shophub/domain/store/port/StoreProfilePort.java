package org.hackathon12.shophub.domain.store.port;

import org.hackathon12.shophub.domain.store.model.StoreProfile;

import java.util.List;
import java.util.UUID;

public interface StoreProfilePort {

    List<StoreProfile> findAll();

    StoreProfile findById(UUID storeId);

    StoreProfile save(StoreProfile storeProfile);
}
