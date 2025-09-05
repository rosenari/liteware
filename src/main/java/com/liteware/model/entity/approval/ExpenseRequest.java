package com.liteware.model.entity.approval;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expense_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long expenseRequestId;
    
    @OneToOne
    @JoinColumn(name = "doc_id", nullable = false)
    private ApprovalDocument document;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;     // 총 청구 금액
    
    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;   // 경비 카테고리
    
    private String projectCode;         // 프로젝트 코드
    
    private String projectName;         // 프로젝트명
    
    private String customerName;        // 고객사명
    
    @Column(columnDefinition = "TEXT")
    private String purpose;             // 사용 목적
    
    @OneToMany(mappedBy = "expenseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseItem> expenseItems = new ArrayList<>(); // 경비 항목들
    
    private String bankName;            // 은행명
    
    private String accountNumber;       // 계좌번호
    
    private String accountHolder;       // 예금주
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // 지급 상태
    
    private LocalDate paidDate;         // 지급일
    
    // 경비 카테고리
    public enum ExpenseCategory {
        BUSINESS_TRIP("출장비"),
        ENTERTAINMENT("접대비"),
        OFFICE_SUPPLIES("사무용품"),
        EDUCATION("교육비"),
        TRANSPORTATION("교통비"),
        MEAL("식대"),
        COMMUNICATION("통신비"),
        OTHER("기타");
        
        private final String description;
        
        ExpenseCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 지급 상태
    public enum PaymentStatus {
        PENDING("지급대기"),
        PAID("지급완료"),
        REJECTED("지급거부");
        
        private final String description;
        
        PaymentStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 경비 항목 추가
    public void addExpenseItem(ExpenseItem item) {
        expenseItems.add(item);
        item.setExpenseRequest(this);
        calculateTotalAmount();
    }
    
    // 경비 항목 제거
    public void removeExpenseItem(ExpenseItem item) {
        expenseItems.remove(item);
        item.setExpenseRequest(null);
        calculateTotalAmount();
    }
    
    // 총 금액 계산
    public void calculateTotalAmount() {
        this.totalAmount = expenseItems.stream()
                .map(ExpenseItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

@Entity
@Table(name = "expense_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ExpenseItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_request_id")
    private ExpenseRequest expenseRequest;
    
    @Column(nullable = false)
    private LocalDate expenseDate;      // 사용일
    
    @Column(nullable = false)
    private String description;         // 내역
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;          // 금액
    
    private String vendor;              // 거래처
    
    private String receiptNumber;       // 영수증 번호
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod; // 결제 방법
    
    @Column(columnDefinition = "TEXT")
    private String remarks;             // 비고
    
    // 결제 방법
    public enum PaymentMethod {
        CASH("현금"),
        CORPORATE_CARD("법인카드"),
        PERSONAL_CARD("개인카드"),
        BANK_TRANSFER("계좌이체");
        
        private final String description;
        
        PaymentMethod(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}