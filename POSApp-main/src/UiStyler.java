import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

public final class UiStyler
{
    private static final Color DIALOG_BACKGROUND = new Color(244, 247, 250);
    private static final Color PANEL_BACKGROUND = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(216, 224, 234);
    private static final Color TEXT_PRIMARY = new Color(32, 45, 60);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color ACCENT = new Color(18, 117, 90);
    private static final Color SECONDARY = new Color(234, 239, 245);
    private static final Color DANGER = new Color(244, 224, 226);

    private UiStyler()
    {
    }

    public static void styleDialog(JDialog dialog, String title, String subtitle)
    {
        dialog.setLayout(new BorderLayout(12, 12));
        JPanel content = (JPanel) dialog.getContentPane();
        content.setBackground(DIALOG_BACKGROUND);
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
    }

    public static JPanel createHeader(String title, String subtitle)
    {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 21));
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(TEXT_MUTED);

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.CENTER);
        return header;
    }

    public static JPanel createSectionPanel(String title, Component component)
    {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(true);
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(createSectionBorder(title));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    public static javax.swing.border.Border createSectionBorder(String title)
    {
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            title
        );
        titledBorder.setTitleFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        titledBorder.setTitleColor(ACCENT);
        return BorderFactory.createCompoundBorder(
            titledBorder,
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );
    }

    public static JPanel createLabeledFormPanel(Object... entries)
    {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setOpaque(false);
        for (int index = 0; index < entries.length; index += 2)
        {
            JLabel label = new JLabel(String.valueOf(entries[index]));
            styleLabel(label);
            panel.add(label);
            panel.add((Component) entries[index + 1]);
        }
        return panel;
    }

    public static void styleLabel(JLabel label)
    {
        label.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        label.setForeground(TEXT_PRIMARY);
        label.setHorizontalAlignment(SwingConstants.LEFT);
    }

    public static void styleTextComponent(JTextComponent component, boolean readOnly)
    {
        component.setFont(new Font("Segoe UI", readOnly ? Font.BOLD : Font.PLAIN, readOnly ? 14 : 14));
        component.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        component.setBackground(readOnly ? new Color(245, 248, 252) : Color.WHITE);
        component.setForeground(TEXT_PRIMARY);
        component.setEditable(!readOnly);
    }

    public static void styleComboBox(JComboBox<?> comboBox)
    {
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(TEXT_PRIMARY);
    }

    public static void styleTable(JTable table)
    {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(229, 234, 241));
        table.setSelectionBackground(new Color(220, 238, 247));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.getTableHeader().setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(48, 60, 79));
        table.getTableHeader().setReorderingAllowed(false);
    }

    public static JScrollPane wrapTable(JTable table)
    {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    public static void styleTextArea(JTextArea area, boolean readOnly)
    {
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        area.setBackground(readOnly ? new Color(245, 248, 252) : Color.WHITE);
        area.setForeground(TEXT_PRIMARY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(!readOnly);
    }

    public static void stylePrimaryButton(JButton button)
    {
        styleButton(button, ACCENT, Color.WHITE);
    }

    public static void styleSecondaryButton(JButton button)
    {
        styleButton(button, SECONDARY, TEXT_PRIMARY);
    }

    public static void styleDangerButton(JButton button)
    {
        styleButton(button, DANGER, new Color(133, 34, 47));
    }

    public static void styleButton(JButton button, Color background, Color foreground)
    {
        button.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(9, 14, 9, 14)
        ));
    }

    public static void setOpaqueBackground(Container container)
    {
        for (Component component : container.getComponents())
        {
            if (component instanceof JPanel panel)
            {
                if (panel.isOpaque())
                {
                    panel.setBackground(PANEL_BACKGROUND);
                }
                setOpaqueBackground(panel);
            }
        }
    }
}
