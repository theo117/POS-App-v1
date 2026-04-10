const TAX_RATE = 0.15;
const currency = new Intl.NumberFormat("en-ZA", {
    style: "currency",
    currency: "ZAR",
    minimumFractionDigits: 2
});

const state = {
    authToken: localStorage.getItem("javapos.token") || "",
    currentUser: null,
    products: [],
    allProducts: [],
    categories: [],
    cart: [],
    paymentMethod: "Cash",
    cashInput: "",
    dashboard: null,
    selectedCategory: "ALL",
    lastReceipt: "No sale completed yet.",
    lastSaleId: null
};

const elements = {
    refreshButton: document.getElementById("refreshButton"),
    adminButton: document.getElementById("adminButton"),
    closeoutButton: document.getElementById("closeoutButton"),
    logoutButton: document.getElementById("logoutButton"),
    lastUpdated: document.getElementById("lastUpdated"),
    todayTotal: document.getElementById("todayTotal"),
    transactionCount: document.getElementById("transactionCount"),
    cartTotalHero: document.getElementById("cartTotalHero"),
    cartCountHero: document.getElementById("cartCountHero"),
    operatorBadge: document.getElementById("operatorBadge"),
    loginOverlay: document.getElementById("loginOverlay"),
    loginUsername: document.getElementById("loginUsername"),
    loginPassword: document.getElementById("loginPassword"),
    loginNewPassword: document.getElementById("loginNewPassword"),
    newPasswordWrap: document.getElementById("newPasswordWrap"),
    loginButton: document.getElementById("loginButton"),
    loginMessage: document.getElementById("loginMessage"),
    metricsGrid: document.getElementById("metricsGrid"),
    categoryChips: document.getElementById("categoryChips"),
    productModeBanner: document.getElementById("productModeBanner"),
    fastMovers: document.getElementById("fastMovers"),
    barcodeInput: document.getElementById("barcodeInput"),
    productGrid: document.getElementById("productGrid"),
    lowStockList: document.getElementById("lowStockList"),
    paymentMix: document.getElementById("paymentMix"),
    topProductsList: document.getElementById("topProductsList"),
    recentSales: document.getElementById("recentSales"),
    receiptOutput: document.getElementById("receiptOutput"),
    checkoutNotice: document.getElementById("checkoutNotice"),
    printReceiptButton: document.getElementById("printReceiptButton"),
    saleModal: document.getElementById("saleModal"),
    saleModalTitle: document.getElementById("saleModalTitle"),
    saleModalText: document.getElementById("saleModalText"),
    modalPrintButton: document.getElementById("modalPrintButton"),
    modalCloseButton: document.getElementById("modalCloseButton"),
    closeoutModal: document.getElementById("closeoutModal"),
    closeoutSummary: document.getElementById("closeoutSummary"),
    closeoutPayments: document.getElementById("closeoutPayments"),
    closeoutCloseButton: document.getElementById("closeoutCloseButton"),
    adminModal: document.getElementById("adminModal"),
    adminProductId: document.getElementById("adminProductId"),
    adminProductName: document.getElementById("adminProductName"),
    adminProductCategory: document.getElementById("adminProductCategory"),
    adminProductPrice: document.getElementById("adminProductPrice"),
    adminProductOrder: document.getElementById("adminProductOrder"),
    adminProductBarcode: document.getElementById("adminProductBarcode"),
    adminProductStock: document.getElementById("adminProductStock"),
    adminProductActive: document.getElementById("adminProductActive"),
    adminSaveProductButton: document.getElementById("adminSaveProductButton"),
    adminAdjustProductId: document.getElementById("adminAdjustProductId"),
    adminAdjustDelta: document.getElementById("adminAdjustDelta"),
    adminAdjustType: document.getElementById("adminAdjustType"),
    adminAdjustNote: document.getElementById("adminAdjustNote"),
    adminAdjustInventoryButton: document.getElementById("adminAdjustInventoryButton"),
    adminProductsTable: document.getElementById("adminProductsTable"),
    adminMovementsTable: document.getElementById("adminMovementsTable"),
    adminRuntimeInfo: document.getElementById("adminRuntimeInfo"),
    adminBackupButton: document.getElementById("adminBackupButton"),
    adminMessage: document.getElementById("adminMessage"),
    adminCloseButton: document.getElementById("adminCloseButton"),
    productSearch: document.getElementById("productSearch"),
    cartItems: document.getElementById("cartItems"),
    paymentMethod: document.getElementById("paymentMethod"),
    subtotalValue: document.getElementById("subtotalValue"),
    taxValue: document.getElementById("taxValue"),
    totalValue: document.getElementById("totalValue"),
    cashValue: document.getElementById("cashValue"),
    changeValue: document.getElementById("changeValue"),
    cashDisplay: document.getElementById("cashDisplay"),
    keypad: document.getElementById("keypad"),
    clearCartButton: document.getElementById("clearCartButton"),
    cashClearButton: document.getElementById("cashClearButton"),
    backspaceButton: document.getElementById("backspaceButton"),
    checkoutButton: document.getElementById("checkoutButton"),
    newSaleButton: document.getElementById("newSaleButton")
};

