import java.awt.BorderLayout;
import java.awt.Frame;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class UserManagementDialog extends JDialog
{
    private final UserRepository userRepository;
    private final JTable usersTable = new JTable();

    public UserManagementDialog(Frame owner, UserRepository userRepository)
    {
        super(owner, "Manage Users", true);
        this.userRepository = userRepository;
        buildUi();
        loadUsers();
    }

    private void buildUi()
    {
        UiStyler.styleDialog(this, "Manage Users", "Create staff accounts, change roles, and control access to the register.");
        add(UiStyler.createHeader("Manage Users", "Create staff accounts, change roles, and control access to the register."), BorderLayout.NORTH);

        usersTable.setModel(new DefaultTableModel(new Object[]{"ID", "Username", "Role", "Active"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        });
        UiStyler.styleTable(usersTable);

        JButton addButton = new JButton("Add User");
        JButton editButton = new JButton("Edit User");
        JButton refreshButton = new JButton("Refresh");
        UiStyler.stylePrimaryButton(addButton);
        UiStyler.styleSecondaryButton(editButton);
        UiStyler.styleSecondaryButton(refreshButton);
        addButton.addActionListener(evt -> addUser());
        editButton.addActionListener(evt -> editUser());
        refreshButton.addActionListener(evt -> loadUsers());

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(addButton);
        actions.add(editButton);
        actions.add(refreshButton);

        add(UiStyler.createSectionPanel("User Accounts", UiStyler.wrapTable(usersTable)), BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        setSize(620, 420);
        setLocationRelativeTo(getOwner());
    }

    private void loadUsers()
    {
        try
        {
            DefaultTableModel model = (DefaultTableModel) usersTable.getModel();
            model.setRowCount(0);
            for (UserAccount user : userRepository.findAll())
            {
                model.addRow(new Object[]{user.getId(), user.getUsername(), user.getRole(), user.isActive() ? "Yes" : "No"});
            }
        }
        catch (SQLException ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to load users.\n" + ex.getMessage(), "User Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addUser()
    {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JComboBox<String> roleField = new JComboBox<>(new String[]{"ADMIN", "CASHIER"});
        UiStyler.styleTextComponent(usernameField, false);
        UiStyler.styleTextComponent(passwordField, false);
        UiStyler.styleTextComponent(confirmPasswordField, false);
        UiStyler.styleComboBox(roleField);

        JPanel form = UiStyler.createLabeledFormPanel(
            "Username", usernameField,
            "Password", passwordField,
            "Confirm Password", confirmPasswordField,
            "Role", roleField
        );

        if (JOptionPane.showConfirmDialog(this, form, "Add User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
        {
            return;
        }

        try
        {
            String password = getPassword(passwordField);
            if (!password.equals(getPassword(confirmPasswordField)))
            {
                JOptionPane.showMessageDialog(this, "Passwords must match.", "User Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            userRepository.createUser(usernameField.getText().trim(), password, roleField.getSelectedItem().toString());
            loadUsers();
        }
        catch (SQLException ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to create user.\n" + ex.getMessage(), "User Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editUser()
    {
        int row = usersTable.getSelectedRow();
        if (row < 0)
        {
            JOptionPane.showMessageDialog(this, "Select a user first.");
            return;
        }

        int id = Integer.parseInt(usersTable.getValueAt(row, 0).toString());
        JTextField usernameField = new JTextField(usersTable.getValueAt(row, 1).toString());
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JComboBox<String> roleField = new JComboBox<>(new String[]{"ADMIN", "CASHIER"});
        roleField.setSelectedItem(usersTable.getValueAt(row, 2).toString());
        JComboBox<String> activeField = new JComboBox<>(new String[]{"Yes", "No"});
        activeField.setSelectedItem(usersTable.getValueAt(row, 3).toString());
        UiStyler.styleTextComponent(usernameField, false);
        UiStyler.styleTextComponent(passwordField, false);
        UiStyler.styleTextComponent(confirmPasswordField, false);
        UiStyler.styleComboBox(roleField);
        UiStyler.styleComboBox(activeField);

        JPanel form = UiStyler.createLabeledFormPanel(
            "Username", usernameField,
            "New Password", passwordField,
            "Confirm Password", confirmPasswordField,
            "Role", roleField,
            "Active", activeField
        );

        if (JOptionPane.showConfirmDialog(this, form, "Edit User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
        {
            return;
        }

        try
        {
            String password = getPassword(passwordField);
            if (!password.equals(getPassword(confirmPasswordField)))
            {
                JOptionPane.showMessageDialog(this, "Passwords must match.", "User Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            userRepository.updateUser(
                id,
                usernameField.getText().trim(),
                password,
                roleField.getSelectedItem().toString(),
                "Yes".equals(activeField.getSelectedItem())
            );
            loadUsers();
        }
        catch (SQLException ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to update user.\n" + ex.getMessage(), "User Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getPassword(JPasswordField passwordField)
    {
        return new String(passwordField.getPassword());
    }
}
