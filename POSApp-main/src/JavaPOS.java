import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author Latitude 7480
 */
public class JavaPOS extends javax.swing.JFrame {
    private static final BigDecimal TAX_RATE_PERCENT = new BigDecimal("15.0");
    private static final DateTimeFormatter RECEIPT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final CartService cartService = new CartService(TAX_RATE_PERCENT);
    private final ReceiptService receiptService = new ReceiptService(RECEIPT_TIME_FORMAT);
    private final DatabaseManager databaseManager = new DatabaseManager();
    private final ProductRepository productRepository = new ProductRepository(databaseManager);
    private final UserRepository userRepository = new UserRepository(databaseManager);
    private final InventoryRepository inventoryRepository = new InventoryRepository(databaseManager, productRepository);
    private final SaleRepository saleRepository = new SaleRepository(databaseManager, productRepository, inventoryRepository);
    private final ReceiptService archivedReceiptService = new ReceiptService(RECEIPT_TIME_FORMAT);
    private final UserAccount currentUser;
    private final SessionManager sessionManager;
    private final JPanel headerPanel = new JPanel(null);
    private final JLabel headerTitleLabel = new JLabel("Counter");
    private final JLabel headerSubtitleLabel = new JLabel("Fast checkout, inventory awareness, and cleaner operator flow.");
    private final JLabel sessionBadgeLabel = new JLabel();
    private final JButton quickLookupButton = new JButton("Quick Add");
    private JButton[] menuButtons;
    private JButton[] keypadButtons;
    private Product[] menuProducts;
    private BigDecimal lastCashAmount = BigDecimal.ZERO;
    private BigDecimal lastChangeAmount = BigDecimal.ZERO;
    private final boolean persistenceReady;

    /**
     * Creates new form JavaPOS
     */
    public JavaPOS(UserAccount currentUser) {
        this(currentUser, false);
    }

