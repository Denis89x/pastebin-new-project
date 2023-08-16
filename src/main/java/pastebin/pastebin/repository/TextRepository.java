package pastebin.pastebin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pastebin.pastebin.model.Text;

@Repository
public interface TextRepository extends JpaRepository<Text, Integer> {

    Text findByLinkText(String linkText);
}
