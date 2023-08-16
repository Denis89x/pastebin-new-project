package pastebin.pastebin.controller;

import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.model.Text;
import pastebin.pastebin.repository.AccountRepository;
import pastebin.pastebin.repository.TextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pastebin.pastebin.service.WriteLinkService;

import javax.validation.Valid;
import java.net.URL;
import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/send")
public class TextController {

    private final TextRepository textRepository;
    private final AccountRepository accountRepository;
    private final MinioClient minioClient;
    private final WriteLinkService writeLinkService;
    private static final Logger logger = LoggerFactory.getLogger(TextController.class);

    @Autowired
    public TextController(TextRepository textRepository, AccountRepository accountRepository, MinioClient minioClient, WriteLinkService writeLinkService) {
        this.textRepository = textRepository;
        this.accountRepository = accountRepository;
        this.minioClient = minioClient;
        this.writeLinkService = writeLinkService;
    }

    @ModelAttribute("account")
    public Optional<Account> getAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return accountRepository.findByUsername(username);
    }

    @GetMapping("/text/list")
    public String showTextList(Model model) {
        List<Text> texts = textRepository.findAll();
        writeLinkService.GetAccount(model);
        model.addAttribute("texts", texts);

        return "main/list";
    }

    @GetMapping("/text/own")
    public String showOwnTexts(Model model) {
        Optional<Account> account = getAccount();
        writeLinkService.show(account, model);
        writeLinkService.GetAccount(model);
        return "main/own";
    }

    @GetMapping("/text/{id}")
    public String getText(@PathVariable("id") Integer id, Model model) {
        Optional<Text> text = textRepository.findById(id);

        if (text.isPresent()) {
            Text t = text.get();
            writeLinkService.checkAndUpdateLinkIfNeeded(t);
            model.addAttribute("text", t);
            model.addAttribute("textFromLink", writeLinkService.getTextFromLink(t.getLinkText()));
        }
        writeLinkService.GetAccount(model);
        return "main/view";
    }

    @GetMapping("/text/add")
    public String showAddTextForm(Model model) {
        writeLinkService.GetAccount(model);

        return "main/text";
    }

    @PostMapping("/text/add")
    public String addText(@ModelAttribute("account") @Valid Account account, BindingResult bindingResult,
                          @RequestParam("text") String text, Model model) {
        if (bindingResult.hasErrors()) {
            return "error"; // Обработка ошибок валидации
        }

        if (text.length() <= 2) {
            model.addAttribute("error", "Text should have at least 2 characters");
            return "main/text";
        }

        String linkText = writeLinkService.writeLink(text, "topic");

        LocalDateTime localTime = LocalDateTime.now();

        localTime = localTime.withNano(0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = localTime.format(formatter);

        Text textEntity = new Text();
        textEntity.setLinkText(linkText);
        textEntity.setOwner(account);
        textEntity.setTime(LocalDateTime.parse(formattedTime, formatter));

        textRepository.save(textEntity);

        model.addAttribute("text", textEntity);

        return "redirect:/send/text/" + textEntity.getTextId();
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        e.printStackTrace();
        System.out.println("Error occurred: " + e.getMessage());
        return "error";
    }

    @GetMapping("/ranking")
    public String showRanking(Model model) {
        List<Account> accounts = accountRepository.findAllByOrderByTextsSizeDesc();
        model.addAttribute("accounts", accounts);

        writeLinkService.GetAccount(model);
        return "main/ranking";
    }

    @GetMapping("/text/user/{username}")
    public String showUserTexts(@PathVariable("username") String username, Model model) {
        Optional<Account> account = accountRepository.findByUsername(username);
        writeLinkService.show(account, model);
        return "main/user-texts";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id) {
        logger.info("Deleting text with ID: {}", id);

        Optional<Text> textOptional = textRepository.findById(id);
        if (textOptional.isPresent()) {
            Text text = textOptional.get();

            // Удаление файла с MinIO
            try {
                // Получите ссылку на файл
                String linkText = text.getLinkText();
                System.out.println("The link of text = " + linkText);

                // Извлеките путь к объекту из ссылки
                URL url = new URL(linkText);
                String path = url.getPath();

                // Удалите файл с MinIO
                RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                        .bucket("storage")
                        .object(path.substring(1)) // Удаляем первый символ ("/") из пути
                        .build();
                minioClient.removeObject(removeObjectArgs);

                logger.info("File deleted from MinIO: {}", linkText);
            } catch (Exception e) {
                logger.error("Error deleting file from MinIO: {}", e.getMessage());
                e.printStackTrace();
                return "error";
            }
            try {
                // Получите ссылку на файл
                String linkText = text.getLinkText();

                // Извлеките путь к объекту из ссылки
                URL url = new URL(linkText);
                String path = url.getPath();

                // Удалите файл с MinIO
                RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                        .bucket("storage")
                        .object(path.substring(1)) // Удаляем первый символ ("/") из пути
                        .build();
                minioClient.removeObject(removeObjectArgs);
            } catch (Exception e) {
                e.printStackTrace();
                return "error";
            }

            // Удаление записи из БД
            textRepository.delete(text);
            logger.info("Text record deleted from the database");
        }

        return "redirect:/send/text/list";
    }
}