package pastebin.pastebin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.repository.AccountRepository;

import java.util.Optional;

@Service
public class AccountServiceImp implements AccountService {

    private final AccountRepository accountRepository;

    @Autowired
    public AccountServiceImp(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Optional<Account> GetByUsername(String username) {
        return accountRepository.findByUsername(username);
    }
}
