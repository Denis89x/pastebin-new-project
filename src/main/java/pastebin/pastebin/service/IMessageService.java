package pastebin.pastebin.service;

import org.springframework.ui.Model;
import pastebin.pastebin.model.Account;

public interface IMessageService {
    void sendMessage(Account sender, String receiverUsername, String messageContent) throws Exception;
}