let searchTimer = null;

async function fetchJson(path) {
    const response = await fetch(path, {
        cache: "no-store",
        headers: authHeaders()
    });
    const payload = await response.json();
    if (!response.ok) {
        if (response.status === 401) {
            handleUnauthorized();
        }
        throw new Error(payload.error || `Request failed for ${path}`);
    }
    return payload;
}

async function postForm(path, formData) {
    const response = await fetch(path, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            ...authHeaders()
        },
        body: new URLSearchParams(formData).toString()
    });
    const payload = await response.json();
    if (!response.ok) {
        if (response.status === 401) {
            handleUnauthorized();
        }
        throw new Error(payload.error || `Request failed for ${path}`);
    }
    return payload;
}

function authHeaders() {
    return state.authToken ? { "X-Session-Token": state.authToken } : {};
}

function money(value) {
    return currency.format(Number(value || 0));
}

function numberValue(value) {
    return Number(value || 0);
}

function renderEmpty(container, message = "No data to show yet.") {
    container.innerHTML = `<div class="empty-state">${message}</div>`;
}

function getCartQuantity() {
    return state.cart.reduce((sum, item) => sum + item.quantity, 0);
}

function getTotals() {
    const subtotal = state.cart.reduce((sum, item) => sum + numberValue(item.price) * item.quantity, 0);
    const tax = Number((subtotal * TAX_RATE).toFixed(2));
    const total = Number((subtotal + tax).toFixed(2));
    const cash = state.paymentMethod === "Cash" ? Number(state.cashInput || 0) : 0;
    const change = state.paymentMethod === "Cash" ? Math.max(0, Number((cash - total).toFixed(2))) : 0;
    return { subtotal, tax, total, cash, change };
}

function renderMetrics(dashboard) {
    const cards = [
        {
            label: "Today total",
            value: money(dashboard.today.total),
            note: `${dashboard.today.transactionCount} transactions`
        },
        {
            label: "Catalog size",
            value: dashboard.store.productCount,
            note: `${dashboard.store.activeProductCount} active products`
        },
        {
            label: "Categories",
            value: dashboard.store.categoryCount,
            note: `${dashboard.store.stockUnits} stock units on hand`
        },
        {
            label: "Low stock",
            value: dashboard.lowStock.length,
            note: "Items at or below threshold"
        }
    ];

    elements.metricsGrid.innerHTML = cards.map(card => `
        <article class="metric-card">
            <span class="metric-label">${card.label}</span>
            <strong class="metric-value">${card.value}</strong>
            <span class="metric-note">${card.note}</span>
        </article>
    `).join("");
}

function renderCategories(categories) {
    const options = ["ALL"].concat(categories);
    elements.categoryChips.innerHTML = options.map(category => `
        <button class="chip ${state.selectedCategory === category ? "chip-active" : ""}" data-category="${category}">
            ${category === "ALL" ? "All Products" : category}
        </button>
    `).join("");
}

function renderProducts(products) {
    if (!products.length) {
        renderEmpty(elements.productGrid, "No products match that search.");
        return;
    }

    elements.productGrid.innerHTML = products.map(product => {
        const stockClass = product.stockQuantity <= 10 ? "stock-low" : "stock-ok";
        const stockLabel = product.stockQuantity <= 10 ? "Low stock" : "In stock";
        const disabled = product.stockQuantity <= 0 ? "disabled" : "";
        return `
            <button class="product-card" data-product-id="${product.id}" ${disabled}>
                <div class="product-meta">
                    <span class="chip">${product.category}</span>
                    <span>${product.barcode || "No barcode"}</span>
                </div>
                <h3>${product.name}</h3>
                <strong class="price-tag">${money(product.price)}</strong>
                <div class="product-meta">
                    <span class="stock-tag ${stockClass}">${stockLabel}: ${product.stockQuantity}</span>
                    <span>#${product.displayOrder}</span>
                </div>
            </button>
        `;
    }).join("");
}

