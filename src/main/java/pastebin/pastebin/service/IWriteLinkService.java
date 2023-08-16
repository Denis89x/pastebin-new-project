package pastebin.pastebin.service;

import pastebin.pastebin.model.Text;

public interface IWriteLinkService {
    String writeLink(String link, String type);

    void checkAndUpdateLinkIfNeeded(Text text);
}
