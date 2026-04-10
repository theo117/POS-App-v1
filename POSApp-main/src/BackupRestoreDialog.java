import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class BackupRestoreDialog extends JDialog
{
    private final DatabaseManager databaseManager;
    private final Runnable afterRestore;
    private final JTextArea infoArea = new JTextArea();

    public BackupRestoreDialog(Frame owner, DatabaseManager databaseManager, Runnable afterRestore)
    {
        super(owner, "Backup And Restore", true);
        this.databaseManager = databaseManager;
        this.afterRestore = afterRestore;
        buildUi();
    }

    private void buildUi()
    {
        UiStyler.styleDialog(this, "Backup And Restore", "Protect your local data before major changes, imports, or end-of-day operations.");
        add(UiStyler.createHeader("Backup And Restore", "Protect your local data before major changes, imports, or end-of-day operations."), BorderLayout.NORTH);
        UiStyler.styleTextArea(infoArea, true);
        infoArea.setText("Database file:\n" + databaseManager.getDatabasePath());

        JButton backupButton = new JButton("Backup");
        JButton restoreButton = new JButton("Restore");
        UiStyler.stylePrimaryButton(backupButton);
        UiStyler.styleDangerButton(restoreButton);
        backupButton.addActionListener(evt -> backup());
        restoreButton.addActionListener(evt -> restore());

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(backupButton);
        actions.add(restoreButton);

        add(UiStyler.createSectionPanel("Current Database", infoArea), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        setSize(520, 220);
        setLocationRelativeTo(getOwner());
    }

    private void backup()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(databaseManager.getDatabasePath().getFileName().toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        try
        {
            Files.copy(databaseManager.getDatabasePath(), chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(this, "Backup completed.");
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, "Backup failed.\n" + ex.getMessage(), "Backup Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restore()
    {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        if (JOptionPane.showConfirmDialog(this, "Restore this database backup? This replaces the current local database.", "Confirm Restore", JOptionPane.YES_NO_OPTION)
            != JOptionPane.YES_OPTION)
        {
            return;
        }

        try
        {
            Files.copy(chooser.getSelectedFile().toPath(), databaseManager.getDatabasePath(), StandardCopyOption.REPLACE_EXISTING);
            afterRestore.run();
            JOptionPane.showMessageDialog(this, "Restore completed. Reopen any open admin screens if needed.");
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, "Restore failed.\n" + ex.getMessage(), "Restore Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
