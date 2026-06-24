import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepository
{
    private static final int MIN_PASSWORD_LENGTH = 6;
    private final DatabaseManager databaseManager;

    public UserRepository(DatabaseManager databaseManager)
    {
        this.databaseManager = databaseManager;
    }

    public void seedDefaultsIfEmpty() throws SQLException
    {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet resultSet = statement.executeQuery())
        {
            if (resultSet.next() && resultSet.getInt(1) > 0)
            {
                return;
            }
        }

        createUser("admin", "admin123", "ADMIN", true);
        createUser("cashier", "cashier123", "CASHIER", true);
    }

    public UserAccount authenticate(String username, String password) throws SQLException
    {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, username, password, role, active, must_change_password FROM users WHERE username = ?"
             ))
        {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery())
            {
                if (resultSet.next())
                {
                    String storedPassword = resultSet.getString("password");
                    if (!PasswordUtils.verifyPassword(password, storedPassword))
                    {
                        return null;
                    }

                    if (PasswordUtils.isLegacyHash(storedPassword))
                    {
                        upgradePasswordHash(resultSet.getInt("id"), PasswordUtils.hashPassword(password));
                    }

                    return new UserAccount(
                        resultSet.getInt("id"),
                        resultSet.getString("username"),
                        resultSet.getString("role"),
                        resultSet.getInt("active") == 1,
                        resultSet.getInt("must_change_password") == 1
                    );
                }
            }
        }

        return null;
    }

    public List<UserAccount> findAll() throws SQLException
    {
        List<UserAccount> users = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, username, role, active, must_change_password FROM users ORDER BY username ASC"
             );
             ResultSet resultSet = statement.executeQuery())
        {
            while (resultSet.next())
            {
                users.add(new UserAccount(
                    resultSet.getInt("id"),
                    resultSet.getString("username"),
                    resultSet.getString("role"),
                    resultSet.getInt("active") == 1,
                    resultSet.getInt("must_change_password") == 1
                ));
            }
        }
        return users;
    }

    public void createUser(String username, String password, String role) throws SQLException
    {
        createUser(username, password, role, false);
    }

    public void createUser(String username, String password, String role, boolean mustChangePassword) throws SQLException
    {
        validateUserInput(username, password, role, true);
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO users(username, password, role, active, must_change_password) VALUES (?, ?, ?, 1, ?)"
             ))
        {
            statement.setString(1, username.trim());
            statement.setString(2, PasswordUtils.hashPassword(password));
            statement.setString(3, role.trim().toUpperCase());
            statement.setInt(4, mustChangePassword ? 1 : 0);
            statement.executeUpdate();
        }
    }

    public void updateUser(int id, String username, String password, String role, boolean active) throws SQLException
    {
        validateUserInput(username, password, role, false);
        String sql = password == null || password.trim().isEmpty()
            ? "UPDATE users SET username = ?, role = ?, active = ? WHERE id = ?"
            : "UPDATE users SET username = ?, password = ?, role = ?, active = ?, must_change_password = ? WHERE id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, username.trim());
            if (password == null || password.trim().isEmpty())
            {
                statement.setString(2, role.trim().toUpperCase());
                statement.setInt(3, active ? 1 : 0);
                statement.setInt(4, id);
            }
            else
            {
                statement.setString(2, PasswordUtils.hashPassword(password));
                statement.setString(3, role.trim().toUpperCase());
                statement.setInt(4, active ? 1 : 0);
                statement.setInt(5, 0);
                statement.setInt(6, id);
            }
            statement.executeUpdate();
        }
    }

    public void forcePasswordChange(int userId, String newPassword) throws SQLException
    {
        validatePassword(newPassword);
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE users SET password = ?, must_change_password = 0 WHERE id = ?"
             ))
        {
            statement.setString(1, PasswordUtils.hashPassword(newPassword));
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private void validateUserInput(String username, String password, String role, boolean passwordRequired) throws SQLException
    {
        if (username == null || username.trim().isEmpty())
        {
            throw new SQLException("Username is required.");
        }

        if (!"ADMIN".equalsIgnoreCase(role) && !"CASHIER".equalsIgnoreCase(role))
        {
            throw new SQLException("Role must be ADMIN or CASHIER.");
        }

        if (passwordRequired || (password != null && !password.trim().isEmpty()))
        {
            validatePassword(password);
        }
    }

    private void validatePassword(String password) throws SQLException
    {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH)
        {
            throw new SQLException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
    }

    private void upgradePasswordHash(int userId, String hashedPassword) throws SQLException
    {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE users SET password = ? WHERE id = ?"
             ))
        {
            statement.setString(1, hashedPassword);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }
}