function renderFastMovers(dashboard) {
    const byName = new Map(dashboard.topProducts.map(product => [product.productName, product]));
    const fastMovers = dashboard.topProducts
        .map(summary => state.allProducts.find(product => product.name === summary.productName))
        .filter(Boolean);

    if (!fastMovers.length) {
        renderEmpty(elements.fastMovers, "Fast movers will appear after sales come in.");
        return;
    }

    elements.fastMovers.innerHTML = fastMovers.map(product => {
        const summary = byName.get(product.name);
        return `
            <button class="fast-mover-card" data-fast-product-id="${product.id}">
                <span class="chip">${product.category}</span>
                <strong>${product.name}</strong>
                <span>${money(product.price)}</span>
                <small>${summary.quantitySold} sold today</small>
            </button>
        `;
    }).join("");
}

function renderKeyValueList(container, rows, mapFn) {
    if (!rows.length) {
        renderEmpty(container);
        return;
    }
    container.innerHTML = rows.map(mapFn).join("");
}

function renderLowStock(products) {
    renderKeyValueList(elements.lowStockList, products, product => `
        <div class="list-item">
            <div>
                <strong>${product.name}</strong>
                <span>${product.category} / ${product.barcode || "No barcode"}</span>
            </div>
            <div>
                <strong>${product.stockQuantity}</strong>
                <span>units left</span>
            </div>
        </div>
    `);
}

function renderPayments(payments) {
    renderKeyValueList(elements.paymentMix, payments, payment => `
        <div class="list-item">
            <div>
                <strong>${payment.paymentMethod}</strong>
                <span>${payment.transactionCount} transactions</span>
            </div>
            <div>
                <strong>${money(payment.totalAmount)}</strong>
                <span>today</span>
            </div>
        </div>
    `);
}

function renderTopProducts(products) {
    renderKeyValueList(elements.topProductsList, products, product => `
        <div class="list-item">
            <div>
                <strong>${product.productName}</strong>
                <span>${product.quantitySold} sold</span>
            </div>
            <div>
                <strong>${money(product.revenue)}</strong>
                <span>revenue</span>
            </div>
        </div>
    `);
}

function renderRecentSales(sales) {
    if (!sales.length) {
        renderEmpty(elements.recentSales);
        return;
    }

    elements.recentSales.innerHTML = sales.map(sale => {
        const statusClass = `status-${String(sale.status || "").toLowerCase()}`;
        const refundButton = sale.status === "COMPLETED" && state.currentUser && state.currentUser.admin
            ? `<button class="ghost-action sale-refund-button" data-refund-sale-id="${sale.id}">Refund</button>`
            : `<span class="panel-note">Closed</span>`;
        return `
            <div class="sale-row">
                <div>
                    <strong>Sale #${sale.id}</strong>
                    <span>${new Date(sale.createdAt).toLocaleString()}</span>
                </div>
                <div>
                    <strong>${money(sale.total)}</strong>
                    <span>${sale.paymentMethod}</span>
                </div>
                <div>
                    <span class="status-badge ${statusClass}">${sale.status}</span>
                </div>
                <div>
                    <strong>${money(sale.tax)}</strong>
                    <span>tax</span>
                </div>
                <div>
                    <button class="ghost-action sale-reprint-button" data-sale-id="${sale.id}">Reprint</button>
                </div>
                <div>
                    ${refundButton}
                </div>
            </div>
        `;
    }).join("");
}

function renderCart() {
    if (!state.cart.length) {
        renderEmpty(elements.cartItems, "Add products to start the sale.");
    } else {
        elements.cartItems.innerHTML = state.cart.map(item => `
            <div class="cart-row">
                <div class="cart-row-copy">
                    <strong>${item.name}</strong>
                    <span>${money(item.price)} each</span>
                </div>
                <div class="cart-row-controls">
                    <button class="qty-button" data-action="decrease" data-product-id="${item.id}">-</button>
                    <span class="qty-value">${item.quantity}</span>
                    <button class="qty-button" data-action="increase" data-product-id="${item.id}">+</button>
                    <button class="remove-button" data-action="remove" data-product-id="${item.id}">Remove</button>
                </div>
                <strong>${money(numberValue(item.price) * item.quantity)}</strong>
            </div>
        `).join("");
    }

    const totals = getTotals();
    elements.subtotalValue.textContent = money(totals.subtotal);
    elements.taxValue.textContent = money(totals.tax);
    elements.totalValue.textContent = money(totals.total);
    elements.cashValue.textContent = money(totals.cash);
    elements.changeValue.textContent = money(totals.change);
    elements.cashDisplay.textContent = state.cashInput || "0";
    elements.cartTotalHero.textContent = money(totals.total);
    elements.cartCountHero.textContent = state.cart.length ? `${getCartQuantity()} items in cart` : "Cart empty";
}

