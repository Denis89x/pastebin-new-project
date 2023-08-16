package pastebin.pastebin.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.repository.AccountRepository;

@Service
public class RegistrationService {

    private final AccountRepository accountRepository;

    private final PasswordEncoder passwordEncoder;

    public RegistrationService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(Account account) {
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        account.setRole("ROLE_USER");
        account.setIsVerifiedEmail(false);

        accountRepository.save(account);
    }
}
