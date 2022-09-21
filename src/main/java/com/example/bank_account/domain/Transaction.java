package com.example.bank_account.domain;

import com.example.bank_account.type.TransactionResultType;
import com.example.bank_account.type.TransactionType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

   @Id
   @GeneratedValue
   private Long id;

   //
    @Enumerated(EnumType.STRING)
     private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private TransactionResultType transactionResultType;

    @ManyToOne
    private Account account;
    private Long amount;
    private Long balanceSnapShot;

    private String transactionId;
    private LocalDateTime transactedAt;
                                        // 실제 비즈니스에 쓰이는 부분

   @CreatedDate
   private LocalDateTime createdAt;
   @LastModifiedDate
   private LocalDateTime updatedAt;

}
