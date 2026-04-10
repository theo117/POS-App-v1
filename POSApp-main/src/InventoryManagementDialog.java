import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class InventoryManagementDialog extends JDialog
{
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final Runnable onInventoryChanged;
    private final SessionManager sessionManager;
    private final JTable productsTable = new JTable();
    private final JTable lowStockTable = new JTable();
    private final JTextArea movementsArea = new JTextArea();
    private final JTextField thresholdField = new JTextField("5");

    public InventoryManagementDialog(Frame owner, ProductRepository productRepository, InventoryRepository inventoryRepository, Runnable onInventoryChanged, SessionManager sessionManager)
    {
        super(owner, "Inventory Management", true);
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.onInventoryChanged = onInventoryChanged;
        this.sessionManager = sessionManager;
        buildUi();
        loadData();
    }

    private void buildUi()
    {
        UiStyler.styleDialog(this, "Inventory Management", "Track stock health, recent movements, and quick corrections without leaving the back office.");
        JPanel topPanel = new JPanel(new BorderLayout(0, 12));
        topPanel.setOpaque(false);
        topPanel.add(UiStyler.createHeader("Inventory Management", "Track stock health, recent movements, and quick corrections without leaving the back office."), BorderLayout.NORTH);
        topPanel.add(buildActionsPanel(), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);

        setSize(980, 680);
        setLocationRelativeTo(getOwner());
    }

    private JPanel buildActionsPanel()
    {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        JButton restockButton = new JButton("Restock");
        JButton adjustButton = new JButton("Adjust");
        JButton refreshButton = new JButton("Refresh");
        UiStyler.stylePrimaryButton(restockButton);
        UiStyler.styleSecondaryButton(adjustButton);
        UiStyler.styleSecondaryButton(refreshButton);
        restockButton.addActionListener(evt -> openStockChangeDialog("RESTOCK"));
        adjustButton.addActionListener(evt -> openStockChangeDialog("ADJUSTMENT"));
        refreshButton.addActionListener(evt -> loadData());
        controls.add(restockButton);
        controls.add(adjustButton);
        controls.add(refreshButton);

        JPanel thresholdPanel = new JPanel();
        thresholdPanel.setOpaque(false);
        JLabel thresholdLabel = new JLabel("Low Stock Threshold");
        UiStyler.styleLabel(thresholdLabel);
        thresholdPanel.add(thresholdLabel);
        UiStyler.styleTextComponent(thresholdField, false);
        thresholdField.setColumns(5);
        thresholdPanel.add(thresholdField);
        JButton applyThresholdButton = new JButton("Apply");
        UiStyler.styleSecondaryButton(applyThresholdButton);
        applyThresholdButton.addActionListener(evt -> loadLowStock());
        thresholdPanel.add(applyThresholdButton);

        panel.add(controls, BorderLayout.WEST);
        panel.add(thresholdPanel, BorderLayout.EAST);
        return panel;
    }

    private JSplitPane buildMainPanel()
    {
        productsTable.setModel(new DefaultTableModel(new Object[]{"ID", "Name", "Category", "Price", "Stock"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        });
        UiStyler.styleTable(productsTable);

        lowStockTable.setModel(new DefaultTableModel(new Object[]{"Name", "Stock"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        });
        UiStyler.styleTable(lowStockTable);

        UiStyler.styleTextArea(movementsArea, true);

        JSplitPane rightPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            UiStyler.createSectionPanel("Low Stock", UiStyler.wrapTable(lowStockTable)),
            UiStyler.createSectionPanel("Recent Movements", new JScrollPane(movementsArea))
        );
        rightPane.setResizeWeight(0.35);

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            UiStyler.createSectionPanel("Products", UiStyler.wrapTable(productsTable)),
            rightPane
        );
        splitPane.setResizeWeight(0.58);
        return splitPane;
    }

    private void loadData()
    {
        loadProducts();
        loadLowStock();
        loadMovements();
        onInventoryChanged.run();
    }

    private void loadProducts()
    {
        try
        {
            DefaultTableModel model = (DefaultTableModel) productsTable.getModel();
            model.setRowCount(0);
            for (Product product : productRepository.findAll())
            {
                model.addRow(new Object[]{
                    product.getId(),
                    product.getName(),
                    product.getCategory(),
                    MoneyUtils.format(product.getUnitPrice()),
                    product.getStockQuantity()
                });
            }
        }
        catch (SQLException ex)
        {
            showError("Unable to load products.", ex);
        }
    }

    private void loadLowStock()
    {
        try
        {
            int threshold = Integer.parseInt(thresholdField.getText().trim());
            DefaultTableModel model = (DefaultTableModel) lowStockTable.getModel();
            model.setRowCount(0);
            for (Product product : inventoryRepository.findLowStockProducts(threshold))
            {
                model.addRow(new Object[]{product.getName(), product.getStockQuantity()});
            }
        }
        catch (NumberFormatException ex)
        {
            JOptionPane.showMessageDialog(this, "Enter a valid low stock threshold.");
        }
        catch (SQLException ex)
        {
            showError("Unable to load low stock products.", ex);
        }
    }

    private void loadMovements()
    {
        try
        {
            StringBuilder builder = new StringBuilder();
            List<StockMovementRecord> movements = inventoryRepository.findRecentMovements(40);
            for (StockMovementRecord movement : movements)
            {
                builder.append(movement.getCreatedAt().format(DISPLAY_TIME_FORMAT))
                    .append(" | ")
                    .append(movement.getProductName())
                    .append(" | ")
                    .append(movement.getMovementType())
                    .append(" | Delta: ")
                    .append(movement.getQuantityDelta())
                    .append(" | ")
                    .append(movement.getStockBefore())
                    .append(" -> ")
                    .append(movement.getStockAfter());

                if (movement.getNote() != null && !movement.getNote().trim().isEmpty())
                {
                    builder.append(" | ").append(movement.getNote());
                }

                builder.append('\n');
            }

            movementsArea.setText(builder.toString());
            movementsArea.setCaretPosition(0);
        }
        catch (SQLException ex)
        {
            showError("Unable to load stock movements.", ex);
        }
    }

    private void openStockChangeDialog(String movementType)
    {
        int row = productsTable.getSelectedRow();
        if (row < 0)
        {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }

        if (!sessionManager.requireSensitiveReauthentication(this, "change inventory"))
        {
            return;
        }

        int productId = Integer.parseInt(productsTable.getValueAt(row, 0).toString());
        String productName = productsTable.getValueAt(row, 1).toString();
        String currentStock = productsTable.getValueAt(row, 4).toString();

        JTextField quantityField = new JTextField();
        JTextField noteField = new JTextField();
        JComboBox<String> modeField = new JComboBox<>(new String[]{"Add", "Set Absolute"});
        UiStyler.styleTextComponent(quantityField, false);
        UiStyler.styleTextComponent(noteField, false);
        UiStyler.styleComboBox(modeField);
        JLabel productLabel = new JLabel(productName);
        JLabel stockLabel = new JLabel(currentStock);
        productLabel.setForeground(new java.awt.Color(48, 60, 79));
        stockLabel.setForeground(new java.awt.Color(48, 60, 79));

        JPanel form = UiStyler.createLabeledFormPanel(
            "Product", productLabel,
            "Current Stock", stockLabel,
            "Mode", modeField,
            "Quantity", quantityField,
            "Note", noteField
        );

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            movementType.equals("RESTOCK") ? "Restock Product" : "Adjust Stock",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION)
        {
            return;
        }

        try
        {
            int enteredQuantity = Integer.parseInt(quantityField.getText().trim());
            int currentStockValue = Integer.parseInt(currentStock);
            int delta = "Set Absolute".equals(modeField.getSelectedItem())
                ? enteredQuantity - currentStockValue
                : enteredQuantity;

            if ("RESTOCK".equals(movementType) && delta < 0)
            {
                JOptionPane.showMessageDialog(this, "Restock quantity must be zero or greater.");
                return;
            }

            inventoryRepository.adjustStock(productId, delta, movementType, noteField.getText().trim());
            loadData();
        }
        catch (NumberFormatException ex)
        {
            JOptionPane.showMessageDialog(this, "Enter a valid quantity.");
        }
        catch (SQLException ex)
        {
            showError("Unable to update stock.", ex);
        }
    }

    private void showError(String message, Exception exception)
    {
        JOptionPane.showMessageDialog(this, message + "\n" + exception.getMessage(), "Inventory Error", JOptionPane.ERROR_MESSAGE);
    }
}
