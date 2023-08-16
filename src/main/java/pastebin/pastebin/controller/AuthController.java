package pastebin.pastebin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.service.CaptchaService;
import pastebin.pastebin.service.RegistrationService;
import pastebin.pastebin.util.AccountValidator;

import javax.validation.Valid;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final AccountValidator accountValidator;
    private final CaptchaService captchaService;

    @Autowired
    public AuthController(RegistrationService registrationService, AccountValidator accountValidator, CaptchaService captchaService) {
        this.registrationService = registrationService;
        this.accountValidator = accountValidator;
        this.captchaService = captchaService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "authentication/login";
    }

    @GetMapping("/registration")
    public String registrationPage(@ModelAttribute("account") Account account) {
        return "authentication/registration";
    }

    @PostMapping("/registration")
    public String performRegistration(@ModelAttribute("account") @Valid Account account, BindingResult bindingResult,
                                      @RequestParam(name = "g-recaptcha-response") String recaptchaResponse) {
        boolean isCaptchaValid = captchaService.isCaptchaValid(recaptchaResponse);

        if (!isCaptchaValid) {
            bindingResult.reject("captchaError", "Проверка reCaptcha не прошла. Пожалуйста, попробуйте еще раз.");
        }

        accountValidator.validate(account, bindingResult);

        if (bindingResult.hasErrors()) {
            return "authentication/registration";
        }

        registrationService.register(account);

        return "redirect:/auth/login";
    }




}
