package pastebin.pastebin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.model.Message;
import pastebin.pastebin.service.MessageService;
import pastebin.pastebin.service.WriteLinkService;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;
    private final WriteLinkService writeLinkService;

    @Autowired
    public MessageController(MessageService messageService, WriteLinkService writeLinkService) {
        this.messageService = messageService;
        this.writeLinkService = writeLinkService;
    }

    @GetMapping("/messages")
    public String showMessages(Model model) {
        writeLinkService.GetAccount(model);

        String currentUser = messageService.getCurrentUserUsername();
        List<String> otherUsers = messageService.getOtherUsernames();
        List<Account> dialogUsernames = messageService.getDialogUsernames(currentUser);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("dialogUsernames", dialogUsernames);

        return "messenger/messages";
    }

    @GetMapping("/messages/{receiverUsername}")
    public String showMessageWithReceiver(@PathVariable String receiverUsername, Model model) {
        String currentUser = messageService.getCurrentUserUsername();

        List<Message> messages = messageService.getConversationMessages(currentUser, receiverUsername);

        writeLinkService.GetAccount(model);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("receiver", receiverUsername);
        model.addAttribute("messages", messages);

        return "messenger/chat";
    }

    @PostMapping("/messages/{receiverUsername}")
    public String sendMessageToReceiver(@PathVariable String receiverUsername,
                                        @RequestParam("messageContent") String messageContent) {
        String senderUsername = messageService.getCurrentUserUsername();;

        messageService.sendMessage(senderUsername, receiverUsername, messageContent);

        return "redirect:/messages/messages/{receiverUsername}";
    }

    @PostMapping("/messages/start-dialog")
    public String startNewDialog(@RequestParam("receiverUsername") String receiverUsername) {
        String senderUsername = messageService.getCurrentUserUsername();

        Optional<Account> receiverAccount = messageService.findAccountByUsername(receiverUsername);
        if (receiverAccount.isEmpty()) {
            System.err.println("получателя нет");
            // Handle case when receiver account doesn't exist
            return "redirect:/messages/messages";
        }

        messageService.sendMessage(senderUsername, receiverUsername, "New dialog started");

        return "redirect:/messages/messages/" + receiverUsername;
    }

}

