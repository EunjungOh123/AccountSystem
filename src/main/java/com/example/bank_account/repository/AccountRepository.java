package com.example.bank_account.repository;

import com.example.bank_account.domain.Account;
import com.example.bank_account.domain.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findFirstByOrderByIdDesc();

    Integer countByAccountUser(AccountUser accountUser);
    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByAccountUser(AccountUser accountUser);
}
