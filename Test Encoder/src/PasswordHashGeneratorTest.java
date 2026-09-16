import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGeneratorTest {
    @Test
    void generateHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("=== Хеши для пользователей ===");
        System.out.println("user1/pass123: " + encoder.encode("pass123"));
        System.out.println("user2/pass456: " + encoder.encode("pass456"));
    }
}
