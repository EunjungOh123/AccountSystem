package com.example.bank_account.service;

import com.example.bank_account.Exception.AccountException;
import com.example.bank_account.domain.Account;
import com.example.bank_account.domain.AccountStatus;
import com.example.bank_account.domain.AccountUser;
import com.example.bank_account.domain.Transaction;
import com.example.bank_account.dto.TransactionDto;
import com.example.bank_account.repository.AccountRepository;
import com.example.bank_account.repository.AccountUserRepository;
import com.example.bank_account.repository.TransactionRepository;
import com.example.bank_account.type.ErrorCode;
import com.example.bank_account.type.TransactionResultType;
import com.example.bank_account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    /**
     * 잔액 사용 정책
     * 사용자 또는 계좌가 없는 경우 실패 응답
     * 사용자 아이디와 계좌 소유주가 다른 경우 실패 응답
     * 계좌가 이미 해지 상태인 경우 실패 응답
     * 거래 금액이 잔액보다 큰 경우, 거래 금액이 너무 작거나 큰 경우 실패 응답
     */
    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {
        AccountUser user = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateUseBalance(user, account, amount);

        account.useBalance(amount);

        return TransactionDto.fromEntity(saveAndGetTransaction(TransactionType.USE, TransactionResultType.S, account, amount));
    }

    private void validateUseBalance(AccountUser user, Account account, Long amount) {
        if(!Objects.equals(user.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(TransactionType.USE, TransactionResultType.F, account, amount);
    }

    private Transaction saveAndGetTransaction(
            TransactionType transactionType,
            TransactionResultType transactionResultType,
            Account account, Long amount) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapShot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }
    /**
     * 잔액 사용 취소 정책
     * 거래 아이디에 해당하는 거래가 없는 경우 실패 응답
     * 거래 금액과 취소 금액이 다른 경우 (부분 취소 불가능) 실패 응답
     * 1년이 넘은 거래는 사용 취소 불가능
     * 해당 계좌에서 거래가 진행 중일 때, 다른 거래 요청이 있는 경우 해당 거래가 동시에 잘못 처리되는 것을 방지
     */
    @Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateCancelBalance(transaction, account, amount);

        account.cancelBalance(amount);

        return TransactionDto.fromEntity(
                saveAndGetTransaction(TransactionType.CANCEL, TransactionResultType.S, account, amount));
    }

    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        if(!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }
        if(!transaction.getAmount().equals(amount)) {
            throw new AccountException(ErrorCode.CANCEL_MUST_FULLY);
        }
        if(transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(ErrorCode.TOO_OLD_TO_CANCEL);
        }
    }
    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(TransactionType.CANCEL, TransactionResultType.F, account, amount);
    }

    /**
     * 잔액 사용 확인 정책
     * 해당 거래 아이디의 거래가 없는 경우 실패 응답
     */
    public TransactionDto queryTransaction(String transactionId) {

        return TransactionDto.fromEntity(transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND)));
    }
}