function updateProductModeBanner() {
    const search = elements.productSearch.value.trim();
    const categoryText = state.selectedCategory === "ALL" ? "all products" : state.selectedCategory;
    const searchText = search ? ` matching "${search}"` : "";
    elements.productModeBanner.textContent = `Showing ${categoryText}${searchText}`;
}

function showCheckoutNotice(message, type) {
    elements.checkoutNotice.textContent = message;
    elements.checkoutNotice.className = `checkout-notice checkout-notice-${type}`;
}

function showLoginMessage(message, type) {
    elements.loginMessage.textContent = message;
    elements.loginMessage.className = `checkout-notice checkout-notice-${type}`;
}

function hideLoginMessage() {
    elements.loginMessage.className = "checkout-notice hidden";
}

function openSaleModal(title, text) {
    elements.saleModalTitle.textContent = title;
    elements.saleModalText.textContent = text;
    elements.saleModal.className = "modal-shell";
}

function closeSaleModal() {
    elements.saleModal.className = "modal-shell hidden";
}

function openCloseoutModal() {
    elements.closeoutModal.className = "modal-shell";
}

function closeCloseoutModal() {
    elements.closeoutModal.className = "modal-shell hidden";
}

function openAdminModal() {
    elements.adminModal.className = "modal-shell";
}

function closeAdminModal() {
    elements.adminModal.className = "modal-shell hidden";
}

function showLoginOverlay() {
    elements.loginOverlay.className = "login-overlay";
}

function hideLoginOverlay() {
    elements.loginOverlay.className = "login-overlay hidden";
}

function updateAuthUi() {
    const user = state.currentUser;
    elements.operatorBadge.textContent = user ? `${user.username} / ${user.role}` : "Signed out";
    elements.adminButton.style.display = user && user.admin ? "inline-flex" : "none";
    elements.closeoutButton.style.display = user && user.admin ? "inline-flex" : "none";
}

