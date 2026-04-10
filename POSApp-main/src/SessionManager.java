import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.time.Duration;
import java.time.Instant;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.GridLayout;

public class SessionManager
{
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(15);
    private final UserAccount currentUser;
    private final UserRepository userRepository;
    private Instant lastActivityAt = Instant.now();

    public SessionManager(UserAccount currentUser, UserRepository userRepository)
    {
        this.currentUser = currentUser;
        this.userRepository = userRepository;
    }

    public void startTracking()
    {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener()
        {
            @Override
            public void eventDispatched(AWTEvent event)
            {
                touch();
            }
        }, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    public void touch()
    {
        lastActivityAt = Instant.now();
    }

    public boolean requireActiveSession(Component parent, String actionDescription)
    {
        if (Duration.between(lastActivityAt, Instant.now()).compareTo(SESSION_TIMEOUT) < 0)
        {
            return true;
        }

        JOptionPane.showMessageDialog(parent, "Your session timed out. Re-enter your password to " + actionDescription + ".");
        return promptForPassword(parent, actionDescription);
    }

    public boolean requireSensitiveReauthentication(Component parent, String actionDescription)
    {
        return promptForPassword(parent, actionDescription);
    }

    private boolean promptForPassword(Component parent, String actionDescription)
    {
        JPasswordField passwordField = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Re-enter password to " + actionDescription + ":"));
        panel.add(passwordField);

        if (JOptionPane.showConfirmDialog(parent, panel, "Re-authentication Required", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
            != JOptionPane.OK_OPTION)
        {
            return false;
        }

        try
        {
            UserAccount user = userRepository.authenticate(currentUser.getUsername(), new String(passwordField.getPassword()));
            if (user == null || user.getId() != currentUser.getId() || !user.isActive())
            {
                JOptionPane.showMessageDialog(parent, "Password verification failed.");
                return false;
            }

            touch();
            return true;
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(parent, "Unable to verify session.\n" + ex.getMessage(), "Session Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
