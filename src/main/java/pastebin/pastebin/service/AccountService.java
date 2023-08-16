package pastebin.pastebin.service;

import pastebin.pastebin.model.Account;

import java.util.Optional;

public interface AccountService {

    Optional<Account> GetByUsername(String username);
}