function printReceipt() {
    if (!state.lastReceipt || state.lastReceipt === "No sale completed yet.") {
        throw new Error("No receipt is available to print.");
    }

    const printWindow = window.open("", "_blank", "width=480,height=720");
    if (!printWindow) {
        throw new Error("Unable to open the print window.");
    }

    printWindow.document.write(`
        <html>
            <head>
                <title>JavaPOS Receipt</title>
                <style>
                    body { font-family: "Courier New", monospace; padding: 24px; white-space: pre-wrap; }
                </style>
            </head>
            <body>${state.lastReceipt.replace(/</g, "&lt;").replace(/>/g, "&gt;")}</body>
        </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
}

function renderCloseout(closeout) {
    elements.closeoutSummary.innerHTML = `
        <div class="metric-card">
            <span class="metric-label">Transactions</span>
            <strong class="metric-value">${closeout.summary.transactionCount}</strong>
            <span class="metric-note">${closeout.date}</span>
        </div>
        <div class="metric-card">
            <span class="metric-label">Subtotal</span>
            <strong class="metric-value">${money(closeout.summary.subtotal)}</strong>
            <span class="metric-note">Before tax</span>
        </div>
        <div class="metric-card">
            <span class="metric-label">Tax</span>
            <strong class="metric-value">${money(closeout.summary.tax)}</strong>
            <span class="metric-note">Today</span>
        </div>
        <div class="metric-card">
            <span class="metric-label">Total</span>
            <strong class="metric-value">${money(closeout.summary.total)}</strong>
            <span class="metric-note">Net sales</span>
        </div>
    `;

    renderKeyValueList(elements.closeoutPayments, closeout.payments, payment => `
        <div class="list-item">
            <div>
                <strong>${payment.paymentMethod}</strong>
                <span>${payment.transactionCount} transactions</span>
            </div>
            <div>
                <strong>${money(payment.totalAmount)}</strong>
                <span>today</span>
            </div>
        </div>
    `);
}

function showAdminMessage(message, type) {
    elements.adminMessage.textContent = message;
    elements.adminMessage.className = `checkout-notice checkout-notice-${type}`;
}

function renderAdminProducts(products) {
    if (!products.length) {
        renderEmpty(elements.adminProductsTable, "No products found.");
        return;
    }

    elements.adminProductsTable.innerHTML = products.map(product => `
        <button class="admin-row" data-admin-product-id="${product.id}">
            <strong>#${product.id} ${product.name}</strong>
            <span>${product.category} / ${money(product.price)} / stock ${product.stockQuantity}</span>
        </button>
    `).join("");
}

function renderAdminMovements(movements) {
    if (!movements.length) {
        renderEmpty(elements.adminMovementsTable, "No stock movements found.");
        return;
    }

    elements.adminMovementsTable.innerHTML = movements.map(movement => `
        <div class="admin-row admin-row-static">
            <strong>${movement.productName} / ${movement.movementType}</strong>
            <span>${movement.quantityDelta} units / ${movement.note || "No note"}</span>
        </div>
    `).join("");
}

function renderAdminRuntimeInfo(runtime) {
    elements.adminRuntimeInfo.innerHTML = `
        <div class="admin-row admin-row-static">
            <strong>Database</strong>
            <span>${runtime.databasePath}</span>
        </div>
        <div class="admin-row admin-row-static">
            <strong>Application data</strong>
            <span>${runtime.applicationDataPath}</span>
        </div>
        ${runtime.accessUrls.map(url => `
            <div class="admin-row admin-row-static">
                <strong>Access URL</strong>
                <span>${url}</span>
            </div>
        `).join("")}
    `;
}

function populateAdminProductForm(product) {
    elements.adminProductId.value = product.id || "";
    elements.adminProductName.value = product.name || "";
    elements.adminProductCategory.value = product.category || "";
    elements.adminProductPrice.value = Number(product.price || 0);
    elements.adminProductOrder.value = product.displayOrder || 0;
    elements.adminProductBarcode.value = product.barcode || "";
    elements.adminProductStock.value = product.stockQuantity || 0;
    elements.adminProductActive.value = String(product.active);
    elements.adminAdjustProductId.value = product.id || "";
}

function resetSaleFlow(options = {}) {
    const preserveReceipt = options.preserveReceipt === true;
    state.cart = [];
    state.cashInput = "";
    state.paymentMethod = "Cash";
    state.selectedCategory = "ALL";
    elements.paymentMethod.value = "Cash";
    elements.productSearch.value = "";
    elements.barcodeInput.value = "";
    renderCategories(state.categories);
    renderCart();
    updateProductModeBanner();

    if (!preserveReceipt) {
        state.lastReceipt = "No sale completed yet.";
        elements.receiptOutput.textContent = state.lastReceipt;
        elements.checkoutNotice.className = "checkout-notice hidden";
    }
}

function buildKeypad() {
    const labels = ["7", "8", "9", "4", "5", "6", "1", "2", "3", "00", "0", "."];
    elements.keypad.innerHTML = labels.map(label => `
        <button class="keypad-button" data-key="${label}">${label}</button>
    `).join("");
}

function addProductToCart(product) {
    const existing = state.cart.find(item => item.id === product.id);
    if (existing) {
        if (existing.quantity < product.stockQuantity) {
            existing.quantity += 1;
        }
    } else {
        state.cart.push({
            id: product.id,
            name: product.name,
            price: product.price,
            stockQuantity: product.stockQuantity,
            quantity: 1
        });
    }
    renderCart();
}

function updateCartItem(productId, action) {
    const item = state.cart.find(entry => entry.id === productId);
    if (!item) {
        return;
    }

    if (action === "increase" && item.quantity < item.stockQuantity) {
        item.quantity += 1;
    }
    if (action === "decrease") {
        item.quantity -= 1;
    }
    if (action === "remove" || item.quantity <= 0) {
        state.cart = state.cart.filter(entry => entry.id !== productId);
    }
    renderCart();
}

function clearCash() {
    state.cashInput = "";
    renderCart();
}

function appendCashInput(value) {
    if (state.paymentMethod !== "Cash") {
        return;
    }
    if (value === "." && state.cashInput.includes(".")) {
        return;
    }
    state.cashInput += value;
    renderCart();
}

function focusBarcodeEntry() {
    elements.barcodeInput.focus();
    elements.barcodeInput.select();
}

function focusSearchEntry() {
    elements.productSearch.focus();
    elements.productSearch.select();
}

function focusCashEntry() {
    state.paymentMethod = "Cash";
    elements.paymentMethod.value = "Cash";
    renderCart();
}

function findProductByBarcode(barcode) {
    return state.allProducts.find(product => String(product.barcode || "").trim() === barcode.trim());
}

function addProductByBarcode(barcode) {
    if (!barcode) {
        return;
    }

    const product = findProductByBarcode(barcode);
    if (!product) {
        throw new Error(`No product found for barcode ${barcode}.`);
    }

    addProductToCart(product);
    elements.barcodeInput.value = "";
    showCheckoutNotice(`${product.name} added to cart.`, "success");
}

async function loadDashboard() {
    elements.lastUpdated.textContent = "Refreshing...";
    const dashboard = await fetchJson("/api/dashboard");
    state.dashboard = dashboard;
    state.categories = dashboard.categories;

    elements.todayTotal.textContent = money(dashboard.today.total);
    elements.transactionCount.textContent = `${dashboard.today.transactionCount} transactions`;
    elements.lastUpdated.textContent = `Updated ${new Date().toLocaleTimeString()}`;

    renderMetrics(dashboard);
    renderCategories(dashboard.categories);
    renderLowStock(dashboard.lowStock);
    renderPayments(dashboard.payments);
    renderTopProducts(dashboard.topProducts);
    renderRecentSales(dashboard.recentSales);

    await searchProducts(elements.productSearch.value.trim());
    renderFastMovers(dashboard);
}

async function searchProducts(query) {
    const path = query
        ? `/api/products?query=${encodeURIComponent(query)}&limit=36`
        : "/api/products?limit=36";
    const payload = await fetchJson(path);
    if (!query) {
        state.allProducts = payload.products.slice();
    }
    const filteredProducts = payload.products.filter(product =>
        state.selectedCategory === "ALL" || product.category === state.selectedCategory
    );
    state.products = filteredProducts;
    renderProducts(filteredProducts);
    updateProductModeBanner();
}

async function checkout() {
    if (!state.cart.length) {
        throw new Error("Add items before checkout.");
    }

    const totals = getTotals();
    if (state.paymentMethod === "Cash" && totals.cash < totals.total) {
        throw new Error("Cash amount is less than the total.");
    }

    const items = state.cart.map(item => `${item.id}:${item.quantity}`).join(",");
    const payload = await postForm("/api/sales", {
        paymentMethod: state.paymentMethod,
        cashAmount: state.paymentMethod === "Cash" ? String(totals.cash.toFixed(2)) : "",
        items
    });

    state.lastReceipt = payload.receipt;
    elements.receiptOutput.textContent = payload.receipt;
    showCheckoutNotice(`Sale completed. ${money(payload.totals.total)} processed via ${state.paymentMethod}.`, "success");
    openSaleModal("Sale completed", `${money(payload.totals.total)} processed via ${state.paymentMethod}.`);
    resetSaleFlow({ preserveReceipt: true });
    await loadDashboard();
    state.lastSaleId = state.dashboard && state.dashboard.recentSales.length
        ? state.dashboard.recentSales[0].id
        : null;
    elements.productSearch.focus();
}

async function reprintSale(saleId) {
    const payload = await fetchJson(`/api/sales/receipt?saleId=${encodeURIComponent(saleId)}`);
    state.lastSaleId = saleId;
    state.lastReceipt = payload.receipt;
    elements.receiptOutput.textContent = payload.receipt;
    showCheckoutNotice(`Receipt reloaded for sale #${saleId}.`, "success");
}

async function loadTodayCloseout() {
    const payload = await fetchJson("/api/closeout/today");
    renderCloseout(payload);
    openCloseoutModal();
}

async function refundSale(saleId) {
    const payload = await postForm("/api/sales/refund", { saleId: String(saleId) });
    showCheckoutNotice(`Sale #${saleId} refunded successfully.`, "success");
    await loadDashboard();
    renderCloseout(payload.closeout);
    openCloseoutModal();
}

async function loadAdminData() {
    const [products, movements, runtime] = await Promise.all([
        fetchJson("/api/admin/products"),
        fetchJson("/api/admin/inventory/movements?limit=20"),
        fetchJson("/api/admin/runtime")
    ]);
    renderAdminProducts(products.products);
    renderAdminMovements(movements.movements);
    renderAdminRuntimeInfo(runtime);
    openAdminModal();
}

async function saveAdminProduct() {
    const payload = await postForm("/api/admin/products", {
        id: elements.adminProductId.value,
        name: elements.adminProductName.value,
        category: elements.adminProductCategory.value,
        price: elements.adminProductPrice.value,
        displayOrder: elements.adminProductOrder.value,
        barcode: elements.adminProductBarcode.value,
        stockQuantity: elements.adminProductStock.value,
        active: elements.adminProductActive.value
    });
    showAdminMessage(payload.message, "success");
    await loadAdminData();
    await loadDashboard();
    populateAdminProductForm(payload.product);
}

async function adjustAdminInventory() {
    const payload = await postForm("/api/admin/inventory/adjust", {
        productId: elements.adminAdjustProductId.value,
        quantityDelta: elements.adminAdjustDelta.value,
        movementType: elements.adminAdjustType.value,
        note: elements.adminAdjustNote.value
    });
    showAdminMessage(payload.message, "success");
    elements.adminAdjustDelta.value = "";
    elements.adminAdjustNote.value = "";
    await loadAdminData();
    await loadDashboard();
}

async function downloadBackup() {
    const response = await fetch("/api/admin/backup", {
        headers: authHeaders()
    });
    if (!response.ok) {
        if (response.status === 401) {
            handleUnauthorized();
        }
        throw new Error("Unable to download backup.");
    }

    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    const disposition = response.headers.get("Content-Disposition") || "";
    const fileNameMatch = disposition.match(/filename=\"([^\"]+)\"/);
    link.href = url;
    link.download = fileNameMatch ? fileNameMatch[1] : "javapos-backup.db";
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    showAdminMessage("Backup downloaded.", "success");
}

async function login() {
    hideLoginMessage();
    const payload = await postForm("/api/auth/login", {
        username: elements.loginUsername.value.trim(),
        password: elements.loginPassword.value,
        newPassword: elements.loginNewPassword.value
    });

    if (payload.mustChangePassword) {
        elements.newPasswordWrap.className = "search-field";
        showLoginMessage("A new password is required for this account.", "error");
        return;
    }

    state.authToken = payload.token;
    state.currentUser = payload.user;
    localStorage.setItem("javapos.token", state.authToken);
    hideLoginOverlay();
    updateAuthUi();
    await loadDashboard();
    focusBarcodeEntry();
}

async function restoreSession() {
    if (!state.authToken) {
        showLoginOverlay();
        return;
    }

    try {
        const payload = await fetchJson("/api/auth/session");
        state.currentUser = payload.user;
        updateAuthUi();
        hideLoginOverlay();
        await loadDashboard();
        focusBarcodeEntry();
    } catch (error) {
        handleUnauthorized();
    }
}

async function logout() {
    try {
        if (state.authToken) {
            await postForm("/api/auth/logout", {});
        }
    } catch (error) {
        // Ignore logout failures and clear local state.
    }
    handleUnauthorized();
}

function handleUnauthorized() {
    state.authToken = "";
    state.currentUser = null;
    localStorage.removeItem("javapos.token");
    updateAuthUi();
    showLoginOverlay();
}

function wireEvents() {
    elements.refreshButton.addEventListener("click", () => {
        loadDashboard().catch(handleError);
    });

    elements.adminButton.addEventListener("click", () => {
        loadAdminData().catch(handleError);
    });

    elements.closeoutButton.addEventListener("click", () => {
        loadTodayCloseout().catch(handleError);
    });

    elements.logoutButton.addEventListener("click", () => {
        logout().catch(handleError);
    });

    elements.loginButton.addEventListener("click", () => {
        login().catch(error => {
            showLoginMessage(error.message || "Unable to sign in.", "error");
        });
    });

    elements.productSearch.addEventListener("input", event => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => {
            searchProducts(event.target.value.trim()).catch(handleError);
        }, 180);
    });

    elements.barcodeInput.addEventListener("keydown", event => {
        if (event.key !== "Enter") {
            return;
        }
        event.preventDefault();
        try {
            addProductByBarcode(event.target.value.trim());
        } catch (error) {
            handleError(error);
        }
    });

    elements.categoryChips.addEventListener("click", event => {
        const button = event.target.closest("[data-category]");
        if (!button) {
            return;
        }
        state.selectedCategory = button.dataset.category;
        renderCategories(state.categories);
        searchProducts(elements.productSearch.value.trim()).catch(handleError);
    });

    elements.fastMovers.addEventListener("click", event => {
        const button = event.target.closest("[data-fast-product-id]");
        if (!button) {
            return;
        }
        const productId = Number(button.dataset.fastProductId);
        const product = state.allProducts.find(entry => entry.id === productId)
            || state.products.find(entry => entry.id === productId);
        if (product) {
            addProductToCart(product);
        }
    });

    elements.productGrid.addEventListener("click", event => {
        const button = event.target.closest("[data-product-id]");
        if (!button) {
            return;
        }
        const productId = Number(button.dataset.productId);
        const product = state.products.find(entry => entry.id === productId);
        if (product) {
            addProductToCart(product);
        }
    });

    elements.cartItems.addEventListener("click", event => {
        const button = event.target.closest("[data-action]");
        if (!button) {
            return;
        }
        updateCartItem(Number(button.dataset.productId), button.dataset.action);
    });

    elements.keypad.addEventListener("click", event => {
        const button = event.target.closest("[data-key]");
        if (!button) {
            return;
        }
        appendCashInput(button.dataset.key);
    });

    elements.paymentMethod.addEventListener("change", event => {
        state.paymentMethod = event.target.value;
        if (state.paymentMethod !== "Cash") {
            state.cashInput = "";
        }
        renderCart();
    });

    elements.clearCartButton.addEventListener("click", () => {
        state.cart = [];
        renderCart();
    });

    elements.newSaleButton.addEventListener("click", () => {
        resetSaleFlow();
        searchProducts("").catch(handleError);
        focusBarcodeEntry();
    });

    elements.cashClearButton.addEventListener("click", () => {
        clearCash();
    });

    elements.backspaceButton.addEventListener("click", () => {
        state.cashInput = state.cashInput.slice(0, -1);
        renderCart();
    });

    elements.checkoutButton.addEventListener("click", () => {
        checkout().catch(handleError);
    });

    elements.recentSales.addEventListener("click", event => {
        const reprintButton = event.target.closest("[data-sale-id]");
        if (reprintButton) {
            reprintSale(Number(reprintButton.dataset.saleId)).catch(handleError);
            return;
        }
        const refundButton = event.target.closest("[data-refund-sale-id]");
        if (refundButton) {
            refundSale(Number(refundButton.dataset.refundSaleId)).catch(handleError);
        }
    });

    elements.printReceiptButton.addEventListener("click", () => {
        try {
            printReceipt();
        } catch (error) {
            handleError(error);
        }
    });

    elements.modalPrintButton.addEventListener("click", () => {
        try {
            printReceipt();
        } catch (error) {
            handleError(error);
        }
    });

    elements.modalCloseButton.addEventListener("click", () => {
        closeSaleModal();
        focusBarcodeEntry();
    });

    elements.closeoutCloseButton.addEventListener("click", () => {
        closeCloseoutModal();
        focusBarcodeEntry();
    });

    elements.adminCloseButton.addEventListener("click", () => {
        closeAdminModal();
        focusBarcodeEntry();
    });

    elements.adminSaveProductButton.addEventListener("click", () => {
        saveAdminProduct().catch(handleError);
    });

    elements.adminAdjustInventoryButton.addEventListener("click", () => {
        adjustAdminInventory().catch(handleError);
    });

    elements.adminBackupButton.addEventListener("click", () => {
        downloadBackup().catch(handleError);
    });

    elements.adminProductsTable.addEventListener("click", event => {
        const row = event.target.closest("[data-admin-product-id]");
        if (!row) {
            return;
        }
        const productId = Number(row.dataset.adminProductId);
        const product = state.allProducts.find(entry => entry.id === productId);
        if (product) {
            populateAdminProductForm(product);
        }
    });

    document.addEventListener("keydown", event => {
        const targetTag = event.target.tagName;
        const isTypingTarget = targetTag === "INPUT" || targetTag === "TEXTAREA" || targetTag === "SELECT";

        if (event.key === "F2") {
            event.preventDefault();
            focusBarcodeEntry();
            return;
        }
        if (event.key === "F3") {
            event.preventDefault();
            focusSearchEntry();
            return;
        }
        if (event.key === "F4") {
            event.preventDefault();
            focusCashEntry();
            return;
        }
        if (event.key === "F8") {
            event.preventDefault();
            checkout().catch(handleError);
            return;
        }
        if (event.key === "Escape") {
            event.preventDefault();
            closeSaleModal();
            closeCloseoutModal();
            closeAdminModal();
            resetSaleFlow();
            searchProducts("").catch(handleError);
            focusBarcodeEntry();
            return;
        }

        if (!isTypingTarget && /^[0-9]$/.test(event.key) && state.paymentMethod === "Cash") {
            appendCashInput(event.key);
            return;
        }
        if (!isTypingTarget && event.key === "." && state.paymentMethod === "Cash") {
            appendCashInput(".");
        }
    });
}

function handleError(error) {
    console.error(error);
    elements.lastUpdated.textContent = error.message || "Unable to load dashboard";
    showCheckoutNotice(error.message || "Unable to load dashboard.", "error");
}

buildKeypad();
wireEvents();
renderCart();
elements.checkoutNotice.className = "checkout-notice hidden";
updateAuthUi();
restoreSession().catch(handleError);
