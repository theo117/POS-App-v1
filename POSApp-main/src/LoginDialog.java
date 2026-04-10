import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class LoginDialog extends JDialog
{
    private final UserRepository userRepository;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private UserAccount authenticatedUser;

    public LoginDialog(Frame owner, UserRepository userRepository)
    {
        super(owner, "Login", true);
        this.userRepository = userRepository;
        buildUi();
    }

    public UserAccount authenticate()
    {
        setVisible(true);
        return authenticatedUser;
    }

    private void buildUi()
    {
        setLayout(new BorderLayout(12, 12));
        JPanel content = (JPanel) getContentPane();
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(205, 214, 226)),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        content.setBackground(new Color(251, 249, 245));

        JLabel titleLabel = new JLabel("JavaPOS Sign In");
        titleLabel.setFont(new java.awt.Font("Segoe UI Semibold", java.awt.Font.BOLD, 22));
        titleLabel.setForeground(new Color(27, 38, 58));

        JLabel subtitleLabel = new JLabel("Use your operator or admin account to open the register.");
        subtitleLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(92, 103, 118));

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setOpaque(false);
        JLabel usernameLabel = new JLabel("Username");
        JLabel passwordLabel = new JLabel("Password");
        styleFormLabel(usernameLabel);
        styleFormLabel(passwordLabel);
        styleField(usernameField);
        styleField(passwordField);
        form.add(usernameLabel);
        form.add(usernameField);
        form.add(passwordLabel);
        form.add(passwordField);
        add(header, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");
        stylePrimaryButton(loginButton);
        styleSecondaryButton(cancelButton);
        loginButton.addActionListener(evt -> tryLogin());
        cancelButton.addActionListener(evt -> dispose());
        buttons.add(loginButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);

        setSize(360, 180);
        setLocationRelativeTo(getOwner());
    }

    private void styleFormLabel(JLabel label)
    {
        label.setFont(new java.awt.Font("Segoe UI Semibold", java.awt.Font.BOLD, 13));
        label.setForeground(new Color(48, 60, 79));
        label.setHorizontalAlignment(SwingConstants.LEFT);
    }

    private void styleField(javax.swing.text.JTextComponent field)
    {
        field.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(196, 206, 221)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    private void stylePrimaryButton(JButton button)
    {
        button.setFont(new java.awt.Font("Segoe UI Semibold", java.awt.Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBackground(new Color(18, 117, 90));
        button.setForeground(Color.WHITE);
    }

    private void styleSecondaryButton(JButton button)
    {
        button.setFont(new java.awt.Font("Segoe UI Semibold", java.awt.Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBackground(new Color(234, 239, 245));
        button.setForeground(new Color(48, 60, 79));
    }

    private void tryLogin()
    {
        try
        {
            UserAccount user = userRepository.authenticate(usernameField.getText().trim(), new String(passwordField.getPassword()));
            if (user == null || !user.isActive())
            {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
                return;
            }

            if (user.mustChangePassword())
            {
                if (!promptForPasswordChange(user))
                {
                    return;
                }

                user = userRepository.authenticate(usernameField.getText().trim(), new String(passwordField.getPassword()));
                if (user == null)
                {
                    JOptionPane.showMessageDialog(this, "Unable to refresh login after password change.");
                    return;
                }
            }

            authenticatedUser = user;
            dispose();
        }
        catch (SQLException ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to log in.\n" + ex.getMessage(), "Login Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean promptForPasswordChange(UserAccount user) throws SQLException
    {
        JPasswordField newPasswordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("New Password"));
        panel.add(newPasswordField);
        panel.add(new JLabel("Confirm Password"));
        panel.add(confirmPasswordField);

        if (JOptionPane.showConfirmDialog(
            this,
            panel,
            "Change Default Password",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        ) != JOptionPane.OK_OPTION)
        {
            return false;
        }

        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        if (newPassword.trim().isEmpty() || !newPassword.equals(confirmPassword))
        {
            JOptionPane.showMessageDialog(this, "Passwords must match and cannot be empty.");
            return false;
        }

        userRepository.forcePasswordChange(user.getId(), newPassword);
        passwordField.setText(newPassword);
        JOptionPane.showMessageDialog(this, "Password updated. Continue logging in.");
        return true;
    }
}
