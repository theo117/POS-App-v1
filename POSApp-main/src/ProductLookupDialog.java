import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class ProductLookupDialog extends JDialog
{
    private final ProductRepository productRepository;
    private final Consumer<Product> onProductSelected;
    private final JTextField queryField = new JTextField();
    private final JTable productsTable = new JTable();
    private List<Product> currentResults;

    public ProductLookupDialog(Frame owner, ProductRepository productRepository, Consumer<Product> onProductSelected)
    {
        super(owner, "Quick Add Product", true);
        this.productRepository = productRepository;
        this.onProductSelected = onProductSelected;
        buildUi();
        refreshResults();
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

        JLabel titleLabel = new JLabel("Quick Add Product");
        titleLabel.setFont(new java.awt.Font("Segoe UI Semibold", java.awt.Font.BOLD, 22));
        titleLabel.setForeground(new Color(27, 38, 58));

        JLabel subtitleLabel = new JLabel("Search by name or barcode, then send the selected item straight to the cart.");
        subtitleLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(92, 103, 118));

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.CENTER);

        queryField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                refreshResults();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                refreshResults();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                refreshResults();
            }
        });

        productsTable.setModel(new DefaultTableModel(new Object[]{"Name", "Barcode", "Price", "Stock"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        });
        productsTable.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        productsTable.setRowHeight(34);
        productsTable.setSelectionBackground(new Color(219, 236, 255));
        productsTable.setSelectionForeground(new Color(27, 38, 58));
        productsTable.getTableHeader().setFont(new java.awt.Font("Segoe UI Semibold", java.awt.Font.BOLD, 13));
        productsTable.getTableHeader().setBackground(new Color(242, 245, 249));
        productsTable.getTableHeader().setForeground(new Color(48, 60, 79));

        queryField.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        queryField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(196, 206, 221)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        queryField.setToolTipText("Search by product name or barcode");

        JButton addButton = new JButton("Add Selected");
        addButton.setFont(new java.awt.Font("Segoe UI Semibold", java.awt.Font.BOLD, 13));
        addButton.setFocusPainted(false);
        addButton.setBackground(new Color(18, 117, 90));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(evt -> selectCurrentProduct());

        JScrollPane scrollPane = new JScrollPane(productsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(205, 214, 226)));

        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(queryField, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(addButton, BorderLayout.SOUTH);

        setSize(640, 420);
        setLocationRelativeTo(getOwner());
    }

    private void refreshResults()
    {
        try
        {
            String query = queryField.getText().trim();
            currentResults = productRepository.searchActiveProducts(query.isEmpty() ? "" : query, 100);
            DefaultTableModel model = (DefaultTableModel) productsTable.getModel();
            model.setRowCount(0);
            for (Product product : currentResults)
            {
                model.addRow(new Object[]{product.getName(), product.getBarcode(), MoneyUtils.format(product.getUnitPrice()), product.getStockQuantity()});
            }
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to search products.\n" + ex.getMessage(), "Lookup Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectCurrentProduct()
    {
        int row = productsTable.getSelectedRow();
        if (row < 0 || currentResults == null || row >= currentResults.size())
        {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }

        onProductSelected.accept(currentResults.get(row));
        dispose();
    }
}
