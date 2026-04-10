import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class SalesHistoryDialog extends JDialog
{
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final SaleRepository saleRepository;
    private final ReceiptService receiptService;
    private final UserAccount currentUser;
    private final SessionManager sessionManager;
    private final JTable salesTable = new JTable();
    private final JTable paymentTable = new JTable();
    private final JTable topProductsTable = new JTable();
    private final JTextArea detailsArea = new JTextArea();
    private final JTextField startDateField = new JTextField();
    private final JTextField endDateField = new JTextField();
    private final JTextField transactionsField = new JTextField();
    private final JTextField subtotalField = new JTextField();
    private final JTextField taxField = new JTextField();
    private final JTextField totalField = new JTextField();
    private final JButton refundButton = new JButton("Refund Selected Sale");
    private final JButton reprintButton = new JButton("Reprint Receipt");

    public SalesHistoryDialog(Frame owner, SaleRepository saleRepository, ReceiptService receiptService, UserAccount currentUser, SessionManager sessionManager)
    {
        super(owner, "Sales History", true);
        this.saleRepository = saleRepository;
        this.receiptService = receiptService;
        this.currentUser = currentUser;
        this.sessionManager = sessionManager;
        buildUi();
        loadDashboard();
    }

    private void buildUi()
    {
        UiStyler.styleDialog(this, "Sales History", "Review sales activity, payment trends, receipts, and refund actions from a single reporting view.");
        JPanel topPanel = new JPanel(new BorderLayout(0, 12));
        topPanel.setOpaque(false);
        topPanel.add(UiStyler.createHeader("Sales History", "Review sales activity, payment trends, receipts, and refund actions from a single reporting view."), BorderLayout.NORTH);
        topPanel.add(buildFilterPanel(), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);

        setSize(980, 680);
        setLocationRelativeTo(getOwner());
    }

    private JPanel buildFilterPanel()
    {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JPanel inputs = new JPanel(new GridLayout(2, 4, 8, 8));
        inputs.setOpaque(false);
        inputs.setBorder(UiStyler.createSectionBorder("Filters"));
        UiStyler.styleTextComponent(startDateField, false);
        UiStyler.styleTextComponent(endDateField, false);
        JLabel startDateLabel = new JLabel("Start Date");
        JLabel endDateLabel = new JLabel("End Date");
        JLabel formatLabel = new JLabel("Format");
        JLabel tipLabel = new JLabel("Quick Tip");
        UiStyler.styleLabel(startDateLabel);
        UiStyler.styleLabel(endDateLabel);
        UiStyler.styleLabel(formatLabel);
        UiStyler.styleLabel(tipLabel);
        JLabel formatValueLabel = new JLabel("yyyy-MM-dd");
        JLabel tipValueLabel = new JLabel("Leave blank for all dates");
        inputs.add(startDateLabel);
        inputs.add(startDateField);
        inputs.add(endDateLabel);
        inputs.add(endDateField);
        inputs.add(formatLabel);
        inputs.add(formatValueLabel);
        inputs.add(tipLabel);
        inputs.add(tipValueLabel);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton applyButton = new JButton("Apply");
        JButton clearButton = new JButton("Clear");
        JButton refreshButton = new JButton("Refresh");
        UiStyler.stylePrimaryButton(applyButton);
        UiStyler.styleSecondaryButton(clearButton);
        UiStyler.styleSecondaryButton(refreshButton);
        UiStyler.styleSecondaryButton(reprintButton);
        UiStyler.styleDangerButton(refundButton);
        refundButton.addActionListener(evt -> refundSelectedSale());
        reprintButton.addActionListener(evt -> reprintSelectedSale());
        applyButton.addActionListener(evt -> loadDashboard());
        clearButton.addActionListener(evt -> {
            startDateField.setText("");
            endDateField.setText("");
            loadDashboard();
        });
        refreshButton.addActionListener(evt -> loadDashboard());
        buttons.add(applyButton);
        buttons.add(clearButton);
        buttons.add(refreshButton);
        buttons.add(reprintButton);
        buttons.add(refundButton);
        refundButton.setEnabled(currentUser != null && currentUser.isAdmin());

        panel.add(inputs, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private JSplitPane buildMainPanel()
    {
        JPanel reportsPanel = new JPanel(new BorderLayout(12, 12));
        reportsPanel.setOpaque(false);
        reportsPanel.add(buildSummaryPanel(), BorderLayout.NORTH);
        reportsPanel.add(buildTabs(), BorderLayout.CENTER);

        UiStyler.styleTextArea(detailsArea, true);

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            reportsPanel,
            UiStyler.createSectionPanel("Sale Details", new JScrollPane(detailsArea))
        );
        splitPane.setResizeWeight(0.72);
        return splitPane;
    }

    private JPanel buildSummaryPanel()
    {
        JPanel panel = new JPanel(new GridLayout(2, 4, 8, 8));
        panel.setOpaque(false);

        configureReadOnlyField(transactionsField);
        configureReadOnlyField(subtotalField);
        configureReadOnlyField(taxField);
        configureReadOnlyField(totalField);

        JLabel transactionsLabel = new JLabel("Transactions");
        JLabel subtotalLabel = new JLabel("Subtotal");
        JLabel taxLabel = new JLabel("Tax");
        JLabel totalLabel = new JLabel("Total");
        UiStyler.styleLabel(transactionsLabel);
        UiStyler.styleLabel(subtotalLabel);
        UiStyler.styleLabel(taxLabel);
        UiStyler.styleLabel(totalLabel);

        panel.add(transactionsLabel);
        panel.add(transactionsField);
        panel.add(subtotalLabel);
        panel.add(subtotalField);
        panel.add(taxLabel);
        panel.add(taxField);
        panel.add(totalLabel);
        panel.add(totalField);

        return UiStyler.createSectionPanel("Summary", panel);
    }

    private JTabbedPane buildTabs()
    {
        salesTable.setModel(new DefaultTableModel(new Object[]{"Sale #", "Date", "Payment", "Status", "Total"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        });
        UiStyler.styleTable(salesTable);
        salesTable.getSelectionModel().addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting())
            {
                loadSelectedSaleDetails();
            }
        });

        paymentTable.setModel(new DefaultTableModel(new Object[]{"Payment Method", "Transactions", "Total"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        });
        UiStyler.styleTable(paymentTable);

        topProductsTable.setModel(new DefaultTableModel(new Object[]{"Product", "Qty Sold", "Revenue"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        });
        UiStyler.styleTable(topProductsTable);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sales", UiStyler.wrapTable(salesTable));
        tabs.addTab("Payments", UiStyler.wrapTable(paymentTable));
        tabs.addTab("Top Products", UiStyler.wrapTable(topProductsTable));
        return tabs;
    }

    private void configureReadOnlyField(JTextField field)
    {
        UiStyler.styleTextComponent(field, true);
    }

    private void loadDashboard()
    {
        try
        {
            LocalDate startDate = parseDate(startDateField.getText().trim(), "start");
            LocalDate endDate = parseDate(endDateField.getText().trim(), "end");
            if (startDate != null && endDate != null && endDate.isBefore(startDate))
            {
                JOptionPane.showMessageDialog(this, "End date cannot be before start date.");
                return;
            }

            loadSummary(startDate, endDate);
            loadSales(startDate, endDate);
            loadPayments(startDate, endDate);
            loadTopProducts(startDate, endDate);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to load reports.\n" + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSummary(LocalDate startDate, LocalDate endDate) throws Exception
    {
        SalesSummary summary = saleRepository.getSalesSummary(startDate, endDate);
        transactionsField.setText(String.valueOf(summary.getTransactionCount()));
        subtotalField.setText(MoneyUtils.format(summary.getSubtotal()));
        taxField.setText(MoneyUtils.format(summary.getTax()));
        totalField.setText(MoneyUtils.format(summary.getTotal()));
    }

    private void loadSales(LocalDate startDate, LocalDate endDate) throws Exception
    {
        DefaultTableModel model = (DefaultTableModel) salesTable.getModel();
        model.setRowCount(0);

        for (SaleRecord sale : saleRepository.findSales(startDate, endDate, 300))
        {
            model.addRow(new Object[]{
                sale.getId(),
                sale.getCreatedAt().format(DISPLAY_TIME_FORMAT),
                sale.getPaymentMethod(),
                sale.getStatus(),
                MoneyUtils.format(sale.getTotal())
            });
        }

        detailsArea.setText("");
        if (model.getRowCount() > 0)
        {
            salesTable.setRowSelectionInterval(0, 0);
        }
    }

    private void loadPayments(LocalDate startDate, LocalDate endDate) throws Exception
    {
        DefaultTableModel model = (DefaultTableModel) paymentTable.getModel();
        model.setRowCount(0);

        for (PaymentSummary paymentSummary : saleRepository.getPaymentSummaries(startDate, endDate))
        {
            model.addRow(new Object[]{
                paymentSummary.getPaymentMethod(),
                paymentSummary.getTransactionCount(),
                MoneyUtils.format(paymentSummary.getTotalAmount())
            });
        }
    }

    private void loadTopProducts(LocalDate startDate, LocalDate endDate) throws Exception
    {
        DefaultTableModel model = (DefaultTableModel) topProductsTable.getModel();
        model.setRowCount(0);

        for (TopProductSummary productSummary : saleRepository.getTopProducts(startDate, endDate, 20))
        {
            model.addRow(new Object[]{
                productSummary.getProductName(),
                productSummary.getQuantitySold(),
                MoneyUtils.format(productSummary.getRevenue())
            });
        }
    }

    private void loadSelectedSaleDetails()
    {
        int row = salesTable.getSelectedRow();
        if (row < 0)
        {
            detailsArea.setText("");
            return;
        }

        int saleId = Integer.parseInt(salesTable.getValueAt(row, 0).toString());

        try
        {
            List<SaleItemRecord> items = saleRepository.findSaleItems(saleId);
            StringBuilder details = new StringBuilder();
            details.append("Sale #").append(saleId).append('\n');
            details.append("Date: ").append(salesTable.getValueAt(row, 1)).append('\n');
            details.append("Payment: ").append(salesTable.getValueAt(row, 2)).append('\n');
            details.append("Status: ").append(salesTable.getValueAt(row, 3)).append('\n');
            details.append("Total: ").append(salesTable.getValueAt(row, 4)).append("\n\n");
            details.append("Items:\n");

            for (SaleItemRecord item : items)
            {
                details.append("- ")
                    .append(item.getProductName())
                    .append(" x")
                    .append(item.getQuantity())
                    .append(" @ ")
                    .append(MoneyUtils.format(item.getUnitPrice()))
                    .append(" = ")
                    .append(MoneyUtils.format(item.getLineTotal()))
                    .append('\n');
            }

            detailsArea.setText(details.toString());
            detailsArea.setCaretPosition(0);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to load sale details.\n" + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private LocalDate parseDate(String value, String fieldName)
    {
        if (value.isEmpty())
        {
            return null;
        }

        try
        {
            return LocalDate.parse(value);
        }
        catch (DateTimeParseException ex)
        {
            throw new IllegalArgumentException("Invalid " + fieldName + " date. Use yyyy-MM-dd.");
        }
    }

    private void refundSelectedSale()
    {
        int row = salesTable.getSelectedRow();
        if (row < 0)
        {
            JOptionPane.showMessageDialog(this, "Select a completed sale to refund.");
            return;
        }

        if (currentUser == null || !currentUser.isAdmin())
        {
            JOptionPane.showMessageDialog(this, "Only admin users can refund sales.");
            return;
        }

        if (!sessionManager.requireSensitiveReauthentication(this, "refund a sale"))
        {
            return;
        }

        int saleId = Integer.parseInt(salesTable.getValueAt(row, 0).toString());
        String status = salesTable.getValueAt(row, 3).toString();
        if (!"COMPLETED".equals(status))
        {
            JOptionPane.showMessageDialog(this, "Only completed sales can be refunded.");
            return;
        }

        if (JOptionPane.showConfirmDialog(
            this,
            "Refund sale #" + saleId + "? This will restore stock and create a refund record.",
            "Confirm Refund",
            JOptionPane.YES_NO_OPTION
        ) != JOptionPane.YES_OPTION)
        {
            return;
        }

        try
        {
            saleRepository.refundSale(saleId);
            loadDashboard();
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to refund sale.\n" + ex.getMessage(), "Refund Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reprintSelectedSale()
    {
        int row = salesTable.getSelectedRow();
        if (row < 0)
        {
            JOptionPane.showMessageDialog(this, "Select a sale first.");
            return;
        }

        int saleId = Integer.parseInt(salesTable.getValueAt(row, 0).toString());
        try
        {
            SaleRecord sale = null;
            for (SaleRecord record : saleRepository.findRecentSales(500))
            {
                if (record.getId() == saleId)
                {
                    sale = record;
                    break;
                }
            }
            if (sale == null)
            {
                JOptionPane.showMessageDialog(this, "Unable to find that sale.");
                return;
            }

            List<SaleItemRecord> items = saleRepository.findSaleItems(saleId);
            String receipt = receiptService.buildReceiptFromSale(sale, items);
            JOptionPane.showMessageDialog(this, receipt, "Receipt Reprint", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to reprint receipt.\n" + ex.getMessage(), "Receipt Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
