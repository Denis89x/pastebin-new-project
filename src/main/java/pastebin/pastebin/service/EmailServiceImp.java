package pastebin.pastebin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class EmailServiceImp implements EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailServiceImp(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationCode(String email, String verificationCode) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setFrom("pastebinp@mail.ru");
            helper.setSubject("Verification Code for Your Account");
            helper.setText("Your verification code is: " + verificationCode);
            System.out.println("Письмо отправлено №1");

            mailSender.send(message);
        } catch (MessagingException e) {
            // Обработайте исключение, если письмо не удалось отправить
            e.printStackTrace();
        }
    }

    public String getEmailMasked(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex > 1) {
            String username = email.substring(0, atIndex);
            int length = username.length();
            String masked = username.charAt(0) + "*".repeat(length - 2) + username.charAt(length - 1);
            return masked + email.substring(atIndex);
        }
        return email;
    }
}
