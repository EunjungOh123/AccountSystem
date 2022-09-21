package com.example.bank_account.dto;

import com.example.bank_account.domain.Account;
import com.example.bank_account.domain.Transaction;
import com.example.bank_account.type.TransactionResultType;
import com.example.bank_account.type.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDto {
    private String accountNumber;
    private TransactionType transactionType;
    private TransactionResultType transactionResultType;
    private Long amount;
    private Long balanceSnapShot;
    private String transactionId;
    private LocalDateTime transactedAt;

    public static TransactionDto fromEntity(Transaction transaction) {
        return TransactionDto.builder()
                .accountNumber(transaction.getAccount().getAccountNumber())
                .transactionType(transaction.getTransactionType())
                .transactionResultType(transaction.getTransactionResultType())
                .amount(transaction.getAmount())
                .balanceSnapShot(transaction.getBalanceSnapShot())
                .transactionId(transaction.getTransactionId())
                .transactedAt(transaction.getTransactedAt())
                .build();
    }
}
