import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebSessionManager
{
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);
    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();

    public String createSession(UserAccount user)
    {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionRecord(user, Instant.now()));
        return token;
    }

    public UserAccount getUser(String token)
    {
        if (token == null || token.trim().isEmpty())
        {
            return null;
        }

        SessionRecord record = sessions.get(token);
        if (record == null)
        {
            return null;
        }

        if (Duration.between(record.lastActivityAt, Instant.now()).compareTo(SESSION_TIMEOUT) > 0)
        {
            sessions.remove(token);
            return null;
        }

        record.lastActivityAt = Instant.now();
        return record.user;
    }

    public void removeSession(String token)
    {
        if (token != null)
        {
            sessions.remove(token);
        }
    }

    private static final class SessionRecord
    {
        private final UserAccount user;
        private Instant lastActivityAt;

        private SessionRecord(UserAccount user, Instant lastActivityAt)
        {
            this.user = user;
            this.lastActivityAt = lastActivityAt;
        }
    }
}
