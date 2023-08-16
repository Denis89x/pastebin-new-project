package pastebin.pastebin.controller;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pastebin.pastebin.dto.VerificationCodeDTO;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.repository.AccountRepository;
import pastebin.pastebin.service.EmailServiceImp;
import pastebin.pastebin.service.WriteLinkService;

import java.util.*;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceImp emailService;
    private final MinioClient minioClient;
    private final WriteLinkService writeLinkService;
    private final Map<Integer, VerificationCodeDTO> verificationCodesMap = new HashMap<>();

    @Autowired
    public ProfileController(AccountRepository accountRepository, PasswordEncoder passwordEncoder, EmailServiceImp emailService, MinioClient minioClient, WriteLinkService writeLinkService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.minioClient = minioClient;
        this.writeLinkService = writeLinkService;
    }

    public void GetAccount(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<Account> account = accountRepository.findByUsername(username);

        model.addAttribute("account", account.orElse(null));
    }

    @GetMapping()
    public String showSettings(Model model) {
        Account account = writeLinkService.getAuthenticatedAccount();
        if (account != null && account.getProfilePicture() != null && writeLinkService.isLinkExpired(account.getProfilePicture())) {
            try {
                String newLinkText = writeLinkService.updateImageLink(account.getProfilePicture());
                account.setProfilePicture(newLinkText);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        GetAccount(model);

        String email = account.getEmail();
        String maskedEmail = emailService.getEmailMasked(email);
        model.addAttribute("maskedEmail", maskedEmail);

        return "user/user";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<Account> accountOptional = accountRepository.findByUsername(username);

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();
            if (passwordEncoder.matches(currentPassword, account.getPassword())) {
                if (!isValidPassword(newPassword)) {
                    redirectAttributes.addFlashAttribute("passwordError", "Неверный формат нового пароля.");
                    return "redirect:/profile";
                }
                account.setPassword(passwordEncoder.encode(newPassword));
                accountRepository.save(account);

                redirectAttributes.addFlashAttribute("successMessage", "Пароль успешно обновлен.");
            } else {
                redirectAttributes.addFlashAttribute("passwordError", "Неверный текущий пароль");
            }
        } else {
            redirectAttributes.addFlashAttribute("passwordError", "Пользователь не найден");
        }
        return "redirect:/profile";
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 4 || password.length() > 15) {
            return false;
        }

        if (!password.matches(".*[a-zA-Z].*")) {
            return false;
        }

        if (!password.matches(".*\\d.*")) {
            return false;
        }

        return password.matches(".*[!@#$%^&*()].*");
    }


    @PostMapping("/change-username")
    public String changeUsername(@RequestParam String newUsername, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<Account> accountOptional = accountRepository.findByUsername(username);

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();
            // Валидируйте новый логин напрямую, без использования DTO и Validator
            if (!newUsername.matches("(?=.*[a-zA-Z])[a-zA-Z0-9_]+")) {
                System.out.println("Не прошёл по требованиям");
                model.addAttribute("usernameError", "Логин должен содержать только латинские буквы, цифры и символ '_'");
                GetAccount(model);
                return "user/user";
            }
            if (accountRepository.findByUsername(newUsername).isPresent()) {

                model.addAttribute("usernameError", "Этот логин уже используется");
                GetAccount(model);
                return "user/user";
            }
            account.setUsername(newUsername);

            Authentication updatedAuthentication = new UsernamePasswordAuthenticationToken(
                    account.getUsername(), authentication.getCredentials(), authentication.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(updatedAuthentication);

            try {
                accountRepository.save(account);
            } catch (Exception e) {
                model.addAttribute("usernameError", "Ошибка при изменении логина. Попробуйте еще раз.");
                GetAccount(model);
                return "user/user";
            }

            return "redirect:/profile";
        }
        model.addAttribute("usernameError", "Пользователь не найден");
        GetAccount(model);
        return "user/user";
    }

    @PostMapping("/change-email")
    public String changeEmail(@RequestParam String newEmail, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<Account> accountOptional = accountRepository.findByUsername(username);

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();

            if (accountRepository.findByEmail(newEmail).isPresent() && newEmail.equals(account.getEmail())) {
                model.addAttribute("emailError", "This is your e-mail.");
                GetAccount(model);
                return "user/user";
            }

            if (accountRepository.findByEmail(newEmail).isPresent()) {
                model.addAttribute("emailError", "This e-mail is already taken");
                GetAccount(model);
                return "user/user";
            }

            account.setEmail(newEmail);

            try {
                accountRepository.save(account);
            } catch (Exception e) {
                model.addAttribute("emailError", "Error when changing mail. Try again.");
                GetAccount(model);
                return "user/user";
            }

            return "redirect:/profile";
        }
        model.addAttribute("emailError", "E-mail not found");
        GetAccount(model);
        return "user/user";
    }

    @PostMapping("/send-verification-code")
    public String sendVerificationCode(RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<Account> accountOptional = accountRepository.findByUsername(username);

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();

            String verificationCode = generateVerificationCode();

            VerificationCodeDTO verificationCodeDTO = new VerificationCodeDTO();
            verificationCodeDTO.setUserId(account.getIdAccount());
            verificationCodeDTO.setVerificationCode(verificationCode);

            verificationCodesMap.put(account.getIdAccount(), verificationCodeDTO);

            emailService.sendVerificationCode(account.getEmail(), verificationCode);

            redirectAttributes.addFlashAttribute("successSend", "Verification code has been sent to your email.");
        } else {
            redirectAttributes.addFlashAttribute("emailError", "User not found");
        }

        return "redirect:/profile";
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    @PostMapping("/verify-email")
    public String verifyEmail(@RequestParam String verificationCode, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<Account> accountOptional = accountRepository.findByUsername(username);

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();

            VerificationCodeDTO verificationCodeDTO = verificationCodesMap.get(account.getIdAccount());

            if (verificationCodeDTO != null) {

                if (verificationCode.equals(verificationCodeDTO.getVerificationCode())) {

                    account.setIsVerifiedEmail(true);
                    accountRepository.save(account);

                    redirectAttributes.addFlashAttribute("successVerify", "Your email has been verified successfully.");

                    verificationCodesMap.remove(account.getIdAccount());
                } else {
                    redirectAttributes.addFlashAttribute("InvalidCode", "Invalid verification code");
                }
            } else {
                redirectAttributes.addFlashAttribute("verificationError", "Verification code not found");
            }
        } else {
            redirectAttributes.addFlashAttribute("emailError", "User not found");
        }

        return "redirect:/profile";
    }

    @PostMapping("/unlink-email")
    public String unlinkEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<Account> accountOptional = accountRepository.findByUsername(username);

        if (accountOptional.isPresent()) {
            Account account = accountOptional.get();

            account.setIsVerifiedEmail(false);

            accountRepository.save(account);
        }

        return "redirect:/profile";
    }

    @PostMapping("/upload-profile-picture")
    public String uploadProfilePicture(@RequestParam("file") MultipartFile file, Model model) {
        GetAccount(model);
        if (file.isEmpty()) {
            // Обработка ошибки, если файл не выбран
            model.addAttribute("uploadError", "Please select a file to upload.");
            GetAccount(model);
            return "user/user";
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        try {
            String fileName = UUID.randomUUID().toString() + extension;

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket("storage")
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType("image/jpeg")
                    .build();

            minioClient.putObject(args);

            String linkPicture = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket("storage")
                            .object(fileName)
                            .method(Method.GET) // Указываем метод HTTP GET
                            .build()
            );

            Account account = writeLinkService.getAuthenticatedAccount();

            if (!(account == null)) {
                account.setProfilePicture(linkPicture);
                accountRepository.save(account);
            }

            return "redirect:/profile";
        } catch (Exception e) {
            e.printStackTrace();
            // Обработка ошибки при загрузке файла в MinIO
            model.addAttribute("uploadError", "An error occurred while uploading the file. Please try again.");
            GetAccount(model);
            return "user/user";
        }
    }

}