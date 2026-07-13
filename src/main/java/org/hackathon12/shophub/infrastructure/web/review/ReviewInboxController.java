package org.hackathon12.shophub.infrastructure.web.review;

import org.hackathon12.shophub.domain.review.model.ReviewInbox;
import org.hackathon12.shophub.domain.review.service.ReviewInboxService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/v1/reviews")
public class ReviewInboxController {

    private final ReviewInboxService reviewInboxService;

    public ReviewInboxController(ReviewInboxService reviewInboxService) {
        this.reviewInboxService = reviewInboxService;
    }

    @GetMapping("/inbox")
    public ReviewInbox getInbox(@RequestParam List<String> placeIds) {
        if (CollectionUtils.isEmpty(placeIds)) {
            throw new ResponseStatusException(BAD_REQUEST, "최소 1개 이상의 placeIds가 필요합니다.");
        }
        return reviewInboxService.getUnifiedInbox(placeIds);
    }
}
