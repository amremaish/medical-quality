package com.app.medicalquality.record.controller;

import com.app.medicalquality.auth.security.annotation.HasPermission;
import com.app.medicalquality.common.AppResponse;
import com.app.medicalquality.record.dto.MrRequests;
import com.app.medicalquality.record.dto.MrViews;
import com.app.medicalquality.record.service.MrReviewService;
import com.app.medicalquality.role.PermissionEnum;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MrReviewController {

    private final MrReviewService mrReviewService;

    public MrReviewController(MrReviewService a_mrReviewService) {
        this.mrReviewService = a_mrReviewService;
    }

    @GetMapping("/api/record-reviews")
    public ResponseEntity<AppResponse<List<MrViews.ReviewSummary>>> list(Authentication a_authentication) {
        return AppResponse.success(mrReviewService.list(a_authentication));
    }

    @PostMapping("/api/record-reviews")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<MrViews.ReviewSummary>> create(
            @Valid @RequestBody MrRequests.Create a_request, Authentication a_authentication) {
        return AppResponse.success(
                mrReviewService.create(a_request, a_authentication), "Successfully Done", HttpStatus.CREATED);
    }

    @GetMapping("/api/record-reviews/{reviewId}")
    public ResponseEntity<AppResponse<MrViews.ReviewDetail>> detail(@PathVariable("reviewId") Long a_reviewId) {
        return AppResponse.success(mrReviewService.detail(a_reviewId));
    }

    @PutMapping("/api/record-reviews/{reviewId}/answers")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<MrViews.ReviewDetail>> answer(
            @PathVariable("reviewId") Long a_reviewId, @Valid @RequestBody MrRequests.Answer a_request) {
        return AppResponse.success(mrReviewService.saveAnswer(a_reviewId, a_request));
    }

    @PutMapping("/api/record-reviews/{reviewId}/records/{code}")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<MrViews.ReviewDetail>> patient(
            @PathVariable("reviewId") Long a_reviewId,
            @PathVariable("code") String a_code,
            @RequestBody MrRequests.RecordId a_request) {
        return AppResponse.success(mrReviewService.setPatient(a_reviewId, a_code, a_request.getPatientIdentifier()));
    }

    @PostMapping("/api/record-reviews/{reviewId}/reset")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<MrViews.ReviewDetail>> reset(@PathVariable("reviewId") Long a_reviewId) {
        return AppResponse.success(mrReviewService.reset(a_reviewId));
    }
}
