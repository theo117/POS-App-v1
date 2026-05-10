import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtils
{
    private static final String PBKDF2_PREFIX = "pbkdf2$";
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    private PasswordUtils()
    {
    }

    public static String hashPassword(String password)
    {
        if (password == null)
        {
            throw new IllegalArgumentException("Password is required.");
        }

        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return PBKDF2_PREFIX
            + ITERATIONS
            + "$"
            + Base64.getEncoder().encodeToString(salt)
            + "$"
            + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(String password, String storedPassword)
    {
        if (password == null || storedPassword == null)
        {
            return false;
        }

        if (storedPassword.startsWith(PBKDF2_PREFIX))
        {
            try
            {
                String[] parts = storedPassword.split("\\$");
                if (parts.length != 4)
                {
                    return false;
                }

                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
                byte[] actualHash = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length * 8);
                return MessageDigest.isEqual(expectedHash, actualHash);
            }
            catch (IllegalArgumentException ex)
            {
                return false;
            }
        }

        return password.equals(storedPassword) || legacySha256(password).equals(storedPassword);
    }

    public static boolean isLegacyHash(String storedPassword)
    {
        return storedPassword != null && !storedPassword.startsWith(PBKDF2_PREFIX);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
    {
        try
        {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException ex)
        {
            throw new IllegalStateException("PBKDF2 password hashing is not available.", ex);
        }
    }

    private static String legacySha256(String password)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hash)
            {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }
}
