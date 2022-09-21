package com.example.bank_account.controller;

import com.example.bank_account.dto.AccountInfo;
import com.example.bank_account.domain.Account;
import com.example.bank_account.dto.CreateAccount;
import com.example.bank_account.dto.DeleteAccount;
import com.example.bank_account.service.AccountService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 계좌 관련 컨트롤러
 * 1. 계좌 생성
 * 2. 계좌 해지
 * 3. 계좌 확인
 */

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/account")
    public CreateAccount.Response createAccount (
            @RequestBody @Valid CreateAccount.Request request
            ) {
        return CreateAccount.Response.from(accountService.createAccount(request.getUserId(),
                request.getInitialBalance()));
    }

    @DeleteMapping("/account")
    public DeleteAccount.Response deleteAccount (
            @RequestBody @Valid DeleteAccount.Request request
    ) {
        return DeleteAccount.Response.from(accountService.deleteAccount(
                request.getUserId(),
                request.getAccountNumber()));
    }

    @GetMapping("/account")
    public List<AccountInfo> getAccountsByUserId (
            @RequestParam("user_id") Long userId
    ) {
        return accountService.getAccountByUserId(userId)
                .stream().map(accountDto -> AccountInfo.builder()
                        .accountNumber(accountDto.getAccountNumber())
                        .balance(accountDto.getBalance())
                        .build()).collect(Collectors.toList());
    }

    @GetMapping("/account/{id}")
    public Account getAccount (
            @PathVariable Long id
    ) {
        return accountService.getAccount(id);
    }


}
