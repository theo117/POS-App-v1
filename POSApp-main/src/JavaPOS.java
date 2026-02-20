
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
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
    private static final double TAX_RATE_PERCENT = 15.0;
    private static final String CURRENCY_PATTERN = "R %.2f";
    private static final DateTimeFormatter RECEIPT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private double lastCashAmount;
    private double lastChangeAmount;

    /**
     * Creates new form JavaPOS
     */
    public JavaPOS() {
        initComponents();
        configureUiTheme();
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
        SwingUtilities.invokeLater(this::applyResponsiveLayout);
    }

    private void configureUiTheme()
    {
        Color appBackground = new Color(244, 247, 252);
        Color panelBackground = new Color(255, 255, 255);
        Color menuButtonBackground = new Color(238, 242, 252);
        Color keypadBackground = new Color(236, 240, 245);
        Color primaryAction = new Color(22, 115, 255);
        Color neutralAction = new Color(59, 72, 89);
        Color dangerAction = new Color(194, 55, 60);

        setTitle("POS App - Checkout");
        setResizable(true);
        setMinimumSize(new Dimension(860, 760));
        getContentPane().setBackground(appBackground);

        jPanel1.setBackground(panelBackground);
        jPanel2.setBackground(panelBackground);
        jPanel3.setBackground(panelBackground);
        jPanel4.setBackground(panelBackground);
        jPanel5.setBackground(panelBackground);
        jPanel6.setBackground(panelBackground);

        jPanel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(208, 216, 228)), "Menu"));
        jPanel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(208, 216, 228)), "Cash Keypad"));
        jPanel3.setBorder(BorderFactory.createLineBorder(new Color(208, 216, 228)));
        jPanel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(208, 216, 228)), "Payment"));
        jPanel6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(208, 216, 228)), "Totals"));

        styleTextField(jTxtCash, false);
        styleTextField(jTxtChange, true);
        styleTextField(jTxtSubTotal, true);
        styleTextField(jTxtTax, true);
        styleTextField(jTxtTotal, true);

        jTable1.setRowHeight(30);
        jTable1.setShowHorizontalLines(true);
        jTable1.setShowVerticalLines(false);
        jTable1.setGridColor(new Color(224, 229, 238));
        jTable1.setSelectionBackground(new Color(216, 234, 255));
        jTable1.setSelectionForeground(new Color(20, 33, 61));
        jTable1.getTableHeader().setReorderingAllowed(false);
        jTable1.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        jTable1.getTableHeader().setBackground(new Color(231, 238, 248));
        jScrollPane1.getViewport().setBackground(panelBackground);
        jScrollPane1.setBorder(BorderFactory.createLineBorder(new Color(208, 216, 228)));

        styleButtons(
            new JButton[]{jBtnStillWater, jBtnLargePizza, jButton11, jButton13, jButton14, jButton16, jButton17, jButton18,
                jButton19, jButton20, jButton21, jButton22, jButton23, jButton24, jButton25, jButton26, jButton27, jButton28},
            menuButtonBackground,
            new Color(33, 40, 54)
        );
        setUniformButtonSize(
            new JButton[]{jBtnStillWater, jBtnLargePizza, jButton11, jButton13, jButton14, jButton16, jButton17, jButton18,
                jButton19, jButton20, jButton21, jButton22, jButton23, jButton24, jButton25, jButton26, jButton27, jButton28},
            198,
            70
        );
        applyMenuButtonLabels();
        applyMenuCategoryColors();

        styleButtons(new JButton[]{jBtn0, jBtn1, jBtn2, jBtn3, jBtn4, jBtn5, jBtn6, jBtn7, jBtn8, jBtn9, jBtnDot},
            keypadBackground,
            new Color(33, 40, 54));
        setUniformButtonSize(new JButton[]{jBtn0, jBtn1, jBtn2, jBtn3, jBtn4, jBtn5, jBtn6, jBtn7, jBtn8, jBtn9, jBtnDot, jBtnC}, 88, 88);
        styleButtons(new JButton[]{jBtnC}, new Color(255, 228, 230), new Color(147, 36, 40));

        Color actionText = new Color(20, 33, 61);
        styleButtons(new JButton[]{jBtnPay}, new Color(201, 226, 255), actionText);
        styleButtons(new JButton[]{jBtnPrint, jBtnReset, jBtnRemove}, new Color(225, 233, 244), actionText);
        styleButtons(new JButton[]{jBtnExit}, new Color(255, 220, 220), new Color(127, 20, 20));
        setUniformButtonSize(new JButton[]{jBtnPay, jBtnPrint, jBtnReset, jBtnRemove, jBtnExit}, 148, 44);

        jBtnPay.setText("Complete Sale");
        jBtnReset.setText("Clear Order");
        jBtnRemove.setText("Remove Item");
        jBtnPrint.setText("Print Bill");
        jBtnExit.setText("Exit");

        jCboPayment.setBackground(Color.WHITE);
        jCboPayment.setForeground(new Color(33, 40, 54));
        jCboPayment.setBorder(BorderFactory.createLineBorder(new Color(196, 206, 221)));
    }

    private void applyResponsiveLayout()
    {
        int padding = 12;
        int gap = 10;
        int width = getContentPane().getWidth();
        int height = getContentPane().getHeight();

        if (width <= 0 || height <= 0)
        {
            return;
        }

        boolean compactMode = width < 1120;
        boolean mediumMode = width >= 1120 && width < 1480;
        boolean ultraWideMode = width >= 1800;
        if (compactMode)
        {
            int compactPadding = 10;
            int compactGap = 8;
            int contentX = compactPadding;
            int contentWidth = width - (compactPadding * 2);
            int contentHeight = height - (compactPadding * 2);

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

            int y = compactPadding;
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
        int availableHeight = height - (padding * 2) - gap;
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
        int topY = padding;
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
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(true);
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setVerticalAlignment(SwingConstants.CENTER);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(196, 206, 221)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
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

    private void applyMenuButtonLabels()
    {
        setMenuButtonLabel(jBtnStillWater, "Still Water", "R12.50", "DRINK");
        setMenuButtonLabel(jBtnLargePizza, "Large Pizza", "R108.50", "MAIN");
        setMenuButtonLabel(jButton11, "Chocolate Shake", "R14.50", "DRINK");
        setMenuButtonLabel(jButton13, "Orange Juice", "R14.50", "DRINK");
        setMenuButtonLabel(jButton14, "Bubblegum Shake", "R14.50", "DRINK");
        setMenuButtonLabel(jButton16, "Strawberry Shake", "R14.50", "DRINK");
        setMenuButtonLabel(jButton17, "Pasta", "R40.00", "MAIN");
        setMenuButtonLabel(jButton18, "Chicken Burger", "R45.50", "MAIN");
        setMenuButtonLabel(jButton19, "Cappuccino", "R15.00", "DRINK");
        setMenuButtonLabel(jButton20, "Vanilla Cake", "R20.00", "DESSERT");
        setMenuButtonLabel(jButton21, "600g Ribs", "R60.50", "MAIN");
        setMenuButtonLabel(jButton22, "Coffee", "R15.00", "DRINK");
        setMenuButtonLabel(jButton23, "Red Velvet Cake", "R20.00", "DESSERT");
        setMenuButtonLabel(jButton24, "Vanilla Shake", "R14.50", "DRINK");
        setMenuButtonLabel(jButton25, "Beef Burger", "R45.50", "MAIN");
        setMenuButtonLabel(jButton26, "Chocolate Cake", "R20.00", "DESSERT");
        setMenuButtonLabel(jButton27, "Hake Fish", "R35.50", "SEAFOOD");
        setMenuButtonLabel(jButton28, "Prawns", "R80.00", "SEAFOOD");
    }

    private void applyMenuCategoryColors()
    {
        Color drinksBg = new Color(228, 243, 255);
        Color drinksBorder = new Color(153, 196, 235);
        Color mainsBg = new Color(234, 250, 236);
        Color mainsBorder = new Color(161, 211, 164);
        Color dessertsBg = new Color(255, 241, 228);
        Color dessertsBorder = new Color(229, 186, 135);
        Color seafoodBg = new Color(236, 246, 255);
        Color seafoodBorder = new Color(168, 200, 231);

        styleMenuButtonCategory(jBtnStillWater, drinksBg, drinksBorder);
        styleMenuButtonCategory(jButton11, drinksBg, drinksBorder);
        styleMenuButtonCategory(jButton13, drinksBg, drinksBorder);
        styleMenuButtonCategory(jButton14, drinksBg, drinksBorder);
        styleMenuButtonCategory(jButton16, drinksBg, drinksBorder);
        styleMenuButtonCategory(jButton19, drinksBg, drinksBorder);
        styleMenuButtonCategory(jButton22, drinksBg, drinksBorder);
        styleMenuButtonCategory(jButton24, drinksBg, drinksBorder);

        styleMenuButtonCategory(jBtnLargePizza, mainsBg, mainsBorder);
        styleMenuButtonCategory(jButton17, mainsBg, mainsBorder);
        styleMenuButtonCategory(jButton18, mainsBg, mainsBorder);
        styleMenuButtonCategory(jButton21, mainsBg, mainsBorder);
        styleMenuButtonCategory(jButton25, mainsBg, mainsBorder);

        styleMenuButtonCategory(jButton20, dessertsBg, dessertsBorder);
        styleMenuButtonCategory(jButton23, dessertsBg, dessertsBorder);
        styleMenuButtonCategory(jButton26, dessertsBg, dessertsBorder);

        styleMenuButtonCategory(jButton27, seafoodBg, seafoodBorder);
        styleMenuButtonCategory(jButton28, seafoodBg, seafoodBorder);
    }

    private void setMenuButtonLabel(JButton button, String itemName, String price, String category)
    {
        String label = String.format("[%s] %s - %s", category, itemName, price);
        button.setText(label);
        button.setToolTipText("[" + category + "] " + itemName + " - " + price);
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
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(196, 206, 221)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
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
    
    private void addItemToBill(String itemName, double price)
    {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        for (int row = 0; row < model.getRowCount(); row++)
        {
            Object currentItem = model.getValueAt(row, 0);
            if (itemName.equals(currentItem))
            {
                int quantity = Integer.parseInt(model.getValueAt(row, 1).toString());
                double amount = Double.parseDouble(model.getValueAt(row, 2).toString());
                model.setValueAt(quantity + 1, row, 1);
                model.setValueAt(amount + price, row, 2);
                ItemCost();
                return;
            }
        }

        model.addRow(new Object[]{itemName, 1, price});
        ItemCost();
    }

    private void appendCashInput(String input)
    {
        jTxtCash.setText(jTxtCash.getText() + input);
    }

    private double getSubTotalAmount()
    {
        double sum = 0;

        for (int i = 0; i < jTable1.getRowCount(); i++)
        {
            sum += Double.parseDouble(jTable1.getValueAt(i, 2).toString());
        }

        return sum;
    }

    private String formatCurrency(double amount)
    {
        return String.format(CURRENCY_PATTERN, amount);
    }

    private void clearOrder(boolean clearCashAndChange)
    {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        ItemCost();

        if (clearCashAndChange)
        {
            jTxtCash.setText("");
            jTxtChange.setText("");
        }
    }

    private String buildReceipt(String paymentMethod)
    {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        double subTotal = getSubTotalAmount();
        double tax = (subTotal * TAX_RATE_PERCENT) / 100;
        double total = subTotal + tax;
        StringBuilder receipt = new StringBuilder();

        receipt.append("POS RECEIPT\n");
        receipt.append("Date: ").append(LocalDateTime.now().format(RECEIPT_TIME_FORMAT)).append('\n');
        receipt.append("Payment: ").append(paymentMethod).append("\n\n");
        receipt.append("Items:\n");

        for (int row = 0; row < model.getRowCount(); row++)
        {
            String item = model.getValueAt(row, 0).toString();
            int qty = Integer.parseInt(model.getValueAt(row, 1).toString());
            double amount = Double.parseDouble(model.getValueAt(row, 2).toString());
            receipt.append(String.format("- %s x%d  %s%n", item, qty, formatCurrency(amount)));
        }

        receipt.append('\n');
        receipt.append("Subtotal: ").append(formatCurrency(subTotal)).append('\n');
        receipt.append("Tax: ").append(formatCurrency(tax)).append('\n');
        receipt.append("Total: ").append(formatCurrency(total)).append('\n');

        if ("Cash".equals(paymentMethod))
        {
            receipt.append("Cash: ").append(formatCurrency(lastCashAmount)).append('\n');
            receipt.append("Change: ").append(formatCurrency(lastChangeAmount)).append('\n');
        }

        return receipt.toString();
    }

    private void completeSale(String paymentMethod)
    {
        String receipt = buildReceipt(paymentMethod);
        JOptionPane.showMessageDialog(this, receipt, "Receipt", JOptionPane.INFORMATION_MESSAGE);
        clearOrder(true);
    }

    public void ItemCost()
    {
        double subTotal = getSubTotalAmount();
        double tax = (subTotal * TAX_RATE_PERCENT) / 100;
        double total = subTotal + tax;

        jTxtSubTotal.setText(formatCurrency(subTotal));
        jTxtTax.setText(formatCurrency(tax));
        jTxtTotal.setText(formatCurrency(total));
    }        

    public boolean Change()
    {
        String cashText = jTxtCash.getText().trim();
        if (cashText.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Please enter a cash amount.");
            return false;
        }

        double cash;
        try
        {
            cash = Double.parseDouble(cashText);
        }
        catch (NumberFormatException ex)
        {
            JOptionPane.showMessageDialog(this, "Please enter a valid cash amount.");
            return false;
        }

        if (cash < 0)
        {
            JOptionPane.showMessageDialog(this, "Cash amount cannot be negative.");
            return false;
        }

        double subTotal = getSubTotalAmount();
        double tax = (subTotal * TAX_RATE_PERCENT) / 100;
        double totalDue = subTotal + tax;

        if (cash < totalDue)
        {
            JOptionPane.showMessageDialog(this, "Insufficient cash for this order.");
            return false;
        }

        double change = cash - totalDue;
        lastCashAmount = cash;
        lastChangeAmount = change;
        jTxtChange.setText(formatCurrency(change));
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
      DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
      int RemoveItem = jTable1.getSelectedRow();
      if (RemoveItem >= 0)
      {
          model.removeRow(RemoveItem);
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
        if (jTable1.getRowCount() == 0)
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
                lastCashAmount = 0;
                lastChangeAmount = 0;
                completeSale(jCboPayment.getSelectedItem().toString());
        }
    }//GEN-LAST:event_jBtnPayActionPerformed

    private void jBtn6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtn6ActionPerformed
        appendCashInput(jBtn6.getText());
    }//GEN-LAST:event_jBtn6ActionPerformed

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton17ActionPerformed
        addItemToBill("Pasta", 40.0);
    }//GEN-LAST:event_jButton17ActionPerformed

    private void jButton19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton19ActionPerformed
        addItemToBill("Cappuccino", 15.0);
    }//GEN-LAST:event_jButton19ActionPerformed

    private void jButton20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton20ActionPerformed
        addItemToBill("Vanilla Cake", 20.0);
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
        addItemToBill("Still Water", 12.50);
    }//GEN-LAST:event_jBtnStillWaterActionPerformed

    private void jBtnLargePizzaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnLargePizzaActionPerformed
        addItemToBill("Large Pizza", 108.50);
    }//GEN-LAST:event_jBtnLargePizzaActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        addItemToBill("Chocolate Milkshake", 14.50);
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton24ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton24ActionPerformed
        addItemToBill("Vanilla Milkshake", 14.50);
    }//GEN-LAST:event_jButton24ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton13ActionPerformed
        addItemToBill("Orange Juice", 14.50);
    }//GEN-LAST:event_jButton13ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton14ActionPerformed
        addItemToBill("Bubblegum Milkshake", 14.50);
    }//GEN-LAST:event_jButton14ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton16ActionPerformed
        addItemToBill("Stawberry Milkshake ", 14.50);
    }//GEN-LAST:event_jButton16ActionPerformed

    private void jButton26ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton26ActionPerformed
        addItemToBill("Chocolate Cake", 20.00);
    }//GEN-LAST:event_jButton26ActionPerformed

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton18ActionPerformed
        addItemToBill("Chicken Burger", 45.50);
    }//GEN-LAST:event_jButton18ActionPerformed

    private void jButton21ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton21ActionPerformed
        addItemToBill("Ribs", 60.50);
    }//GEN-LAST:event_jButton21ActionPerformed

    private void jButton22ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton22ActionPerformed
        addItemToBill("Coffee", 15.0);
    }//GEN-LAST:event_jButton22ActionPerformed

    private void jButton23ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton23ActionPerformed
        addItemToBill("Red Velvet Cake", 20.0);
    }//GEN-LAST:event_jButton23ActionPerformed

    private void jButton25ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton25ActionPerformed
        addItemToBill("Beef Burger", 45.50);
    }//GEN-LAST:event_jButton25ActionPerformed

    private void jButton28ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton28ActionPerformed
        addItemToBill("Prawns", 80.0);
    }//GEN-LAST:event_jButton28ActionPerformed

    private void jButton27ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton27ActionPerformed
        addItemToBill("Hake Fish", 35.50);
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
                new JavaPOS().setVisible(true);
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