    public JavaPOS(UserAccount currentUser, boolean persistenceReady) {
        this.currentUser = currentUser;
        this.persistenceReady = persistenceReady;
        this.sessionManager = new SessionManager(currentUser, userRepository);
        initComponents();
        if (!this.persistenceReady)
        {
            initializePersistence();
        }
        initializeHeader();
        configureUiTheme();
        initializeAdminMenu();
        jTxtSubTotal.setEditable(false);
        jTxtTax.setEditable(false);
        jTxtTotal.setEditable(false);
        jTxtChange.setEditable(false);
        ItemCost();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                applyResponsiveLayout();
            }
        });
        sessionManager.startTracking();
        SwingUtilities.invokeLater(this::applyResponsiveLayout);
    }

    private void initializeHeader()
    {
        headerPanel.add(headerTitleLabel);
        headerPanel.add(headerSubtitleLabel);
        headerPanel.add(sessionBadgeLabel);
        headerPanel.add(quickLookupButton);
        quickLookupButton.addActionListener(evt -> openProductLookupDialog());
        getContentPane().add(headerPanel);
    }

    private void initializePersistence()
    {
        try
        {
            databaseManager.initialize();
            productRepository.seedDefaultsIfEmpty();
            userRepository.seedDefaultsIfEmpty();
        }
        catch (SQLException ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to initialize the local database.\n" + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeAdminMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        JMenu adminMenu = new JMenu("Admin");
        JMenuItem manageProductsItem = new JMenuItem("Manage Products");
        JMenuItem manageUsersItem = new JMenuItem("Manage Users");
        JMenuItem manageInventoryItem = new JMenuItem("Manage Inventory");
        JMenuItem viewSalesItem = new JMenuItem("Sales History");
        JMenuItem closeoutItem = new JMenuItem("Closeout");
        JMenuItem backupRestoreItem = new JMenuItem("Backup / Restore");
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem quickAddItem = new JMenuItem("Quick Add Product");
        manageProductsItem.addActionListener(evt -> openProductManagementDialog());
        manageUsersItem.addActionListener(evt -> openUserManagementDialog());
        manageInventoryItem.addActionListener(evt -> openInventoryManagementDialog());
        viewSalesItem.addActionListener(evt -> openSalesHistoryDialog());
        closeoutItem.addActionListener(evt -> openCloseoutDialog());
        backupRestoreItem.addActionListener(evt -> openBackupRestoreDialog());
        quickAddItem.addActionListener(evt -> openProductLookupDialog());
        adminMenu.add(manageProductsItem);
        adminMenu.add(manageUsersItem);
        adminMenu.add(manageInventoryItem);
        adminMenu.add(viewSalesItem);
        adminMenu.add(closeoutItem);
        adminMenu.add(backupRestoreItem);
        toolsMenu.add(quickAddItem);
        boolean adminEnabled = currentUser != null && currentUser.isAdmin();
        manageProductsItem.setEnabled(adminEnabled);
        manageUsersItem.setEnabled(adminEnabled);
        manageInventoryItem.setEnabled(adminEnabled);
        closeoutItem.setEnabled(adminEnabled);
        backupRestoreItem.setEnabled(adminEnabled);
        menuBar.add(adminMenu);
        menuBar.add(toolsMenu);
        setJMenuBar(menuBar);
    }

    private void openProductManagementDialog()
    {
        if (!ensureAdminAccess("manage products"))
        {
            return;
        }
        if (!sessionManager.requireActiveSession(this, "manage products"))
        {
            return;
        }
        ProductManagementDialog dialog = new ProductManagementDialog(this, productRepository, this::loadMenuProducts);
        dialog.setVisible(true);
    }

    private void openUserManagementDialog()
    {
        if (!ensureAdminAccess("manage users"))
        {
            return;
        }
        if (!sessionManager.requireActiveSession(this, "manage users"))
        {
            return;
        }
        UserManagementDialog dialog = new UserManagementDialog(this, userRepository);
        dialog.setVisible(true);
    }

    private void openInventoryManagementDialog()
    {
        if (!ensureAdminAccess("manage inventory"))
        {
            return;
        }
        if (!sessionManager.requireActiveSession(this, "manage inventory"))
        {
            return;
        }
        InventoryManagementDialog dialog = new InventoryManagementDialog(this, productRepository, inventoryRepository, this::loadMenuProducts, sessionManager);
        dialog.setVisible(true);
    }

    private void openSalesHistoryDialog()
    {
        if (!sessionManager.requireActiveSession(this, "view sales history"))
        {
            return;
        }
        SalesHistoryDialog dialog = new SalesHistoryDialog(this, saleRepository, archivedReceiptService, currentUser, sessionManager);
        dialog.setVisible(true);
    }

    private void openCloseoutDialog()
    {
        if (!ensureAdminAccess("run closeout"))
        {
            return;
        }
        if (!sessionManager.requireActiveSession(this, "run closeout"))
        {
            return;
        }
        CloseoutDialog dialog = new CloseoutDialog(this, saleRepository);
        dialog.setVisible(true);
    }

    private void openBackupRestoreDialog()
    {
        if (!ensureAdminAccess("use backup and restore"))
        {
            return;
        }
        if (!sessionManager.requireActiveSession(this, "use backup and restore"))
        {
            return;
        }
        BackupRestoreDialog dialog = new BackupRestoreDialog(this, databaseManager, this::reloadAfterRestore);
        dialog.setVisible(true);
    }

    private void openProductLookupDialog()
    {
        if (!sessionManager.requireActiveSession(this, "add products"))
        {
            return;
        }
        ProductLookupDialog dialog = new ProductLookupDialog(this, productRepository, this::addItemToBill);
        dialog.setVisible(true);
    }

    private void reloadAfterRestore()
    {
        clearOrder(true);
        loadMenuProducts();
    }

    private boolean ensureAdminAccess(String action)
    {
        if (currentUser != null && currentUser.isAdmin())
        {
            return true;
        }

        JOptionPane.showMessageDialog(this, "You do not have permission to " + action + ".");
        return false;
    }

    private void configureUiTheme()
    {
        UIManager.put("ToolTip.background", new Color(29, 40, 56));
        UIManager.put("ToolTip.foreground", Color.WHITE);

        Color appBackground = new Color(241, 244, 248);
        Color panelBackground = new Color(255, 252, 247);
        Color chromeBackground = new Color(27, 38, 58);
        Color menuButtonBackground = new Color(245, 247, 250);
        Color keypadBackground = new Color(236, 240, 245);
        Color primaryAction = new Color(18, 117, 90);
        Color neutralAction = new Color(55, 68, 92);
        Color dangerAction = new Color(166, 51, 65);
        Color lineColor = new Color(205, 214, 226);
        Color accentColor = new Color(223, 145, 62);

        setTitle("POS App - Checkout" + (currentUser == null ? "" : " | " + currentUser.getUsername() + " (" + currentUser.getRole() + ")"));
        setResizable(true);
        setMinimumSize(new Dimension(980, 780));
        getContentPane().setBackground(appBackground);

        headerPanel.setBackground(chromeBackground);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(14, 22, 35)),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        headerTitleLabel.setForeground(Color.WHITE);
        headerTitleLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 30));
        headerSubtitleLabel.setForeground(new Color(198, 209, 224));
        headerSubtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sessionBadgeLabel.setOpaque(true);
        sessionBadgeLabel.setBackground(new Color(42, 59, 86));
        sessionBadgeLabel.setForeground(new Color(231, 238, 246));
        sessionBadgeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sessionBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(94, 120, 158)),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        sessionBadgeLabel.setText(currentUser == null ? "Guest" : currentUser.getUsername() + " | " + currentUser.getRole());
        sessionBadgeLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));

        styleButtons(new JButton[]{quickLookupButton}, new Color(255, 255, 255), chromeBackground);
        quickLookupButton.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        quickLookupButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(214, 223, 235)),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        jPanel1.setBackground(panelBackground);
        jPanel2.setBackground(panelBackground);
        jPanel3.setBackground(new Color(232, 237, 244));
        jPanel4.setBackground(panelBackground);
        jPanel5.setBackground(panelBackground);
        jPanel6.setBackground(panelBackground);

        jPanel1.setBorder(createSectionBorder("Product Grid", accentColor));
        jPanel2.setBorder(createSectionBorder("Cash Pad", accentColor));
        jPanel3.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(lineColor),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        jPanel5.setBorder(createSectionBorder("Payment", accentColor));
        jPanel6.setBorder(createSectionBorder("Order Summary", accentColor));
        jPanel4.setVisible(false);

        styleTextField(jTxtCash, false);
        styleTextField(jTxtChange, true);
        styleTextField(jTxtSubTotal, true);
        styleTextField(jTxtTax, true);
        styleTextField(jTxtTotal, true);
        styleSummaryLabel(jLabel1);
        styleSummaryLabel(jLabel2);
        styleSummaryLabel(jLabel3);
        styleSummaryLabel(jLabel4);
        styleSummaryLabel(jLabel5);
        styleSummaryLabel(jLabel6);

        jTable1.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        jTable1.setRowHeight(38);
        jTable1.setShowHorizontalLines(true);
        jTable1.setShowVerticalLines(false);
        jTable1.setGridColor(new Color(229, 234, 241));
        jTable1.setSelectionBackground(new Color(219, 236, 255));
        jTable1.setSelectionForeground(new Color(22, 36, 49));
        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.getTableHeader().setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        jTable1.getTableHeader().setBackground(new Color(242, 245, 249));
        jTable1.getTableHeader().setForeground(new Color(35, 44, 56));
        jScrollPane1.getViewport().setBackground(panelBackground);
        jScrollPane1.setBorder(createSectionBorder("Current Order", accentColor));

        styleButtons(
            new JButton[]{jBtnStillWater, jBtnLargePizza, jButton11, jButton13, jButton14, jButton16, jButton17, jButton18,
                jButton19, jButton20, jButton21, jButton22, jButton23, jButton24, jButton25, jButton26, jButton27, jButton28},
            menuButtonBackground,
            new Color(33, 40, 54)
        );
        setUniformButtonSize(
            new JButton[]{jBtnStillWater, jBtnLargePizza, jButton11, jButton13, jButton14, jButton16, jButton17, jButton18,
                jButton19, jButton20, jButton21, jButton22, jButton23, jButton24, jButton25, jButton26, jButton27, jButton28},
            216,
            76
        );
        menuButtons = new JButton[]{jBtnStillWater, jBtnLargePizza, jButton11, jButton13, jButton14, jButton16, jButton17, jButton18,
            jButton19, jButton20, jButton21, jButton22, jButton23, jButton24, jButton25, jButton26, jButton27, jButton28};
        menuProducts = new Product[menuButtons.length];
        rebuildProductGrid();
        loadMenuProducts();

        styleButtons(new JButton[]{jBtn0, jBtn1, jBtn2, jBtn3, jBtn4, jBtn5, jBtn6, jBtn7, jBtn8, jBtn9, jBtnDot},
            keypadBackground,
            new Color(33, 40, 54));
        setUniformButtonSize(new JButton[]{jBtn0, jBtn1, jBtn2, jBtn3, jBtn4, jBtn5, jBtn6, jBtn7, jBtn8, jBtn9, jBtnDot, jBtnC}, 92, 92);
        styleButtons(new JButton[]{jBtnC}, new Color(255, 228, 230), new Color(147, 36, 40));
        keypadButtons = new JButton[]{jBtn7, jBtn8, jBtn9, jBtn4, jBtn5, jBtn6, jBtn1, jBtn2, jBtn3, jBtn0, jBtnDot, jBtnC};
        rebuildCashPad();

        Color actionText = new Color(20, 33, 61);
        styleButtons(new JButton[]{jBtnPay}, new Color(208, 240, 230), new Color(17, 79, 61));
        styleButtons(new JButton[]{jBtnPrint, jBtnReset, jBtnRemove}, new Color(225, 233, 244), actionText);
        styleButtons(new JButton[]{jBtnExit}, new Color(255, 220, 220), new Color(127, 20, 20));
        setUniformButtonSize(new JButton[]{jBtnPay, jBtnPrint, jBtnReset, jBtnRemove, jBtnExit}, 152, 48);

        jBtnPay.setText("Complete Sale");
        jBtnReset.setText("New Order");
        jBtnRemove.setText("Remove Item");
        jBtnPrint.setText("Print Slip");
        jBtnExit.setText("Exit");
        jBtnPay.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));

        jCboPayment.setBackground(Color.WHITE);
        jCboPayment.setForeground(new Color(33, 40, 54));
        jCboPayment.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(196, 206, 221)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        jCboPayment.setFont(new Font("Segoe UI", Font.BOLD, 16));
        rebuildCheckoutPanels();
    }

    private void applyResponsiveLayout()
    {
        int padding = 14;
        int gap = 12;
        int width = getContentPane().getWidth();
        int height = getContentPane().getHeight();
        int headerHeight = 88;

        if (width <= 0 || height <= 0)
        {
            return;
        }

        headerPanel.setBounds(padding, padding, width - (padding * 2), headerHeight);
        headerTitleLabel.setBounds(20, 12, 260, 34);
        headerSubtitleLabel.setBounds(22, 46, 480, 22);
        quickLookupButton.setBounds(headerPanel.getWidth() - 132, 22, 112, 40);
        sessionBadgeLabel.setBounds(headerPanel.getWidth() - 336, 22, 190, 40);

        boolean compactMode = width < 1120;
        boolean mediumMode = width >= 1120 && width < 1480;
        boolean ultraWideMode = width >= 1800;
        if (compactMode)
        {
            int compactPadding = 10;
            int compactGap = 8;
            int contentX = compactPadding;
            int contentWidth = width - (compactPadding * 2);
            int contentTop = headerHeight + (padding * 2);
            int contentHeight = height - contentTop - compactPadding;

            int keypadHeight = Math.max(160, (int) (contentHeight * 0.19));
            int tableHeight = Math.max(170, (int) (contentHeight * 0.23));
            int bottomHeight = Math.max(180, (int) (contentHeight * 0.22));
            int menuHeight = contentHeight - keypadHeight - tableHeight - bottomHeight - (compactGap * 3);

            if (menuHeight < 220)
            {
                int shortage = 220 - menuHeight;
                menuHeight = 220;
                int reduceFromMenu = Math.min(shortage, Math.max(0, tableHeight - 150));
                tableHeight -= reduceFromMenu;
                shortage -= reduceFromMenu;
                if (shortage > 0)
                {
                    int reduceFromKeypad = Math.min(shortage, Math.max(0, keypadHeight - 140));
                    keypadHeight -= reduceFromKeypad;
                    shortage -= reduceFromKeypad;
                }
                if (shortage > 0)
                {
                    int reduceFromBottom = Math.min(shortage, Math.max(0, bottomHeight - 170));
                    bottomHeight -= reduceFromBottom;
                }
            }

            int y = contentTop;
            jPanel2.setBounds(contentX, y, contentWidth, keypadHeight);
            y += keypadHeight + compactGap;
            jScrollPane1.setBounds(contentX, y, contentWidth, tableHeight);
            y += tableHeight + compactGap;
            jPanel1.setBounds(contentX, y, contentWidth, menuHeight);
            y += menuHeight + compactGap;
            jPanel3.setBounds(contentX, y, contentWidth, bottomHeight);

            revalidate();
            repaint();
            return;
        }

        int minLeftWidth = mediumMode ? 500 : (ultraWideMode ? 700 : 520);
        int minMenuWidth = mediumMode ? 320 : (ultraWideMode ? 540 : 360);
        int availableWidth = width - (padding * 2) - gap;
        double leftRatio = mediumMode ? 0.56 : (ultraWideMode ? 0.53 : 0.49);
        int leftWidth = Math.max(minLeftWidth, (int) (availableWidth * leftRatio));
        leftWidth = Math.min(leftWidth, availableWidth - minMenuWidth);
        if (leftWidth < minLeftWidth)
        {
            leftWidth = Math.max(420, availableWidth - minMenuWidth);
        }

        int menuWidth = availableWidth - leftWidth;

        int minBottomHeight = mediumMode ? 240 : (ultraWideMode ? 280 : 260);
        int availableHeight = height - headerHeight - (padding * 3) - gap;
        double topRatio = mediumMode ? 0.58 : (ultraWideMode ? 0.65 : 0.62);
        int topHeight = Math.max(330, (int) (availableHeight * topRatio));
        topHeight = Math.min(topHeight, availableHeight - minBottomHeight);
        int bottomHeight = availableHeight - topHeight;

        int minKeypadWidth = ultraWideMode ? 310 : 250;
        int minTableWidth = ultraWideMode ? 420 : 250;
        int keyAndTableWidth = leftWidth - gap;
        double keypadRatio = mediumMode ? 0.42 : (ultraWideMode ? 0.40 : 0.45);
        int keypadWidth = Math.max(minKeypadWidth, (int) (keyAndTableWidth * keypadRatio));
        keypadWidth = Math.min(keypadWidth, keyAndTableWidth - minTableWidth);
        int tableWidth = keyAndTableWidth - keypadWidth;

        int leftX = padding;
        int rightX = leftX + leftWidth + gap;
        int topY = padding + headerHeight + gap;
        int bottomY = topY + topHeight + gap;

        jPanel2.setBounds(leftX, topY, keypadWidth, topHeight);
        jScrollPane1.setBounds(leftX + keypadWidth + gap, topY, tableWidth, topHeight);
        jPanel3.setBounds(leftX, bottomY, leftWidth, bottomHeight);
        jPanel1.setBounds(rightX, topY, menuWidth, availableHeight);

        revalidate();
        repaint();
    }

    private void styleButtons(JButton[] buttons, Color background, Color foreground)
    {
        for (JButton button : buttons)
        {
            button.setBackground(background);
            button.setForeground(foreground);
            button.setFont(new Font("Segoe UI Semibold", Font.BOLD, 12));
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(true);
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setVerticalAlignment(SwingConstants.CENTER);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(196, 206, 221)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            button.setMargin(new Insets(2, 6, 2, 6));
        }
    }

    private void setUniformButtonSize(JButton[] buttons, int width, int height)
    {
        Dimension size = new Dimension(width, height);
        for (JButton button : buttons)
        {
            button.setPreferredSize(size);
        }
    }

    private void rebuildProductGrid()
    {
        jPanel1.removeAll();
        jPanel1.setLayout(new GridLayout(6, 3, 12, 12));
        jPanel1.setBorder(createSectionBorder("Product Grid", new Color(223, 145, 62)));

        for (JButton button : menuButtons)
        {
            jPanel1.add(button);
        }

        jPanel1.revalidate();
        jPanel1.repaint();
    }

    private void rebuildCashPad()
    {
        jPanel2.removeAll();
        jPanel2.setLayout(new GridLayout(4, 3, 10, 10));
        jPanel2.setBorder(createSectionBorder("Cash Pad", new Color(223, 145, 62)));

        for (JButton button : keypadButtons)
        {
            jPanel2.add(button);
        }

        jPanel2.revalidate();
        jPanel2.repaint();
    }

    private void rebuildCheckoutPanels()
    {
        rebuildTotalsPanel();
        rebuildPaymentPanel();
        rebuildBottomPanel();
    }

    private void rebuildTotalsPanel()
    {
        jPanel6.removeAll();
        jPanel6.setLayout(new GridLayout(3, 2, 10, 10));
        jPanel6.setBorder(createSectionBorder("Order Summary", new Color(223, 145, 62)));

        jPanel6.add(jLabel1);
        jPanel6.add(jTxtSubTotal);
        jPanel6.add(jLabel3);
        jPanel6.add(jTxtTax);
        jPanel6.add(jLabel2);
        jPanel6.add(jTxtTotal);
        jPanel6.revalidate();
        jPanel6.repaint();
    }

    private void rebuildPaymentPanel()
    {
        JPanel paymentFields = new JPanel(new GridLayout(3, 2, 10, 10));
        paymentFields.setOpaque(false);
        paymentFields.add(jLabel4);
        paymentFields.add(jCboPayment);
        paymentFields.add(jLabel5);
        paymentFields.add(jTxtCash);
        paymentFields.add(jLabel6);
        paymentFields.add(jTxtChange);

        JPanel actionButtons = new JPanel(new GridLayout(3, 2, 10, 10));
        actionButtons.setOpaque(false);
        actionButtons.add(jBtnPay);
        actionButtons.add(jBtnReset);
        actionButtons.add(jBtnPrint);
        actionButtons.add(jBtnRemove);
        actionButtons.add(jBtnExit);
        actionButtons.add(new JPanel());

        jPanel5.removeAll();
        jPanel5.setLayout(new GridLayout(1, 2, 16, 16));
        jPanel5.setBorder(createSectionBorder("Payment", new Color(223, 145, 62)));
        jPanel5.add(paymentFields);
        jPanel5.add(actionButtons);
        jPanel5.revalidate();
        jPanel5.repaint();
    }

    private void rebuildBottomPanel()
    {
        jPanel3.removeAll();
        jPanel3.setLayout(new GridLayout(1, 2, 12, 12));
        jPanel3.add(jPanel5);
        jPanel3.add(jPanel6);
        jPanel3.revalidate();
        jPanel3.repaint();
    }

    private void loadMenuProducts()
    {
        if (menuButtons == null)
        {
            return;
        }

        try
        {
            List<Product> products = productRepository.findMenuProducts(menuButtons.length);
            for (int index = 0; index < menuButtons.length; index++)
            {
                JButton button = menuButtons[index];
                if (index < products.size())
                {
                    Product product = products.get(index);
                    menuProducts[index] = product;
                    setMenuButtonLabel(button, product.getName(), MoneyUtils.format(product.getUnitPrice()), product.getCategory(), product.getStockQuantity());
                    applyMenuCategoryStyle(button, product.getCategory());
                    button.setEnabled(product.getStockQuantity() > 0);
                }
                else
                {
                    menuProducts[index] = null;
                    button.setText("Unused Slot");
                    button.setToolTipText("Add a product in Admin > Manage Products");
                    styleMenuButtonCategory(button, new Color(245, 246, 248), new Color(210, 214, 220));
                    button.setEnabled(false);
                }
            }
        }
        catch (SQLException ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to load products from the local database.\n" + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyMenuCategoryStyle(JButton button, String category)
    {
        Color background;
        Color borderColor;

        switch (category)
        {
            case "DRINK":
                background = new Color(228, 243, 255);
                borderColor = new Color(153, 196, 235);
                break;
            case "DESSERT":
                background = new Color(255, 241, 228);
                borderColor = new Color(229, 186, 135);
                break;
            case "SEAFOOD":
                background = new Color(236, 246, 255);
                borderColor = new Color(168, 200, 231);
                break;
            case "MAIN":
            default:
                background = new Color(234, 250, 236);
                borderColor = new Color(161, 211, 164);
                break;
        }

        styleMenuButtonCategory(button, background, borderColor);
    }

    private void setMenuButtonLabel(JButton button, String itemName, String price, String category, int stockQuantity)
    {
        String stockLabel = stockQuantity > 0 ? "Stock: " + stockQuantity : "OUT";
        String label = String.format(
            "<html><div style='text-align:center; line-height:1.35;'><div style='font-size:10px; letter-spacing:0.08em; color:#6b7688;'>%s</div><div style='font-size:13px; font-weight:700; color:#1f2b3a; margin:4px 0;'>%s</div><div style='font-size:12px; font-weight:600; color:#12654f;'>%s</div><div style='font-size:10px; color:#6b7688; margin-top:4px;'>%s</div></div></html>",
            category,
            itemName,
            price,
            stockLabel
        );
        button.setText(label);
        button.setToolTipText("[" + category + "] " + itemName + " - " + price + " - Stock: " + stockQuantity);
    }

    private void styleMenuButtonCategory(JButton button, Color background, Color borderColor)
    {
        button.setBackground(background);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    private void styleTextField(JTextField textField, boolean readOnly)
    {
        textField.setHorizontalAlignment(SwingConstants.RIGHT);
        textField.setFont(new Font("Segoe UI Semibold", Font.BOLD, 17));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(196, 206, 221)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        if (readOnly)
        {
            textField.setBackground(new Color(247, 250, 255));
        }
        else
        {
            textField.setBackground(Color.WHITE);
        }
    }

    private void styleSummaryLabel(JLabel label)
    {
        label.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
        label.setForeground(new Color(48, 60, 79));
    }

    private javax.swing.border.Border createSectionBorder(String title, Color accentColor)
    {
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(205, 214, 226)),
            title
        );
        titledBorder.setTitleFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        titledBorder.setTitleColor(accentColor.darker());
        return BorderFactory.createCompoundBorder(
            titledBorder,
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jBtnLargePizza = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jBtnStillWater = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jButton19 = new javax.swing.JButton();
        jButton20 = new javax.swing.JButton();
        jButton21 = new javax.swing.JButton();
        jButton22 = new javax.swing.JButton();
        jButton23 = new javax.swing.JButton();
        jButton24 = new javax.swing.JButton();
        jButton25 = new javax.swing.JButton();
        jButton26 = new javax.swing.JButton();
        jButton27 = new javax.swing.JButton();
        jButton28 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jBtn2 = new javax.swing.JButton();
        jBtn3 = new javax.swing.JButton();
        jBtn4 = new javax.swing.JButton();
        jBtn1 = new javax.swing.JButton();
        jBtn6 = new javax.swing.JButton();
        jBtn7 = new javax.swing.JButton();
        jBtn8 = new javax.swing.JButton();
        jBtn9 = new javax.swing.JButton();
        jBtn5 = new javax.swing.JButton();
        jBtnC = new javax.swing.JButton();
        jBtnDot = new javax.swing.JButton();
        jBtn0 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTxtSubTotal = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTxtTax = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTxtTotal = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTxtCash = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jTxtChange = new javax.swing.JTextField();
        jCboPayment = new javax.swing.JComboBox<>();
        jBtnPrint = new javax.swing.JButton();
        jBtnRemove = new javax.swing.JButton();
        jBtnReset = new javax.swing.JButton();
        jBtnPay = new javax.swing.JButton();
        jBtnExit = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jBtnLargePizza.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jBtnLargePizza.setText("Large Pizza - R108.50");
        jBtnLargePizza.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnLargePizzaActionPerformed(evt);
            }
        });

        jButton11.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton11.setText("Chocolate Milkshake - R14.50");
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        jButton13.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton13.setText("Orange Juice - 14.50 ");
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });

        jButton14.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton14.setText("Bubblegum Milkshake - R14.50");
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });

        jBtnStillWater.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jBtnStillWater.setText("Still Water - R12.50");
        jBtnStillWater.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnStillWaterActionPerformed(evt);
            }
        });

        jButton16.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton16.setText("Strawberry Milkshake - R14.50");
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });

        jButton17.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton17.setText("Pasta - R40.00");
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });

        jButton18.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton18.setText("Chicken Burger - R45.50");
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });

        jButton19.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton19.setText("Cappuccino - R15.00");
        jButton19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton19ActionPerformed(evt);
            }
        });

        jButton20.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton20.setText("Vanilla Cake - R20.00");
        jButton20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton20ActionPerformed(evt);
            }
        });

        jButton21.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton21.setText("600g Ribs - R60.50");
        jButton21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton21ActionPerformed(evt);
            }
        });

        jButton22.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton22.setText("Coffee - R15.00");
        jButton22.setToolTipText("");
        jButton22.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton22ActionPerformed(evt);
            }
        });

        jButton23.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton23.setText("Red Velvet Cake - R20.00");
        jButton23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton23ActionPerformed(evt);
            }
        });

        jButton24.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton24.setText("Vanilla Milkshake - R14.50");
        jButton24.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton24ActionPerformed(evt);
            }
        });

        jButton25.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton25.setText("Beef Burger - R45.50");
        jButton25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton25ActionPerformed(evt);
            }
        });

        jButton26.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton26.setText("Chocolate Cake - R20.00");
        jButton26.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton26ActionPerformed(evt);
            }
        });

        jButton27.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton27.setText("Hake Fish - R35.50");
        jButton27.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton27ActionPerformed(evt);
            }
        });

        jButton28.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton28.setText("Prawns - R80.00");
        jButton28.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton28ActionPerformed(evt);
            }
        });

        jPanel4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 696, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 256, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton16)
                            .addComponent(jButton24)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jButton21, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton18, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jButton25, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButton19)
                                    .addComponent(jButton26))
                                .addGap(12, 12, 12)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jButton20, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jButton17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGap(15, 15, 15))))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                        .addComponent(jButton22, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(31, 31, 31)
                                        .addComponent(jButton23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                        .addComponent(jButton28, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(27, 27, 27)
                                        .addComponent(jButton27, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jButton13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jBtnStillWater, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jBtnLargePizza)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton11)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(12, 12, 12))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(jBtnStillWater, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jBtnLargePizza, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton11, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton14, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton13, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jButton24, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(27, 27, 27)
                                .addComponent(jButton16, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jButton26, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton18, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton19, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton20, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton21, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton22, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton23, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton25, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton28, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton27, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        getContentPane().add(jPanel1);

        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jBtn2.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jBtn2.setText("2");
        jBtn2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtn2ActionPerformed(evt);
            }
        });

        jBtn3.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jBtn3.setText("3");
        jBtn3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtn3ActionPerformed(evt);
            }
        });

        jBtn4.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jBtn4.setText("4");
        jBtn4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtn4ActionPerformed(evt);
            }
        });

        jBtn1.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jBtn1.setText("1");
        jBtn1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtn1ActionPerformed(evt);
            }
        });

        jBtn6.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jBtn6.setText("6");
        jBtn6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtn6ActionPerformed(evt);
            }
        });

        jBtn7.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jBtn7.setText("7");
        jBtn7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtn7ActionPerformed(evt);
            }
        });

        jBtn8.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jBtn8.setText("8");
        jBtn8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtn8ActionPerformed(evt);
            }
        });

        jBtn9.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jBtn9.setText("9");
        jBtn9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtn9ActionPerformed(evt);
            }
        });

        jBtn5.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jBtn5.setText("5");
        jBtn5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtn5ActionPerformed(evt);
            }
        });

        jBtnC.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jBtnC.setText("C");
        jBtnC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnCActionPerformed(evt);
            }
        });

        jBtnDot.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jBtnDot.setText(".");
        jBtnDot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDotActionPerformed(evt);
            }
        });

        jBtn0.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jBtn0.setText("0");
        jBtn0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtn0ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jBtn1)
                        .addGap(28, 28, 28)
                        .addComponent(jBtn2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jBtn3))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jBtn7)
                                    .addComponent(jBtn4)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jBtn0, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(24, 24, 24)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jBtn5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jBtn6))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jBtn8)
                                .addGap(28, 28, 28)
                                .addComponent(jBtn9))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jBtnDot, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jBtnC, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(85, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBtn1, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtn2, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtn3, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBtn5, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtn6, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtn4, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBtn8, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtn9, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtn7, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jBtnC, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(jBtnDot, javax.swing.GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jBtn0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );

        getContentPane().add(jPanel2);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Item", "Qty", "Amount"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        getContentPane().add(jScrollPane1);

        jPanel3.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jPanel6.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("SubTotal");

        jTxtSubTotal.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Tax");

        jTxtTax.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Total");

        jTxtTotal.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTxtTax, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE)
                            .addComponent(jTxtSubTotal)))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jTxtTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTxtSubTotal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(55, 55, 55)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTxtTax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(44, 44, 44)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTxtTotal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(30, Short.MAX_VALUE))
        );

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("Payment Method");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Cash");

        jTxtCash.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setText("Change");

        jTxtChange.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N

        jCboPayment.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jCboPayment.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Cash", "Visa Card", "Master Card" }));

        jBtnPrint.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jBtnPrint.setText("Print");
        jBtnPrint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnPrintActionPerformed(evt);
            }
        });

        jBtnRemove.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jBtnRemove.setText("Remove");
        jBtnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnRemoveActionPerformed(evt);
            }
        });

        jBtnReset.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jBtnReset.setText("Reset");
        jBtnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnResetActionPerformed(evt);
            }
        });

        jBtnPay.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jBtnPay.setText("Pay");
        jBtnPay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnPayActionPerformed(evt);
            }
        });

        jBtnExit.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jBtnExit.setText("Exit");
        jBtnExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnExitActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6))
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jTxtChange))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGap(3, 3, 3)
                                .addComponent(jTxtCash, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jBtnPrint, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jBtnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jBtnPay, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jBtnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18)
                        .addComponent(jCboPayment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(47, 47, 47)
                        .addComponent(jBtnExit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(28, 28, 28))))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCboPayment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jBtnExit, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(jTxtCash, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jBtnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jBtnPay, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(jBtnPrint, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jBtnRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 52, Short.MAX_VALUE)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jTxtChange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(78, 78, 78))))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel3);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents
    //===================================Function===========================
    
    private void addItemToBill(Product product)
    {
        if (product == null)
        {
            JOptionPane.showMessageDialog(this, "Product is not assigned to this menu button.");
            return;
        }

        int quantityAlreadyInCart = cartService.getQuantityForProduct(product.getName());
        if (quantityAlreadyInCart >= product.getStockQuantity())
        {
            JOptionPane.showMessageDialog(this, "Not enough stock for " + product.getName() + ". Available: " + product.getStockQuantity());
            return;
        }

        cartService.addProduct(product);
        refreshCartTable();
        ItemCost();
    }

    private void addItemFromButton(JButton button)
    {
        if (menuButtons == null || menuProducts == null)
        {
            return;
        }

        for (int index = 0; index < menuButtons.length; index++)
        {
            if (menuButtons[index] == button)
            {
                addItemToBill(menuProducts[index]);
                return;
            }
        }
    }

    private void appendCashInput(String input)
    {
        jTxtCash.setText(jTxtCash.getText() + input);
    }

    private void refreshCartTable()
    {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        for (CartItem item : cartService.getItems())
        {
            model.addRow(new Object[]{
                item.getProduct().getName(),
                item.getQuantity(),
                item.getLineTotal().doubleValue()
            });
        }
    }

    private CartTotals getCartTotals()
    {
        return cartService.calculateTotals();
    }

    private void clearOrder(boolean clearCashAndChange)
    {
        cartService.clear();
        refreshCartTable();
        ItemCost();

        if (clearCashAndChange)
        {
            jTxtCash.setText("");
            jTxtChange.setText("");
            lastCashAmount = BigDecimal.ZERO;
            lastChangeAmount = BigDecimal.ZERO;
        }
    }

    private String buildReceipt(String paymentMethod)
    {
        return receiptService.buildReceipt(
            cartService.getItems(),
            getCartTotals(),
            paymentMethod,
            lastCashAmount,
            lastChangeAmount
        );
    }

    private void completeSale(String paymentMethod)
    {
        if (!sessionManager.requireActiveSession(this, "complete the sale"))
        {
            return;
        }
        try
        {
            saleRepository.saveSale(cartService.getItems(), getCartTotals(), paymentMethod, lastCashAmount, lastChangeAmount);
            loadMenuProducts();
        }
        catch (SQLException ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to save this sale.\n" + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String receipt = buildReceipt(paymentMethod);
        JOptionPane.showMessageDialog(this, receipt, "Receipt", JOptionPane.INFORMATION_MESSAGE);
        clearOrder(true);
    }

    public void ItemCost()
    {
        CartTotals totals = getCartTotals();

        jTxtSubTotal.setText(MoneyUtils.format(totals.getSubtotal()));
        jTxtTax.setText(MoneyUtils.format(totals.getTax()));
        jTxtTotal.setText(MoneyUtils.format(totals.getTotal()));
    }        

    public boolean Change()
    {
        String cashText = jTxtCash.getText().trim();
        if (cashText.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Please enter a cash amount.");
            return false;
        }

        BigDecimal cash;
        try
        {
            cash = new BigDecimal(cashText);
        }
        catch (NumberFormatException ex)
        {
            JOptionPane.showMessageDialog(this, "Please enter a valid cash amount.");
            return false;
        }

        if (cash.compareTo(BigDecimal.ZERO) < 0)
        {
            JOptionPane.showMessageDialog(this, "Cash amount cannot be negative.");
            return false;
        }

        CartTotals totals = getCartTotals();

        if (cash.compareTo(totals.getTotal()) < 0)
        {
            JOptionPane.showMessageDialog(this, "Insufficient cash for this order.");
            return false;
        }

        BigDecimal change = MoneyUtils.scale(cash.subtract(totals.getTotal()));
        lastCashAmount = MoneyUtils.scale(cash);
        lastChangeAmount = change;
        jTxtChange.setText(MoneyUtils.format(change));
        return true;
    }        
    
    private void jBtnPrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnPrintActionPerformed
        MessageFormat header = new MessageFormat("Printing in progress");
        MessageFormat footer = new MessageFormat("Page {0, number, integer}");
        
        try
        {
            jTable1.print(JTable.PrintMode.NORMAL, header,footer);
            
        }
        catch(java.awt.print.PrinterException e)
        {
            System.err.format("No Printer Found", e.getMessage());
            
        }
    }//GEN-LAST:event_jBtnPrintActionPerformed

    private void jBtnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnResetActionPerformed
        clearOrder(true);
    }//GEN-LAST:event_jBtnResetActionPerformed

    private void jBtnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnRemoveActionPerformed
      int RemoveItem = jTable1.getSelectedRow();
      if (RemoveItem >= 0)
      {
          cartService.removeItem(RemoveItem);
          refreshCartTable();
      }
      else
      {
          JOptionPane.showMessageDialog(this, "Select an item to remove.");
          return;
      }
      
      ItemCost();
      
        if (jCboPayment.getSelectedItem().equals("Cash"))
        {
          if (!jTxtCash.getText().trim().isEmpty() && jTable1.getRowCount() > 0)
          {
              Change();
          }
          
      }
      else
      {
          jTxtChange.setText("");
      }
    }//GEN-LAST:event_jBtnRemoveActionPerformed

    private void jBtnPayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnPayActionPerformed
        if (cartService.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Add at least one item before paying.");
            return;
        }
        
        if (jCboPayment.getSelectedItem().equals("Cash"))
        {
            if (Change())
            {
                completeSale("Cash");
            }
        }
        
        else
        
        {
                jTxtChange.setText("");
                jTxtCash.setText("");
                lastCashAmount = BigDecimal.ZERO;
                lastChangeAmount = BigDecimal.ZERO;
                completeSale(jCboPayment.getSelectedItem().toString());
        }
    }//GEN-LAST:event_jBtnPayActionPerformed

    private void jBtn6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn6ActionPerformed
        appendCashInput(jBtn6.getText());
    }//GEN-LAST:event_jBtn6ActionPerformed

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton17ActionPerformed
        addItemFromButton(jButton17);
    }//GEN-LAST:event_jButton17ActionPerformed

    private void jButton19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton19ActionPerformed
        addItemFromButton(jButton19);
    }//GEN-LAST:event_jButton19ActionPerformed

    private void jButton20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton20ActionPerformed
        addItemFromButton(jButton20);
    }//GEN-LAST:event_jButton20ActionPerformed

    private void jBtn1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn1ActionPerformed
        appendCashInput(jBtn1.getText());
    }//GEN-LAST:event_jBtn1ActionPerformed

    private void jBtn2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn2ActionPerformed
        appendCashInput(jBtn2.getText());
    }//GEN-LAST:event_jBtn2ActionPerformed

    private void jBtn3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn3ActionPerformed
        appendCashInput(jBtn3.getText());
    }//GEN-LAST:event_jBtn3ActionPerformed

    private void jBtn4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn4ActionPerformed
        appendCashInput(jBtn4.getText());
    }//GEN-LAST:event_jBtn4ActionPerformed

    private void jBtn5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn5ActionPerformed
        appendCashInput(jBtn5.getText());
    }//GEN-LAST:event_jBtn5ActionPerformed

    private void jBtn7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn7ActionPerformed
        appendCashInput(jBtn7.getText());
    }//GEN-LAST:event_jBtn7ActionPerformed

    private void jBtn8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn8ActionPerformed
        appendCashInput(jBtn8.getText());
    }//GEN-LAST:event_jBtn8ActionPerformed

    private void jBtn9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn9ActionPerformed
        appendCashInput(jBtn9.getText());
    }//GEN-LAST:event_jBtn9ActionPerformed

    private void jBtnDotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDotActionPerformed
        if (!jTxtCash.getText().contains("."))
        {
            jTxtCash.setText(jTxtCash.getText() + jBtnDot.getText());
        }
    }//GEN-LAST:event_jBtnDotActionPerformed

    private void jBtn0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn0ActionPerformed
        appendCashInput(jBtn0.getText());
      
    }//GEN-LAST:event_jBtn0ActionPerformed

    private void jBtnCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnCActionPerformed
        jTxtCash.setText("");
        jTxtChange.setText("");
    }//GEN-LAST:event_jBtnCActionPerformed

    private void jBtnStillWaterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnStillWaterActionPerformed
        addItemFromButton(jBtnStillWater);
    }//GEN-LAST:event_jBtnStillWaterActionPerformed

    private void jBtnLargePizzaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnLargePizzaActionPerformed
        addItemFromButton(jBtnLargePizza);
    }//GEN-LAST:event_jBtnLargePizzaActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        addItemFromButton(jButton11);
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton24ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton24ActionPerformed
        addItemFromButton(jButton24);
    }//GEN-LAST:event_jButton24ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        addItemFromButton(jButton13);
    }//GEN-LAST:event_jButton13ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        addItemFromButton(jButton14);
    }//GEN-LAST:event_jButton14ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
        addItemFromButton(jButton16);
    }//GEN-LAST:event_jButton16ActionPerformed

    private void jButton26ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton26ActionPerformed
        addItemFromButton(jButton26);
    }//GEN-LAST:event_jButton26ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
        addItemFromButton(jButton18);
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jButton21ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton21ActionPerformed
        addItemFromButton(jButton21);
    }//GEN-LAST:event_jButton21ActionPerformed

    private void jButton22ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton22ActionPerformed
        addItemFromButton(jButton22);
    }//GEN-LAST:event_jButton22ActionPerformed

    private void jButton23ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton23ActionPerformed
        addItemFromButton(jButton23);
    }//GEN-LAST:event_jButton23ActionPerformed

    private void jButton25ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton25ActionPerformed
        addItemFromButton(jButton25);
    }//GEN-LAST:event_jButton25ActionPerformed

    private void jButton28ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton28ActionPerformed
        addItemFromButton(jButton28);
    }//GEN-LAST:event_jButton28ActionPerformed

    private void jButton27ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton27ActionPerformed
        addItemFromButton(jButton27);
    }//GEN-LAST:event_jButton27ActionPerformed
    private JFrame frame;
    private void jBtnExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnExitActionPerformed
        frame = new JFrame("Exit");
        
        if (JOptionPane.showConfirmDialog(frame, "Confirm if you want to exit", "Point of Sale",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_NO_OPTION)
                
        
        {
            System.exit(0);
        }
    }//GEN-LAST:event_jBtnExitActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set system look and feel for stable runtime rendering */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(JavaPOS.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(JavaPOS.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(JavaPOS.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JavaPOS.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                long startupStart = System.currentTimeMillis();
                DatabaseManager databaseManager = new DatabaseManager();
                UserRepository userRepository = new UserRepository(databaseManager);
                try
                {
                    System.out.println("JavaPOS startup: initializing database...");
                    databaseManager.initialize();
                    userRepository.seedDefaultsIfEmpty();
                    System.out.println("JavaPOS startup: database ready in " + (System.currentTimeMillis() - startupStart) + " ms");
                }
                catch (SQLException ex)
                {
                    JOptionPane.showMessageDialog(null, "Unable to initialize login data.\n" + ex.getMessage(), "Startup Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                System.out.println("JavaPOS startup: opening login dialog...");
                LoginDialog loginDialog = new LoginDialog(null, userRepository);
                UserAccount user = loginDialog.authenticate();
                if (user == null)
                {
                    return;
                }

                System.out.println("JavaPOS startup: opening main window...");
                new JavaPOS(user, true).setVisible(true);
                System.out.println("JavaPOS startup: main window ready in " + (System.currentTimeMillis() - startupStart) + " ms");
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBtn0;
    private javax.swing.JButton jBtn1;
    private javax.swing.JButton jBtn2;
    private javax.swing.JButton jBtn3;
    private javax.swing.JButton jBtn4;
    private javax.swing.JButton jBtn5;
    private javax.swing.JButton jBtn6;
    private javax.swing.JButton jBtn7;
    private javax.swing.JButton jBtn8;
    private javax.swing.JButton jBtn9;
    private javax.swing.JButton jBtnC;
    private javax.swing.JButton jBtnDot;
    private javax.swing.JButton jBtnExit;
    private javax.swing.JButton jBtnLargePizza;
    private javax.swing.JButton jBtnPay;
    private javax.swing.JButton jBtnPrint;
    private javax.swing.JButton jBtnRemove;
    private javax.swing.JButton jBtnReset;
    private javax.swing.JButton jBtnStillWater;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    private javax.swing.JButton jButton20;
    private javax.swing.JButton jButton21;
    private javax.swing.JButton jButton22;
    private javax.swing.JButton jButton23;
    private javax.swing.JButton jButton24;
    private javax.swing.JButton jButton25;
    private javax.swing.JButton jButton26;
    private javax.swing.JButton jButton27;
    private javax.swing.JButton jButton28;
    private javax.swing.JComboBox<String> jCboPayment;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTxtCash;
    private javax.swing.JTextField jTxtChange;
    private javax.swing.JTextField jTxtSubTotal;
    private javax.swing.JTextField jTxtTax;
    private javax.swing.JTextField jTxtTotal;
    // End of variables declaration//GEN-END:variables
}
