package co.edu.eci.blueprints.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class InMemoryUserService {

    private record UserRecord(String passwordHash, String scopes) { }

    private final Map<String, UserRecord> users;
    private final PasswordEncoder encoder;

    public InMemoryUserService(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.users = Map.of(
            // "student" solo puede leer blueprints (SCOPE_blueprints.read)
            "student", new UserRecord(encoder.encode("student123"), "blueprints.read"),
            // "assistant" puede leer y crear/modificar blueprints
            "assistant", new UserRecord(encoder.encode("assistant123"), "blueprints.read blueprints.write")
        );
    }

    public boolean isValid(String username, String rawPassword) {
        UserRecord user = users.get(username);
        return user != null && encoder.matches(rawPassword, user.passwordHash());
    }

    public String scopesOf(String username) {
        UserRecord user = users.get(username);
        return user != null ? user.scopes() : "";
    }
}
