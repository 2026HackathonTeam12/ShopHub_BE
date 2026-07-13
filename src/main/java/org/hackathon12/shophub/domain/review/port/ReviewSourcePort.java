package org.hackathon12.shophub.domain.review.port;

import org.hackathon12.shophub.domain.review.model.ReviewSourceData;

public interface ReviewSourcePort {

    ReviewSourceData fetchByPlaceId(String placeId);
}
