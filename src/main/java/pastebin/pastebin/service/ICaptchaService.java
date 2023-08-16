package pastebin.pastebin.service;

public interface ICaptchaService {
    boolean isCaptchaValid(String recaptchaResponse);
}
