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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance () {
    // given
        AccountUser user = AccountUser.builder()
                .id(12L).name("Kevin").build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapShot(9000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    // when
        TransactionDto transactionDto = transactionService.useBalance(1L, "1000000000", 200L);
    // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapShot());
        assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapShot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void useBalance_UserNotFound () {
        // given

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void useBalance_AccountNotFound () {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L).name("Kevin").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }
    @Test
    @DisplayName("계좌 소유주 다름 - 잔액 사용 실패")
    void useBalance_userUnMatch () {
        // given
        AccountUser Kevin = AccountUser.builder()
                .id(12L).name("Kevin").build();

        AccountUser Grace = AccountUser.builder()
                .id(13L).name("Grace").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(Kevin));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(Grace)
                        .balance(0L)
                        .accountNumber("1000000012").build()));
        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));
        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }
    @Test
    @DisplayName("해지 계좌는 해지할 수 없다 - 잔액 사용 실패")
    void deleteAccountFailed_alreadyUnregistered () {
        // given
        AccountUser Kevin = AccountUser.builder()
                .id(12L).name("Kevin").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(Kevin));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(Kevin)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012").build()));
        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }
    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우 - 잔액 사용 실패")
    void exceedAmount_UseBalance () {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L).name("Kevin").build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(100L)
                .accountNumber("1000000012").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));
        // then
        verify(transactionRepository, times(0)).save(any());
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    }
    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L).name("Kevin").build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapShot(9000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        // when
        transactionService.saveFailedUseTransaction( "1000000000", 200L);
        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapShot());
        assertEquals(TransactionResultType.F, captor.getValue().getTransactionResultType());
    }
    @Test
    void successCancelBalance () {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L).name("Kevin").build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(200L)
                .balanceSnapShot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.CANCEL)
                        .transactionResultType(TransactionResultType.S)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(200L)
                        .balanceSnapShot(10000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        // when
        TransactionDto transactionDto = transactionService.cancelBalance("transactionId", "1000000000", 200L);
        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(10200L, captor.getValue().getBalanceSnapShot());
        assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
        assertEquals(TransactionType.CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapShot());
        assertEquals(200L, transactionDto.getAmount());
    }
    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_AccountNotFound () {
        // given

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }
    @Test
    @DisplayName("원 사용 거래 없음 - 잔액 사용 취소 실패")
    void cancelTransaction_TransactionNotFound () {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
    @Test
    @DisplayName("거래와 계좌가 매치 실패 - 잔액 사용 취소 실패")
    void cancelTransaction_TRANSACTIONACCOUNTUNMATCH () {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L).name("Kevin").build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        Account accountNotUse = Account.builder()
                .id(2L)
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000013").build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(200L)
                .balanceSnapShot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService
                        .cancelBalance("transactionId", "1000000000", 200L));

        // then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }
    @Test
    @DisplayName("거래 금액과 취소 금액이 다름 - 잔액 사용 취소 실패")
    void cancelTransaction_CANCELMUSTFULLY () {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L).name("Kevin").build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1200L)
                .balanceSnapShot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService
                        .cancelBalance("transactionId", "1000000000", 200L));

        // then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
    }
    @Test
    @DisplayName("취소는 1년까지만 가능 - 잔액 사용 취소 실패")
    void cancelTransaction_TOO_OLD_TO_CANCEL () {
        // given
        AccountUser user = AccountUser.builder()
                .id(12L).name("Kevin").build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(200L)
                .balanceSnapShot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService
                        .cancelBalance("transactionId", "1000000000", 200L));

        // then
        assertEquals(ErrorCode.TOO_OLD_TO_CANCEL, exception.getErrorCode());
    }
    @Test
    void successQueryTransaction() {
    // given
        AccountUser user = AccountUser.builder()
                .id(12L).name("Kevin").build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(200L)
                .balanceSnapShot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
    // when
        TransactionDto transactionDto
                = transactionService.queryTransaction("trxId");
    // then
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
        assertEquals(200L, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }
    @Test
    @DisplayName("원 거래 없음 - 거래 조회 실패")
    void queryTransaction_TransactionNotFound () {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}