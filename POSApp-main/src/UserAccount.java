public class UserAccount
{
    private final int id;
    private final String username;
    private final String role;
    private final boolean active;
    private final boolean mustChangePassword;

    public UserAccount(int id, String username, String role, boolean active, boolean mustChangePassword)
    {
        this.id = id;
        this.username = username;
        this.role = role;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
    }

    public int getId()
    {
        return id;
    }

    public String getUsername()
    {
        return username;
    }

    public String getRole()
    {
        return role;
    }

    public boolean isActive()
    {
        return active;
    }

    public boolean isAdmin()
    {
        return "ADMIN".equals(role);
    }

    public boolean mustChangePassword()
    {
        return mustChangePassword;
    }
}
