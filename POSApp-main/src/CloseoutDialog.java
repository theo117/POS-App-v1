import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class CloseoutDialog extends JDialog
{
    private final SaleRepository saleRepository;
    private final JTextField transactionsField = new JTextField();
    private final JTextField subtotalField = new JTextField();
    private final JTextField taxField = new JTextField();
    private final JTextField totalField = new JTextField();
    private final JTable paymentsTable = new JTable();

    public CloseoutDialog(Frame owner, SaleRepository saleRepository)
    {
        super(owner, "End Of Day Closeout", true);
        this.saleRepository = saleRepository;
        buildUi();
        loadToday();
    }

    private void buildUi()
    {
        UiStyler.styleDialog(this, "End Of Day Closeout", "Review today’s totals and payment mix before wrapping up the trading day.");
        JPanel topPanel = new JPanel(new BorderLayout(0, 12));
        topPanel.setOpaque(false);
        topPanel.add(UiStyler.createHeader("End Of Day Closeout", "Review today’s totals and payment mix before wrapping up the trading day."), BorderLayout.NORTH);

        JPanel summary = new JPanel(new GridLayout(2, 4, 8, 8));
        summary.setOpaque(false);
        configure(transactionsField);
        configure(subtotalField);
        configure(taxField);
        configure(totalField);
        JLabel transactionsLabel = new JLabel("Transactions");
        JLabel subtotalLabel = new JLabel("Subtotal");
        JLabel taxLabel = new JLabel("Tax");
        JLabel totalLabel = new JLabel("Total");
        UiStyler.styleLabel(transactionsLabel);
        UiStyler.styleLabel(subtotalLabel);
        UiStyler.styleLabel(taxLabel);
        UiStyler.styleLabel(totalLabel);
        summary.add(transactionsLabel);
        summary.add(transactionsField);
        summary.add(subtotalLabel);
        summary.add(subtotalField);
        summary.add(taxLabel);
        summary.add(taxField);
        summary.add(totalLabel);
        summary.add(totalField);

        paymentsTable.setModel(new DefaultTableModel(new Object[]{"Payment", "Transactions", "Total"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        });
        UiStyler.styleTable(paymentsTable);

        topPanel.add(UiStyler.createSectionPanel("Today", summary), BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
        add(UiStyler.createSectionPanel("Payments", UiStyler.wrapTable(paymentsTable)), BorderLayout.CENTER);
        setSize(640, 420);
        setLocationRelativeTo(getOwner());
    }

    private void loadToday()
    {
        try
        {
            LocalDate today = LocalDate.now();
            SalesSummary summary = saleRepository.getSalesSummary(today, today);
            transactionsField.setText(String.valueOf(summary.getTransactionCount()));
            subtotalField.setText(MoneyUtils.format(summary.getSubtotal()));
            taxField.setText(MoneyUtils.format(summary.getTax()));
            totalField.setText(MoneyUtils.format(summary.getTotal()));

            DefaultTableModel model = (DefaultTableModel) paymentsTable.getModel();
            model.setRowCount(0);
            List<PaymentSummary> payments = saleRepository.getPaymentSummaries(today, today);
            for (PaymentSummary payment : payments)
            {
                model.addRow(new Object[]{payment.getPaymentMethod(), payment.getTransactionCount(), MoneyUtils.format(payment.getTotalAmount())});
            }
        }
        catch (Exception ex)
        {
            transactionsField.setText("Error");
        }
    }

    private void configure(JTextField field)
    {
        UiStyler.styleTextComponent(field, true);
    }
}
