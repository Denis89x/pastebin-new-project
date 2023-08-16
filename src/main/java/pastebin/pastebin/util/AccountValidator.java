package pastebin.pastebin.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.repository.AccountRepository;
import pastebin.pastebin.service.AccountDetailsService;

import java.util.Optional;

@Component
public class AccountValidator implements Validator {

    private final AccountRepository accountRepository;

    private final AccountDetailsService accountDetailsService;

    @Autowired
    public AccountValidator(AccountRepository accountRepository, AccountDetailsService accountDetailsService) {
        this.accountRepository = accountRepository;
        this.accountDetailsService = accountDetailsService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Account.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Account account = (Account) target;
        String password = account.getPassword();
        String email = account.getEmail();

        // Проверка длины пароля
        if (password.length() < 4 || password.length() > 15) {
            errors.rejectValue("password", "", "Пароль должен быть длиной от 4 до 15 символов");
        }

        // Проверка наличия латинских букв
        if (!password.matches(".*[a-zA-Z].*")) {
            errors.rejectValue("password", "", "Пароль должен содержать латинские буквы");
        }

        // Проверка наличия хотя бы одной цифры
        if (!password.matches(".*\\d.*")) {
            errors.rejectValue("password", "", "Пароль должен содержать хотя бы одну цифру");
        }

        // Проверка наличия знака
        if (!password.matches(".*[!@#$%^&*()].*")) {
            errors.rejectValue("password", "", "Пароль должен содержать хотя бы один знак");
        }

        if (!account.getUsername().matches("(?=.*[a-zA-Z])[a-zA-Z0-9_]+")) {
            errors.rejectValue("username", "", "Логин должен содержать только латинские буквы, цифры и символ '_'");
        }

        Optional<Account> newAccount = accountRepository.findByEmail(email);

        if (newAccount.isPresent())
            errors.rejectValue("email", "", "Эта почта уже используется");

        try {
            accountDetailsService.loadUserByUsername(account.getUsername());
        } catch (UsernameNotFoundException ignored) {
            return; // все ок, пользователь не найден
        }

        errors.rejectValue("username", "", "Это логин уже используется");
    }
}
