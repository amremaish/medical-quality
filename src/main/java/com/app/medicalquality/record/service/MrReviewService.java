package com.app.medicalquality.record.service;

import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.record.dto.MrRequests;
import com.app.medicalquality.record.dto.MrViews;
import com.app.medicalquality.record.entity.MrAnswer;
import com.app.medicalquality.record.entity.MrItem;
import com.app.medicalquality.record.entity.MrRecord;
import com.app.medicalquality.record.entity.MrReview;
import com.app.medicalquality.record.repository.MrAnswerRepository;
import com.app.medicalquality.record.repository.MrItemRepository;
import com.app.medicalquality.record.repository.MrRecordRepository;
import com.app.medicalquality.record.repository.MrReviewRepository;
import com.app.medicalquality.score.RecordScoreCalculator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MrReviewService {

    private final MrReviewRepository reviewRepository;
    private final MrRecordRepository recordRepository;
    private final MrItemRepository itemRepository;
    private final MrAnswerRepository answerRepository;
    private final CurrentUserService currentUserService;

    public MrReviewService(
            MrReviewRepository a_reviewRepository,
            MrRecordRepository a_recordRepository,
            MrItemRepository a_itemRepository,
            MrAnswerRepository a_answerRepository,
            CurrentUserService a_currentUserService) {
        this.reviewRepository = a_reviewRepository;
        this.recordRepository = a_recordRepository;
        this.itemRepository = a_itemRepository;
        this.answerRepository = a_answerRepository;
        this.currentUserService = a_currentUserService;
    }

    @Transactional
    public MrViews.ReviewSummary create(MrRequests.Create a_request, Authentication a_authentication) {
        MrReview review = new MrReview();
        review.setHospitalName(a_request.getHospitalName().trim());
        review.setReviewerName(a_request.getReviewerName().trim());
        review.setReviewDate(a_request.getReviewDate());
        review.setDepartmentName(a_request.getDepartmentName().trim());
        review.setCreatedBy(currentUserService.require(a_authentication));
        reviewRepository.save(review);
        for (int index = 1; index <= 10; index++) {
            MrRecord record = new MrRecord();
            record.setReview(review);
            record.setCode("MR." + index);
            recordRepository.save(record);
        }
        return summary(review);
    }

    @Transactional(readOnly = true)
    public List<MrViews.ReviewSummary> list() {
        return reviewRepository.findByDeletedAtIsNullOrderByReviewDateDesc().stream()
                .map(this::summary)
                .toList();
    }

    @Transactional(readOnly = true)
    public MrViews.ReviewDetail detail(Long a_reviewId) {
        MrReview review = require(a_reviewId);
        List<MrItem> items = itemRepository.findByDeletedAtIsNullOrderBySection_SortOrderAscSortOrderAsc();
        Map<String, MrAnswer> answers = answers(a_reviewId);
        return new MrViews.ReviewDetail(
                review.getId(),
                review.getHospitalName(),
                review.getReviewerName(),
                review.getReviewDate(),
                review.getDepartmentName(),
                items.stream().map(this::itemLine).toList(),
                records(a_reviewId, items, answers));
    }

    @Transactional
    public MrViews.ReviewDetail saveAnswer(Long a_reviewId, MrRequests.Answer a_request) {
        MrReview review = require(a_reviewId);
        MrRecord record = recordRepository
                .findByReviewIdAndCodeAndDeletedAtIsNull(review.getId(), a_request.getRecordCode())
                .orElseThrow(() -> new NotFoundException("error.record.not_found"));
        MrItem item = itemRepository
                .findByIdAndDeletedAtIsNull(a_request.getItemId())
                .orElseThrow(() -> new NotFoundException("error.item.not_found"));
        MrAnswer answer = answerRepository
                .findByRecordIdAndItemId(record.getId(), item.getId())
                .orElseGet(MrAnswer::new);
        answer.setRecord(record);
        answer.setItem(item);
        answer.setLegible(a_request.isLegible());
        answer.setAccurate(a_request.isAccurate());
        answer.setComplete(a_request.isComplete());
        answer.setNa(a_request.isNa());
        answer.setComment(a_request.getComment());
        answer.setDeletedAt(null);
        answerRepository.save(answer);
        return detail(a_reviewId);
    }

    @Transactional
    public MrViews.ReviewDetail setPatient(Long a_reviewId, String a_code, String a_patientIdentifier) {
        require(a_reviewId);
        MrRecord record = recordRepository
                .findByReviewIdAndCodeAndDeletedAtIsNull(a_reviewId, a_code)
                .orElseThrow(() -> new NotFoundException("error.record.not_found"));
        record.setPatientIdentifier(a_patientIdentifier == null ? "" : a_patientIdentifier.trim());
        return detail(a_reviewId);
    }

    @Transactional
    public MrViews.ReviewDetail reset(Long a_reviewId) {
        require(a_reviewId);
        answerRepository.deleteByRecord_Review_Id(a_reviewId);
        recordRepository.findByReviewIdAndDeletedAtIsNullOrderByCodeAsc(a_reviewId).forEach(a_record -> a_record.setPatientIdentifier(""));
        return detail(a_reviewId);
    }

    private MrReview require(Long a_reviewId) {
        return reviewRepository
                .findByIdAndDeletedAtIsNull(a_reviewId)
                .orElseThrow(() -> new NotFoundException("error.review.not_found"));
    }

    private MrViews.ReviewSummary summary(MrReview a_review) {
        return new MrViews.ReviewSummary(
                a_review.getId(),
                a_review.getHospitalName(),
                a_review.getReviewerName(),
                a_review.getReviewDate(),
                a_review.getDepartmentName());
    }

    private MrViews.ItemLine itemLine(MrItem a_item) {
        return new MrViews.ItemLine(
                a_item.getId(),
                a_item.getSection().getName(),
                a_item.getStandardCode(),
                a_item.getEocNo(),
                a_item.getBody());
    }

    private Map<String, MrAnswer> answers(Long a_reviewId) {
        Map<String, MrAnswer> byKey = new LinkedHashMap<>();
        for (MrAnswer answer : answerRepository.findByRecord_Review_IdAndDeletedAtIsNull(a_reviewId)) {
            byKey.put(key(answer.getRecord().getId(), answer.getItem().getId()), answer);
        }
        return byKey;
    }

    private List<MrViews.RecordLine> records(Long a_reviewId, List<MrItem> a_items, Map<String, MrAnswer> a_answers) {
        List<MrViews.RecordLine> lines = new ArrayList<>();
        for (MrRecord record : recordRepository.findByReviewIdAndDeletedAtIsNullOrderByCodeAsc(a_reviewId)) {
            lines.add(recordLine(record, a_items, a_answers));
        }
        return lines;
    }

    private MrViews.RecordLine recordLine(MrRecord a_record, List<MrItem> a_items, Map<String, MrAnswer> a_answers) {
        List<MrViews.AnswerLine> lines = new ArrayList<>();
        List<RecordScoreCalculator.Mark> marks = new ArrayList<>();
        for (MrItem item : a_items) {
            MrAnswer answer = a_answers.get(key(a_record.getId(), item.getId()));
            boolean legible = answer != null && answer.isLegible();
            boolean accurate = answer != null && answer.isAccurate();
            boolean complete = answer != null && answer.isComplete();
            boolean na = answer != null && answer.isNa();
            String comment = answer == null ? "" : answer.getComment();
            marks.add(new RecordScoreCalculator.Mark(legible, accurate, complete, na));
            lines.add(new MrViews.AnswerLine(
                    item.getId(),
                    legible,
                    accurate,
                    complete,
                    na,
                    comment,
                    RecordScoreCalculator.tone(legible, accurate, complete, na)));
        }
        return new MrViews.RecordLine(
                a_record.getCode(),
                a_record.getPatientIdentifier(),
                RecordScoreCalculator.summarize(marks),
                lines);
    }

    private String key(Long a_recordId, Long a_itemId) {
        return a_recordId + ":" + a_itemId;
    }
}
