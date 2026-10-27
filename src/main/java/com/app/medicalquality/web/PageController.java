package com.app.medicalquality.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/audits")
    public String audits() {
        return "audits";
    }

    @GetMapping("/audits/new")
    public String newAudit() {
        return "audit-new";
    }

    @GetMapping("/audits/{id}")
    public String audit(@PathVariable("id") Long a_id, Model a_model) {
        a_model.addAttribute("auditId", a_id);
        return "audit";
    }

    @GetMapping("/record-reviews")
    public String reviews() {
        return "record-reviews";
    }

    @GetMapping("/record-reviews/new")
    public String newReview() {
        return "record-review-new";
    }

    @GetMapping("/record-reviews/{id}")
    public String review(@PathVariable("id") Long a_id, Model a_model) {
        a_model.addAttribute("reviewId", a_id);
        return "record-review";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model a_model) {
        a_model.addAttribute("title", "Command center");
        return "dashboard";
    }

    @GetMapping("/dashboard/{view}")
    public String dashboardView(@PathVariable("view") String a_view, Model a_model) {
        a_model.addAttribute("title", dashboardTitle(a_view));
        return "dashboard";
    }

    @GetMapping("/departments")
    public String departments() {
        return "departments";
    }

    @GetMapping("/departments/new")
    public String newDepartment() {
        return "department-new";
    }

    @GetMapping("/assessors")
    public String assessors() {
        return "assessors";
    }

    @GetMapping("/findings")
    public String findings() {
        return "findings";
    }

    @GetMapping("/capas")
    public String capas() {
        return "capas";
    }

    @GetMapping("/capas/new")
    public String newCapa() {
        return "capa-new";
    }

    @GetMapping("/evidence")
    public String evidence() {
        return "evidence";
    }

    @GetMapping("/evidence/new")
    public String newEvidence() {
        return "evidence-new";
    }

    @GetMapping("/imports")
    public String imports() {
        return "imports";
    }

    @GetMapping("/imports/new")
    public String newImport() {
        return "import-new";
    }

    @GetMapping("/organizations")
    public String organizations() {
        return "organizations";
    }

    @GetMapping("/organizations/new")
    public String newOrganization() {
        return "organizations-new";
    }

    @GetMapping("/hospitals")
    public String hospitals() {
        return "redirect:/organizations";
    }

    @GetMapping("/owners")
    public String owners() {
        return "owners";
    }

    @GetMapping("/owners/new")
    public String newOwner() {
        return "owners-new";
    }

    @GetMapping("/account/password")
    public String password() {
        return "password";
    }

    private String dashboardTitle(String a_view) {
        return switch (a_view) {
            case "executive" -> "Executive view";
            case "assessor" -> "Assessor view";
            case "quality-director" -> "Quality director view";
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }
}
