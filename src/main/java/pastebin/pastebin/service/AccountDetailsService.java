package pastebin.pastebin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.repository.AccountRepository;
import pastebin.pastebin.security.AccountDetails;

import java.util.Optional;

@Service
public class AccountDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Autowired
    public AccountDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        Optional<Account> accountByUsername = accountRepository.findByUsername(s);
        if (accountByUsername.isPresent()) {
            return new AccountDetails(accountByUsername.get());
        }

        Optional<Account> accountByEmail = accountRepository.findByEmail(s);
        if (accountByEmail.isPresent()) {
            return new AccountDetails(accountByEmail.get());
        }

        throw new UsernameNotFoundException("User not found");
    }
}
