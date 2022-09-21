package com.example.bank_account.service;

import com.example.bank_account.Exception.AccountException;
import com.example.bank_account.domain.Account;
import com.example.bank_account.domain.AccountStatus;
import com.example.bank_account.domain.AccountUser;
import com.example.bank_account.dto.AccountDto;
import com.example.bank_account.repository.AccountRepository;
import com.example.bank_account.repository.AccountUserRepository;
import com.example.bank_account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.bank_account.domain.AccountStatus.IN_USE;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /** 계좌 생성
     * 사용자가 있는지 조회
     * 계좌의 번호를 생성하고
     * 계좌를 저장하고, 그 정보를 넘긴다
     */

    /**
     * 계좌 생성 시 정책
     * 사용자 없는 경우 실패 응답
     * 계좌가 10개(사용자당 최대 보유 가능 계좌 수)인 경우 실패 응답
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = getAccountUser(userId);

        validateCreateAccount(accountUser);

        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber()) + 1 + ""))
                .orElse("1000000000");

        return AccountDto.fromEntity(accountRepository.save(
                Account.builder().accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .accountNumber(newAccountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build()));
    }
    private void validateCreateAccount(AccountUser accountUser) {
        if(accountRepository.countByAccountUser(accountUser) == 10) {
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_10);
        }
    }


    @Transactional
    public Account getAccount(Long id) {
        if(id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

    /** 계좌 삭제 시 정책
     * 사용자 또는 계좌가 없는 경우 실패 응답
     * 사용자 아이디와 계좌 소유주가 다른 경우 실패 응답
     * 계좌가 이미 해지 상태인 경우, 잔액이 있는 경우 실패 응답
     */
    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = getAccountUser(userId);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account); // 안 써도 되지만 test 편의를 위해

        return AccountDto.fromEntity(account);
    }
    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if(!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        if(account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if(account.getBalance() > 0) {
            throw new AccountException(ErrorCode.BALANCE_NOT_EMPTY);
        }
    }
    /** 계좌 확인 시 정책
     * 사용자 없는 경우 실패 응답
     */
    @Transactional
    public List<AccountDto> getAccountByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);
        List<Account> accounts = accountRepository
                .findByAccountUser(accountUser);
        return accounts.stream()
                .map(AccountDto :: fromEntity)
                .collect(Collectors.toList());
    }

    private AccountUser getAccountUser(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        return accountUser;
    }
}
