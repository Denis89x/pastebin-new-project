package pastebin.pastebin.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.model.Message;
import pastebin.pastebin.model.Text;
import pastebin.pastebin.repository.AccountRepository;
import pastebin.pastebin.repository.MessageRepository;
import pastebin.pastebin.repository.TextRepository;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WriteLinkService implements IWriteLinkService {

    private final MinioClient minioClient;
    private final TextRepository textRepository;
    private final AccountRepository accountRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public WriteLinkService(MinioClient minioClient, TextRepository textRepository, AccountRepository accountRepository, MessageRepository messageRepository) {
        this.minioClient = minioClient;
        this.textRepository = textRepository;
        this.accountRepository = accountRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public String writeLink(String link, String type) {
        String messageFileName = type + UUID.randomUUID().toString();
        InputStream messageInputStream = new ByteArrayInputStream(link.getBytes());

        PutObjectArgs messageArgs = null;
        try {
            messageArgs = PutObjectArgs.builder()
                    .bucket("storage")
                    .object(messageFileName)
                    .stream(messageInputStream, messageInputStream.available(), -1)
                    .contentType("text/plain")
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            minioClient.putObject(messageArgs);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new RuntimeException(e);
        }

        String messageLink;
        try {
            messageLink = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket("storage")
                            .object(messageFileName)
                            .method(Method.GET)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
                 ServerException e) {
            throw new RuntimeException(e);
        }

        return messageLink;
    }

    @Override
    public void checkAndUpdateLinkIfNeeded(Text text) {
        if (isLinkExpired(text.getLinkText())) {
            try {
                String newLinkText = updateTextUrl(text.getLinkText());
                text.setLinkText(newLinkText);
                textRepository.save(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void checkAndUpdateLinkIfNeeded(Message message) {
        if (isLinkExpired(message.getContent())) {
            try {
                String newLinkText = updateMessageUrl(message.getContent());
                message.setContent(newLinkText);
                messageRepository.save(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String updateTextUrl(String oldLinkText) {

        String newLinkText = updateUrl(oldLinkText);

        // Обновите ссылку в объекте Text
        Text textEntity = textRepository.findByLinkText(oldLinkText);
        textEntity.setLinkText(newLinkText);

        // Сохраните обновленную сущность Text в базе данных
        textRepository.save(textEntity);

        return newLinkText;
    }

    public String updateMessageUrl(String oldLinkText) {

        String newLinkText = updateUrl(oldLinkText);

        Message messageEntity = messageRepository.findByContent(oldLinkText);
        messageEntity.setContent(newLinkText);

        messageRepository.save(messageEntity);

        return newLinkText;
    }

    public String updateUrl(String oldLinkText) {
        URL oldURL = null;
        try {
            oldURL = new URL(oldLinkText);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String objectName = oldURL.getPath().substring(8); // Удаляем первый символ ("/") из пути

        // Создайте новую пресайнед URL с обновленным сроком действия
        String newLinkText = null;
        try {
            newLinkText = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket("storage")
                            .object(objectName)
                            .expiry(604800)
                            .method(Method.GET) // Указываем метод HTTP GET
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
                 ServerException e) {
            throw new RuntimeException(e);
        }
        return newLinkText;
    }

    public void GetAccount(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<Account> account = accountRepository.findByUsername(username);

        model.addAttribute("account", account.orElse(null));
    }

    public boolean isLinkExpired(String linkText) {
        try {
            URL url = new URL(linkText);
            String objectName = url.getPath().substring(9);

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket("storage")
                            .object(objectName)
                            .build()
            );

            ZonedDateTime lastModified = stat.lastModified();
            ZonedDateTime currentTime = ZonedDateTime.now();

            return currentTime.minusDays(7).isAfter(lastModified);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getTextFromLink(String linkText) {
        try {
            URL url = new URL(linkText);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream inputStream = connection.getInputStream();

            // Читаем текст из InputStream
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to retrieve text from link.";
        }
    }

    public void show(Optional<Account> account, Model model) {
        account.ifPresent(a -> {
            List<Text> texts = a.getTexts();
            List<String> textFromLinks = new ArrayList<>();
            for (Text text : texts) {
                checkAndUpdateLinkIfNeeded(text);
                String textFromLink = getTextFromLink(text.getLinkText());
                textFromLinks.add(textFromLink);
            }
            model.addAttribute("texts", texts);
            model.addAttribute("textFromLinks", textFromLinks);
        });
        GetAccount(model);
    }

    public String updateImageLink(String oldLinkText) throws Exception {
        // Получите имя объекта из старой ссылки
        URL oldURL = new URL(oldLinkText);
        String objectName = oldURL.getPath().substring(8); // Удаляем первый символ ("/") из пути

        // Создайте новую пресайнед URL с обновленным сроком действия
        String newLinkText = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket("storage")
                        .object(objectName)
                        .expiry(604800)
                        .method(Method.GET) // Указываем метод HTTP GET
                        .build()
        );

        // Обновите ссылку в объекте Account
        Account account = getAuthenticatedAccount();
        account.setProfilePicture(newLinkText);
        accountRepository.save(account);

        return newLinkText;
    }

    public Account getAuthenticatedAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return accountRepository.findByUsername(username).orElse(null);
    }


}
