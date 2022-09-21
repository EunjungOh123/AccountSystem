package com.example.bank_account.repository;

import com.example.bank_account.domain.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {

}
