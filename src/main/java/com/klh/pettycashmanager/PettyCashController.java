
package com.klh.pettycashmanager;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PettyCashController {

    private final CashRecordRepository repository;

    public PettyCashController(CashRecordRepository repository) {
        this.repository = repository;
    }

    // Dashboard and date-wise analytics
    @GetMapping("/")
    public String index(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            Model model) {

        List<CashRecord> allRecords = repository.findAll();

        List<CashRecord> records = filterRecords(
                allRecords, fromDate, toDate);

        double totalCountedCash = 0.0;
        double totalSurplus = 0.0;
        double totalShortage = 0.0;

        for (CashRecord record : records) {
            totalCountedCash += record.getTotalCash();

            double difference = record.getDifference();

            if (difference > 0) {
                totalSurplus += difference;
            } else if (difference < 0) {
                totalShortage += Math.abs(difference);
            }
        }

        model.addAttribute("records", records);
        model.addAttribute("totalReports", records.size());
        model.addAttribute("totalCountedCash", totalCountedCash);
        model.addAttribute("totalSurplus", totalSurplus);
        model.addAttribute("totalShortage", totalShortage);

        // Keep selected dates visible on the dashboard.
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "index";
    }

    // Save a new cash report
    @PostMapping("/save")
    public String save(
            @RequestParam String cashierName,
            @RequestParam double expectedCash,
            @RequestParam double totalCash,
            @RequestParam int totalNotes,
            RedirectAttributes redirectAttributes) {

        if (cashierName == null || cashierName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "message", "Please enter the cashier name.");
            return "redirect:/";
        }

        if (expectedCash < 0 || totalCash < 0 || totalNotes < 0) {
            redirectAttributes.addFlashAttribute(
                    "message", "Cash values cannot be negative.");
            return "redirect:/";
        }

        CashRecord record = new CashRecord();

        record.setCashierName(cashierName.trim());
        record.setExpectedCash(expectedCash);
        record.setTotalCash(totalCash);
        record.setDifference(totalCash - expectedCash);
        record.setTotalNotes(totalNotes);

        repository.save(record);

        redirectAttributes.addFlashAttribute(
                "message", "Cash report saved successfully!");

        return "redirect:/";
    }

    // Open the edit page
    @GetMapping("/edit/{id}")
    public String edit(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        CashRecord record = repository.findById(id).orElse(null);

        if (record == null) {
            redirectAttributes.addFlashAttribute(
                    "message", "Cash report not found.");
            return "redirect:/";
        }

        model.addAttribute("record", record);
        return "edit";
    }

    // Update an existing report
    @PostMapping("/update/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String cashierName,
            @RequestParam double expectedCash,
            @RequestParam double totalCash,
            @RequestParam int totalNotes,
            RedirectAttributes redirectAttributes) {

        CashRecord record = repository.findById(id).orElse(null);

        if (record == null) {
            redirectAttributes.addFlashAttribute(
                    "message", "Cash report not found.");
            return "redirect:/";
        }

        if (cashierName == null || cashierName.trim().isEmpty()
                || expectedCash < 0 || totalCash < 0 || totalNotes < 0) {

            redirectAttributes.addFlashAttribute(
                    "message", "Please enter valid report details.");
            return "redirect:/";
        }

        record.setCashierName(cashierName.trim());
        record.setExpectedCash(expectedCash);
        record.setTotalCash(totalCash);
        record.setDifference(totalCash - expectedCash);
        record.setTotalNotes(totalNotes);

        repository.save(record);

        redirectAttributes.addFlashAttribute(
                "message", "Cash report updated successfully!");

        return "redirect:/";
    }

    // Delete a report
    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        if (!repository.existsById(id)) {
            redirectAttributes.addFlashAttribute(
                    "message", "Cash report not found.");
            return "redirect:/";
        }

        repository.deleteById(id);

        redirectAttributes.addFlashAttribute(
                "message", "Cash report deleted successfully!");

        return "redirect:/";
    }

    // Export reports, optionally filtered by date
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        List<CashRecord> records = filterRecords(
                repository.findAll(), fromDate, toDate);

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');

        csv.append(
                "Report ID,Cashier Name,Expected Cash,"
                + "Counted Cash,Difference,Total Notes and Coins,Date\n");

        for (CashRecord record : records) {
            csv.append(record.getId()).append(',')
                    .append(csvEscape(record.getCashierName())).append(',')
                    .append(record.getExpectedCash()).append(',')
                    .append(record.getTotalCash()).append(',')
                    .append(record.getDifference()).append(',')
                    .append(record.getTotalNotes()).append(',')
                    .append(csvEscape(
                            record.getCreatedAt() == null
                                    ? ""
                                    : record.getCreatedAt().toString()))
                    .append('\n');
        }

        byte[] fileBytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"petty_cash_reports.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(fileBytes);
    }

    // Filter reports by the selected date range.
    private List<CashRecord> filterRecords(
            List<CashRecord> records,
            LocalDate fromDate,
            LocalDate toDate) {

        if (fromDate != null && toDate != null
                && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException(
                    "From date cannot be after To date.");
        }

        return records.stream()
                .filter(record -> {
                    LocalDateTime createdAt = record.getCreatedAt();

                    // When no dates are selected, include all reports.
                    if (fromDate == null && toDate == null) {
                        return true;
                    }

                    // A report without a date cannot match a date filter.
                    if (createdAt == null) {
                        return false;
                    }

                    LocalDate reportDate = createdAt.toLocalDate();

                    boolean afterStart =
                            fromDate == null
                            || !reportDate.isBefore(fromDate);

                    boolean beforeEnd =
                            toDate == null
                            || !reportDate.isAfter(toDate);

                    return afterStart && beforeEnd;
                })
                .collect(Collectors.toList());
    }

    // Escape CSV values containing commas, quotes or newlines.
    private String csvEscape(String value) {
        if (value == null) {
            return "\"\"";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
