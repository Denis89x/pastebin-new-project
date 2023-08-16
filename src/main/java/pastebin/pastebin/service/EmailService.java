package pastebin.pastebin.service;

public interface EmailService {
    void sendVerificationCode(String email, String verificationCode);
}
