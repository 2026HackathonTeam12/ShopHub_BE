package org.hackathon12.shophub.infrastructure.web.dashboard;

import org.hackathon12.shophub.domain.dashboard.model.DashboardOverview;
import org.hackathon12.shophub.domain.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/stores/{storeId}/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardOverview getOverview(@PathVariable UUID storeId) {
        return dashboardService.getOverview(storeId);
    }
}
