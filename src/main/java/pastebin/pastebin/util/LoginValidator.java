package pastebin.pastebin.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import pastebin.pastebin.dto.NewUserDTO;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.service.AccountDetailsService;

@Component
public class LoginValidator implements Validator {

    private final AccountDetailsService accountDetailsService;

    @Autowired
    public LoginValidator(AccountDetailsService accountDetailsService) {
        this.accountDetailsService = accountDetailsService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return NewUserDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        NewUserDTO account = (NewUserDTO) target;

        if (!account.getNewUsername().matches("(?=.*[a-zA-Z])[a-zA-Z0-9_]+")) {
            errors.rejectValue("newUsername", "", "Логин должен содержать только латинские буквы, цифры и символ '_'");
        }

        try {
            accountDetailsService.loadUserByUsername(account.getNewUsername());
        } catch (UsernameNotFoundException ignored) {
            return; // все ок, пользователь не найден
        }

        errors.rejectValue("newUsername", "", "Это логин уже используется");
    }
}
