
package com.klh.pettycashmanager;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_records")
public class CashRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashier_name")
    private String cashierName;

    @Column(name = "expected_cash")
    private double expectedCash;

    @Column(name = "total_cash")
    private double totalCash;

    @Column(name = "difference")
    private double difference;

    @Column(name = "total_notes")
    private int totalNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public CashRecord() {
    }

    @PrePersist
    public void beforeSave() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public double getExpectedCash() {
        return expectedCash;
    }

    public void setExpectedCash(double expectedCash) {
        this.expectedCash = expectedCash;
    }

    public double getTotalCash() {
        return totalCash;
    }

    public void setTotalCash(double totalCash) {
        this.totalCash = totalCash;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }

    public int getTotalNotes() {
        return totalNotes;
    }

    public void setTotalNotes(int totalNotes) {
        this.totalNotes = totalNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
