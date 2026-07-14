package org.hackathon12.shophub.infrastructure.web.dashboard;

import org.hackathon12.shophub.domain.dashboard.model.DashboardOverview;
import org.hackathon12.shophub.domain.dashboard.service.DashboardService;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stores/{storeId}/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public DashboardController(DashboardService dashboardService, ShopHubAuthGuard shopHubAuthGuard) {
        this.dashboardService = dashboardService;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @GetMapping
    public DashboardOverview getOverview(@PathVariable UUID storeId, HttpServletRequest request) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return dashboardService.getOverview(storeId);
    }
}
