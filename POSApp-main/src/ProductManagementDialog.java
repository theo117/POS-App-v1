import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class ProductManagementDialog extends JDialog
{
    private final ProductRepository productRepository;
    private final Runnable onProductsChanged;
    private final JTable productTable = new JTable();

    public ProductManagementDialog(Frame owner, ProductRepository productRepository, Runnable onProductsChanged)
    {
        super(owner, "Manage Products", true);
        this.productRepository = productRepository;
        this.onProductsChanged = onProductsChanged;
        buildUi();
        loadProducts();
    }

    private void buildUi()
    {
        UiStyler.styleDialog(this, "Manage Products", "Maintain your menu catalog, pricing, stock, and visibility from one screen.");
        add(UiStyler.createHeader("Manage Products", "Maintain your menu catalog, pricing, stock, and visibility from one screen."), BorderLayout.NORTH);

        productTable.setModel(new DefaultTableModel(new Object[]{"ID", "Name", "Barcode", "Price", "Category", "Order", "Stock", "Active"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        });
        UiStyler.styleTable(productTable);

        add(UiStyler.createSectionPanel("Product Catalog", UiStyler.wrapTable(productTable)), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 4, 8, 8));
        actions.setOpaque(false);
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");
        UiStyler.stylePrimaryButton(addButton);
        UiStyler.styleSecondaryButton(editButton);
        UiStyler.styleDangerButton(deleteButton);
        UiStyler.styleSecondaryButton(refreshButton);

        addButton.addActionListener(evt -> addProduct());
        editButton.addActionListener(evt -> editSelectedProduct());
        deleteButton.addActionListener(evt -> deleteSelectedProduct());
        refreshButton.addActionListener(evt -> loadProducts());

        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(refreshButton);
        add(actions, BorderLayout.SOUTH);

        setSize(760, 420);
        setLocationRelativeTo(getOwner());
    }

    private void loadProducts()
    {
        try
        {
            DefaultTableModel model = (DefaultTableModel) productTable.getModel();
            model.setRowCount(0);
            for (Product product : productRepository.findAll())
            {
                model.addRow(new Object[]{
                    product.getId(),
                    product.getName(),
                    product.getBarcode(),
                    MoneyUtils.format(product.getUnitPrice()),
                    product.getCategory(),
                    product.getDisplayOrder(),
                    product.getStockQuantity(),
                    product.isActive() ? "Yes" : "No"
                });
            }
            onProductsChanged.run();
        }
        catch (SQLException ex)
        {
            showError("Unable to load products.", ex);
        }
    }

    private void addProduct()
    {
        Product product = promptForProduct(null);
        if (product == null)
        {
            return;
        }

        try
        {
            productRepository.save(product);
            loadProducts();
        }
        catch (SQLException ex)
        {
            showError("Unable to save product.", ex);
        }
    }

    private void editSelectedProduct()
    {
        int row = productTable.getSelectedRow();
        if (row < 0)
        {
            JOptionPane.showMessageDialog(this, "Select a product to edit.");
            return;
        }

        Product product = selectedRowToProduct(row);
        Product updatedProduct = promptForProduct(product);
        if (updatedProduct == null)
        {
            return;
        }

        try
        {
            productRepository.update(updatedProduct);
            loadProducts();
        }
        catch (SQLException ex)
        {
            showError("Unable to update product.", ex);
        }
    }

    private void deleteSelectedProduct()
    {
        int row = productTable.getSelectedRow();
        if (row < 0)
        {
            JOptionPane.showMessageDialog(this, "Select a product to delete.");
            return;
        }

        int productId = Integer.parseInt(productTable.getValueAt(row, 0).toString());
        if (JOptionPane.showConfirmDialog(this, "Delete the selected product?", "Manage Products", JOptionPane.YES_NO_OPTION)
            != JOptionPane.YES_OPTION)
        {
            return;
        }

        try
        {
            productRepository.delete(productId);
            loadProducts();
        }
        catch (SQLException ex)
        {
            showError("Unable to delete product.", ex);
        }
    }

    private Product promptForProduct(Product existingProduct)
    {
        JTextField nameField = new JTextField(existingProduct == null ? "" : existingProduct.getName());
        JTextField barcodeField = new JTextField(existingProduct == null ? "" : defaultString(existingProduct.getBarcode()));
        JTextField priceField = new JTextField(existingProduct == null ? "" : existingProduct.getUnitPrice().toPlainString());
        JComboBox<String> categoryField = new JComboBox<>(new String[]{"DRINK", "MAIN", "DESSERT", "SEAFOOD"});
        JTextField displayOrderField = new JTextField(existingProduct == null ? "" : String.valueOf(existingProduct.getDisplayOrder()));
        JTextField stockField = new JTextField(existingProduct == null ? "" : String.valueOf(existingProduct.getStockQuantity()));
        JComboBox<String> activeField = new JComboBox<>(new String[]{"Yes", "No"});
        UiStyler.styleTextComponent(nameField, false);
        UiStyler.styleTextComponent(barcodeField, false);
        UiStyler.styleTextComponent(priceField, false);
        UiStyler.styleTextComponent(displayOrderField, false);
        UiStyler.styleTextComponent(stockField, false);
        UiStyler.styleComboBox(categoryField);
        UiStyler.styleComboBox(activeField);

        if (existingProduct != null)
        {
            categoryField.setSelectedItem(existingProduct.getCategory());
            activeField.setSelectedItem(existingProduct.isActive() ? "Yes" : "No");
        }

        JPanel form = UiStyler.createLabeledFormPanel(
            "Name", nameField,
            "Barcode", barcodeField,
            "Price", priceField,
            "Category", categoryField,
            "Display Order", displayOrderField,
            "Stock Quantity", stockField,
            "Active", activeField
        );

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            existingProduct == null ? "Add Product" : "Edit Product",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION)
        {
            return null;
        }

        try
        {
            String name = nameField.getText().trim();
            BigDecimal price = MoneyUtils.scale(new BigDecimal(priceField.getText().trim()));
            int displayOrder = Integer.parseInt(displayOrderField.getText().trim());
            int stockQuantity = Integer.parseInt(stockField.getText().trim());
            boolean active = "Yes".equals(activeField.getSelectedItem());

            if (name.isEmpty())
            {
                JOptionPane.showMessageDialog(this, "Product name is required.");
                return null;
            }

            if (stockQuantity < 0)
            {
                JOptionPane.showMessageDialog(this, "Stock quantity cannot be negative.");
                return null;
            }

            return new Product(
                existingProduct == null ? null : existingProduct.getId(),
                name,
                price,
                categoryField.getSelectedItem().toString(),
                displayOrder,
                active,
                stockQuantity,
                barcodeField.getText().trim().isEmpty() ? null : barcodeField.getText().trim()
            );
        }
        catch (NumberFormatException ex)
        {
            JOptionPane.showMessageDialog(this, "Enter a valid price and display order.");
            return null;
        }
    }

    private Product selectedRowToProduct(int row)
    {
        int id = Integer.parseInt(productTable.getValueAt(row, 0).toString());
        String name = productTable.getValueAt(row, 1).toString();
        String barcode = valueOrNull(productTable.getValueAt(row, 2));
        String price = productTable.getValueAt(row, 3).toString().replace("R ", "");
        String category = productTable.getValueAt(row, 4).toString();
        int displayOrder = Integer.parseInt(productTable.getValueAt(row, 5).toString());
        int stockQuantity = Integer.parseInt(productTable.getValueAt(row, 6).toString());
        boolean active = "Yes".equals(productTable.getValueAt(row, 7).toString());
        return new Product(id, name, new BigDecimal(price), category, displayOrder, active, stockQuantity, barcode);
    }

    private void showError(String message, Exception exception)
    {
        JOptionPane.showMessageDialog(this, message + "\n" + exception.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private String defaultString(String value)
    {
        return value == null ? "" : value;
    }

    private String valueOrNull(Object value)
    {
        if (value == null)
        {
            return null;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
