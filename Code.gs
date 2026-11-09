/**
 * GOOGLE APPS SCRIPT CODE (Code.gs)
 * ----------------------------------------------------
 * Dynamic E-Commerce Backend: Settings + Products + Orders & 1-Tap UPI Verification
 * 
 * 1. 'Settings' Tab: Brand Name, Logo, Hero Text, WhatsApp Number, UPI ID
 * 2. 'Products' Tab: Digital goods catalog, prices, features
 * 3. 'Orders' Tab: Live Customer Orders, UTR Numbers, and 1-Tap Checkbox Verification!
 */

function doGet(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var params = (e && e.parameter) ? e.parameter : {};
    var action = params.action ? String(params.action).trim() : "";

    // ----------------------------------------------------
    // ACTION 0: Run setupAutoTabs remotely via URL
    // ----------------------------------------------------
    if (action === "setupTabs" || action === "setupAutoTabs" || action === "init") {
      setupAutoTabs();
      return returnJsonResponse({
        success: true,
        message: "Google Sheet successfully initialized with Settings, Products, and Orders tabs!",
        settings: getSheetSettings(ss),
        products: getSheetProducts(ss)
      });
    }

    // Auto-create tabs if blank spreadsheet
    if (!ss.getSheetByName("Settings") || !ss.getSheetByName("Products") || !ss.getSheetByName("Orders")) {
      setupAutoTabs();
    }

    // ----------------------------------------------------
    // ACTION 1: Create New Order (Customer enters UTR on website)
    // ----------------------------------------------------
    if (action === "createOrder") {
      return handleCreateOrder(ss, params);
    }

    // ----------------------------------------------------
    // ACTION 2: Check Order Status (Website polling for 1-Tap mobile approval)
    // ----------------------------------------------------
    if (action === "checkOrderStatus") {
      return handleCheckOrderStatus(ss, params);
    }

    // ----------------------------------------------------
    // ACTION 3: Mark Order as Paid (Direct API override)
    // ----------------------------------------------------
    if (action === "markPaid") {
      return handleMarkPaid(ss, params);
    }

    // ----------------------------------------------------
    // ACTION 4: Android Companion App (Auto-Listen SMS & Notifications)
    // ----------------------------------------------------
    if (action === "autoVerifyFromSms" || action === "bankNotification" || action === "webhook") {
      return handleBankNotification(ss, params);
    }

    // ----------------------------------------------------
    // ACTION 5: Android Admin App - Full Dashboard Data
    // ----------------------------------------------------
    if (action === "getAdminDashboard" || action === "getOrders") {
      return handleGetAdminDashboard(ss, params);
    }

    // ----------------------------------------------------
    // ACTION 6: Android Admin App - Update Site Settings & Logo Upload
    // ----------------------------------------------------
    if (action === "updateSettings") {
      return handleUpdateSettings(ss, params);
    }
    if (action === "uploadImage" || action === "uploadLogo") {
      return handleUploadImage(ss, params);
    }

    // ----------------------------------------------------
    // ACTION 7: Android Admin App - Manage Products
    // ----------------------------------------------------
    if (action === "updateProduct") {
      return handleUpdateProduct(ss, params);
    }
    if (action === "addProduct") {
      return handleAddProduct(ss, params);
    }
    if (action === "approveOrder" || action === "markPaid") {
      return handleMarkPaid(ss, params);
    }
    if (action === "checkUserPayment") {
      return handleCheckUserPayment(ss, params);
    }
    if (action === "signup") {
      return handleSignup(ss, params);
    }
    if (action === "login") {
      return handleLogin(ss, params);
    }
    if (action === "updateUserProfile") {
      return handleUpdateUserProfile(ss, params);
    }
    if (action === "verifySiteUrl") {
      return handleVerifySiteUrl(params);
    }
    if (action === "changePassword") {
      return handleChangePassword(ss, params);
    }

    // ----------------------------------------------------
    // DEFAULT: Fetch Full Dashboard (Settings + Products Catalog + Orders + Stats)
    // ----------------------------------------------------
    if (params.view !== "html") {
      return handleGetAdminDashboard(ss, params);
    }

    var settings = getSheetSettings(ss);
    var products = getSheetProducts(ss);

    var responseData = {
      settings: settings,
      products: products
    };

    // Optional: Return HTML view if parameter view=html is present
    if (params.view === "html") {
      return HtmlService.createHtmlOutputFromFile("Index")
        .setTitle(settings.brand_name || "VastuGeo Store")
        .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL)
        .addMetaTag('viewport', 'width=device-width, initial-scale=1');
    }

    return returnJsonResponse(responseData);

  } catch (err) {
    return returnJsonResponse({ error: err.toString() });
  }
}

function doPost(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var params = {};

    // Support JSON postData or standard form parameters
    if (e && e.postData && e.postData.contents) {
      try {
        params = JSON.parse(e.postData.contents);
      } catch (parseErr) {
        params = e.parameter || {};
      }
    } else if (e && e.parameter) {
      params = e.parameter;
    }

    var action = params.action ? String(params.action).trim() : "";

    if (action === "createOrder") {
      return handleCreateOrder(ss, params);
    } else if (action === "checkOrderStatus") {
      return handleCheckOrderStatus(ss, params);
    } else if (action === "checkUserPayment") {
      return handleCheckUserPayment(ss, params);
    } else if (action === "markPaid" || action === "approveOrder") {
      return handleMarkPaid(ss, params);
    } else if (action === "autoVerifyFromSms" || action === "bankNotification" || action === "webhook") {
      return handleBankNotification(ss, params);
    } else if (action === "getAdminDashboard" || action === "getOrders") {
      return handleGetAdminDashboard(ss, params);
    } else if (action === "updateSettings") {
      return handleUpdateSettings(ss, params);
    } else if (action === "uploadImage" || action === "uploadLogo") {
      return handleUploadImage(ss, params);
    } else if (action === "updateProduct") {
      return handleUpdateProduct(ss, params);
    } else if (action === "addProduct") {
      return handleAddProduct(ss, params);
    } else if (action === "signup") {
      return handleSignup(ss, params);
    } else if (action === "login") {
      return handleLogin(ss, params);
    } else if (action === "updateUserProfile") {
      return handleUpdateUserProfile(ss, params);
    } else if (action === "verifySiteUrl") {
      return handleVerifySiteUrl(params);
    } else if (action === "changePassword") {
      return handleChangePassword(ss, params);
    }

    return returnJsonResponse({ error: "Unknown action", received: action });
  } catch (err) {
    return returnJsonResponse({ error: err.toString() });
  }
}

/**
 * 1. Create a new Order in the 'Orders' Sheet
 */
function handleCreateOrder(ss, params) {
  var ordersSheet = getOrCreateOrdersSheet(ss);
  
  var orderId = params.order_id || ("#ORD-" + Math.floor(100000 + Math.random() * 900000));
  var dateStr = Utilities.formatDate(new Date(), "Asia/Kolkata", "dd/MM/yyyy, hh:mm a");
  var name = params.name || params.customer_name || "Guest Buyer";
  var phone = params.phone || params.customer_phone || "";
  var product = params.product || params.product_title || "SheetStore Pro Kit";
  var amount = params.amount || 499;
  var utr = params.utr || params.utr_number || "";
  var status = "PENDING";
  var isVerified = false; // Checkbox starts unchecked (FALSE)

  // 1. Reconciliation: Check if Android Companion App already auto-captured this UTR from bank SMS!
  var data = ordersSheet.getDataRange().getValues();
  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var rowUtr = String(row[6]).trim();
    if (utr && rowUtr === utr && (row[7] === "PAID" || row[8] === true)) {
      // Merge customer's actual name and details into pre-captured row
      ordersSheet.getRange(i + 1, 1).setValue(orderId);
      ordersSheet.getRange(i + 1, 3).setValue(name);
      ordersSheet.getRange(i + 1, 4).setValue(phone);
      ordersSheet.getRange(i + 1, 5).setValue(product);
      SpreadsheetApp.flush();
      return returnJsonResponse({
        success: true,
        order_id: orderId,
        status: "PAID",
        message: "Payment instantly verified via Android App pre-capture!"
      });
    }
  }

  // Append new order row:
  // [Order ID, Date & Time, Customer Name, Phone, Product, Amount, UTR, Status, Quick Verify [✓]]
  ordersSheet.appendRow([orderId, dateStr, name, phone, product, amount, utr, status, isVerified]);
  
  var lastRow = ordersSheet.getLastRow();
  
  // Format the 9th cell as a Checkbox
  var checkboxCell = ordersSheet.getRange(lastRow, 9);
  var rule = SpreadsheetApp.newDataValidation().requireCheckbox().build();
  checkboxCell.setDataValidation(rule);
  
  // Style status cell as PENDING (Amber)
  var statusCell = ordersSheet.getRange(lastRow, 8);
  statusCell.setFontColor("#D97706").setFontWeight("bold");

  SpreadsheetApp.flush();

  return returnJsonResponse({
    success: true,
    order_id: orderId,
    status: "PENDING",
    message: "Order placed successfully. Waiting for admin 1-tap verification or Android auto-capture."
  });
}

/**
 * 2. Check Order Status (Website polls this every 3 seconds)
 */
function handleCheckOrderStatus(ss, params) {
  var orderId = params.order_id ? String(params.order_id).trim().toUpperCase() : "";
  var utr = params.utr ? String(params.utr).trim() : "";
  var rawPhone = params.phone || params.customer_phone || "";
  var cleanPhone = String(rawPhone).trim().replace(/\D/g, '');
  if (cleanPhone.length > 10) cleanPhone = cleanPhone.slice(-10);
  var email = params.email ? String(params.email).trim().toLowerCase() : "";
  
  if (!orderId && !utr && !cleanPhone && !email) {
    return returnJsonResponse({ error: "Missing order_id, utr, phone or email parameter" });
  }

  var ordersSheet = ss.getSheetByName("Orders");
  if (!ordersSheet) {
    return returnJsonResponse({ status: "PENDING", is_paid: false, message: "Orders tab not created yet" });
  }

  var data = ordersSheet.getDataRange().getValues();
  // Headers: [0: Order ID, 1: Date, 2: Name, 3: Phone, 4: Product, 5: Amount, 6: UTR, 7: Status, 8: Verified]
  // Iterate newest orders first
  for (var i = data.length - 1; i >= 1; i--) {
    var row = data[i];
    var rowOrderId = String(row[0]).trim().toUpperCase();
    var rowPhone = String(row[3]).trim().replace(/\D/g, '');
    if (rowPhone.length > 10) rowPhone = rowPhone.slice(-10);
    var rowUtr = String(row[6]).trim();
    var rowStatus = String(row[7]).trim().toUpperCase();
    var isVerified = (row[8] === true || String(row[8]).toUpperCase() === "TRUE");

    var match = false;
    if (orderId && rowOrderId === orderId) match = true;
    if (utr && rowUtr === utr) match = true;
    if (cleanPhone && rowPhone && rowPhone === cleanPhone) match = true;

    if (match) {
      // If admin tapped the Checkbox in Google Sheets mobile app, update Status column to PAID (Green)
      if (isVerified && rowStatus !== "PAID") {
        ordersSheet.getRange(i + 1, 8).setValue("PAID");
        ordersSheet.getRange(i + 1, 8).setFontColor("#10B981").setFontWeight("bold");
        SpreadsheetApp.flush();
        rowStatus = "PAID";
      }

      var isPaid = (rowStatus === "PAID" || isVerified);
      if (isPaid) {
        var userAcc = autoProvisionUserOnPaymentSuccess(ss, row[2], row[3], params.email || "");
        return returnJsonResponse({
          success: true,
          order_id: row[0],
          status: "PAID",
          is_paid: true,
          customer_name: row[2],
          customer_phone: row[3],
          product: row[4],
          amount: row[5],
          login_identifier: userAcc.login_id,
          default_password: "123456"
        });
      } else {
        return returnJsonResponse({
          success: true,
          order_id: row[0],
          status: "PENDING",
          is_paid: false,
          customer_name: row[2],
          product: row[4],
          amount: row[5]
        });
      }
    }
  }

  return returnJsonResponse({ success: false, status: "NOT_FOUND", is_paid: false, message: "Order not found" });
}

/**
 * 3. Mark Order as Paid (Manual Approval from Admin App or Web)
 */
function handleMarkPaid(ss, params) {
  var orderId = params.order_id ? String(params.order_id).trim().toUpperCase() : "";
  var utr = params.utr ? String(params.utr).trim() : "";
  if (!orderId && !utr) {
    return returnJsonResponse({ error: "Missing order_id or utr" });
  }

  var ordersSheet = ss.getSheetByName("Orders");
  if (!ordersSheet) return returnJsonResponse({ error: "Orders sheet not found" });

  var data = ordersSheet.getDataRange().getValues();
  for (var i = data.length - 1; i >= 1; i--) {
    var rOrderId = String(data[i][0]).trim().toUpperCase();
    var rUtr = String(data[i][6]).trim();
    if ((orderId && rOrderId === orderId) || (utr && rUtr === utr)) {
      ordersSheet.getRange(i + 1, 8).setValue("PAID").setFontColor("#10B981").setFontWeight("bold");
      ordersSheet.getRange(i + 1, 9).setValue(true);
      SpreadsheetApp.flush();
      var userAcc = autoProvisionUserOnPaymentSuccess(ss, data[i][2], data[i][3], params.email || "");
      return returnJsonResponse({
        success: true,
        order_id: data[i][0],
        status: "PAID",
        is_paid: true,
        customer_name: data[i][2],
        login_identifier: userAcc.login_id,
        default_password: "123456"
      });
    }
  }

  return returnJsonResponse({ error: "Order not found with given ID or UTR" });
}

/**
 * Check User Payment Guard (Protects Dashboard Access)
 */
function handleCheckUserPayment(ss, params) {
  var orderId = params.order_id ? String(params.order_id).trim().toUpperCase() : "";
  var utr = params.utr ? String(params.utr).trim() : "";
  var rawPhone = params.phone || "";
  var cleanPhone = String(rawPhone).trim().replace(/\D/g, '');
  if (cleanPhone.length > 10) cleanPhone = cleanPhone.slice(-10);
  var email = params.email ? String(params.email).trim().toLowerCase() : "";

  // 1. Check Orders Sheet first
  var ordersSheet = ss.getSheetByName("Orders");
  if (ordersSheet) {
    var oData = ordersSheet.getDataRange().getValues();
    for (var i = oData.length - 1; i >= 1; i--) {
      var row = oData[i];
      var rOrderId = String(row[0]).trim().toUpperCase();
      var rPhone = String(row[3]).trim().replace(/\D/g, '');
      if (rPhone.length > 10) rPhone = rPhone.slice(-10);
      var rUtr = String(row[6]).trim();
      var rStatus = String(row[7]).trim().toUpperCase();
      var rVerified = (row[8] === true || String(row[8]).toUpperCase() === "TRUE");

      var match = false;
      if (orderId && rOrderId === orderId) match = true;
      if (utr && rUtr === utr) match = true;
      if (cleanPhone && rPhone && rPhone === cleanPhone) match = true;

      if (match) {
        var isPaid = (rStatus === "PAID" || rVerified);
        return returnJsonResponse({
          success: true,
          is_paid: isPaid,
          status: isPaid ? "PAID" : "PENDING",
          order_id: row[0],
          customer_name: row[2],
          customer_phone: row[3],
          amount: row[5],
          utr: row[6]
        });
      }
    }
  }

  // 2. Check Users Sheet (If already registered and active)
  var usersSheet = ss.getSheetByName("Users");
  if (usersSheet) {
    var uData = usersSheet.getDataRange().getValues();
    for (var j = 1; j < uData.length; j++) {
      var uRow = uData[j];
      var uEmail = String(uRow[3]).trim().toLowerCase();
      var uPhone = String(uRow[5]).trim().replace(/\D/g, '');
      if (uPhone.length > 10) uPhone = uPhone.slice(-10);
      var uStatus = String(uRow[10]).trim().toUpperCase();

      if ((cleanPhone && uPhone === cleanPhone) || (email && uEmail === email)) {
        var isActive = (uStatus === "ACTIVE");
        return returnJsonResponse({
          success: true,
          is_paid: isActive,
          status: isActive ? "PAID" : "PENDING",
          customer_name: uRow[2],
          customer_phone: uRow[5],
          email: uRow[3]
        });
      }
    }
  }

  return returnJsonResponse({
    success: false,
    is_paid: false,
    status: "UNPAID",
    message: "No verified paid record found"
  });
}

/**
 * 4. Automated UPI Verification from Android Companion App (SMS / Notification Listener)
 * ----------------------------------------------------
 * When the Android app receives an SMS or Notification from Bank/PhonePe/GPay:
 * e.g. "Credited with Rs. 499.00 on 05-Nov-26 by UPI Ref 412984920194"
 * The Android app posts to: WebAppURL?action=bankNotification&raw_text=... OR utr=...&amount=...
 */
function handleBankNotification(ss, params) {
  var ordersSheet = getOrCreateOrdersSheet(ss);
  
  var utr = params.utr ? String(params.utr).trim() : "";
  var amount = params.amount ? String(params.amount).trim() : "";
  var rawText = params.raw_text || params.message || params.text || "";

  // If raw_text is supplied, auto-parse UTR and Amount via regex
  if (rawText) {
    // 12-digit UTR Regex (e.g. 412984920194 or UPI/412984920194)
    if (!utr) {
      var utrMatch = rawText.match(/\b\d{12}\b/);
      if (utrMatch) utr = utrMatch[0];
    }
    // Amount Regex (e.g. Rs. 499, INR 499, ₹499)
    if (!amount) {
      var amountMatch = rawText.match(/(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{2})?)/i);
      if (amountMatch) amount = amountMatch[1].replace(/,/g, '');
    }
  }

  if (!utr) {
    return returnJsonResponse({
      success: false,
      message: "No 12-digit UTR found in notification/SMS text",
      received_text: rawText
    });
  }

  var data = ordersSheet.getDataRange().getValues();
  var orderFound = false;
  var matchedOrderId = "";

  // Search existing orders for this UTR
  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var rowUtr = String(row[6]).trim();

    if (rowUtr === utr) {
      orderFound = true;
      matchedOrderId = row[0];
      // Mark as PAID (Green) and tick the Checkbox!
      ordersSheet.getRange(i + 1, 8).setValue("PAID").setFontColor("#10B981").setFontWeight("bold");
      ordersSheet.getRange(i + 1, 9).setValue(true);
      SpreadsheetApp.flush();
      autoProvisionUserOnPaymentSuccess(ss, row[2], row[3], "");
      break;
    }
  }

  // Pre-Payment Buffer: If customer hasn't submitted website form yet, log verified incoming payment!
  if (!orderFound) {
    var tempId = "#AUTO-" + Math.floor(100000 + Math.random() * 900000);
    var dateStr = Utilities.formatDate(new Date(), "Asia/Kolkata", "dd/MM/yyyy, hh:mm a");
    var rowAmount = amount ? Number(amount) : 499;
    ordersSheet.appendRow([tempId, dateStr, "Auto-Captured (Android App)", "Via Bank SMS", "Direct UPI Payment", rowAmount, utr, "PAID", true]);
    
    var lastRow = ordersSheet.getLastRow();
    ordersSheet.getRange(lastRow, 9).setDataValidation(SpreadsheetApp.newDataValidation().requireCheckbox().build());
    ordersSheet.getRange(lastRow, 8).setFontColor("#10B981").setFontWeight("bold");
    SpreadsheetApp.flush();
    matchedOrderId = tempId;
  }

  return returnJsonResponse({
    success: true,
    message: "Payment automatically verified via Android Companion App!",
    utr: utr,
    amount: amount,
    order_id: matchedOrderId,
    status: "PAID"
  });
}

/**
 * 5. Android Admin App: Get Full Dashboard Data (Orders + Revenue Stats + Settings + Products)
 */
function handleGetAdminDashboard(ss, params) {
  var ordersSheet = getOrCreateOrdersSheet(ss);
  var data = ordersSheet.getDataRange().getValues();

  var orders = [];
  var totalRevenue = 0;
  var totalOrders = 0;
  var paidOrders = 0;
  var pendingOrders = 0;
  var todayRevenue = 0;
  var todayOrders = 0;

  var todayStr = Utilities.formatDate(new Date(), "Asia/Kolkata", "dd/MM/yyyy");

  // Headers: [0: Order ID, 1: Date, 2: Name, 3: Phone, 4: Product, 5: Amount, 6: UTR, 7: Status, 8: Verified]
  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    if (!row[0] || String(row[0]).trim() === "") continue;

    var amt = Number(row[5]) || 0;
    var status = String(row[7]).trim().toUpperCase();
    var isPaid = (status === "PAID" || row[8] === true || String(row[8]).toUpperCase() === "TRUE");
    var dateStr = String(row[1]);

    totalOrders++;
    if (isPaid) {
      paidOrders++;
      totalRevenue += amt;
    } else {
      pendingOrders++;
    }

    if (dateStr.indexOf(todayStr) !== -1) {
      todayOrders++;
      if (isPaid) {
        todayRevenue += amt;
      }
    }

    orders.push({
      order_id: String(row[0]),
      date: dateStr,
      customer_name: String(row[2]),
      customer_phone: String(row[3]),
      product: String(row[4]),
      amount: amt,
      utr: String(row[6]),
      status: isPaid ? "PAID" : "PENDING",
      verified: isPaid
    });
  }

  // Reverse so latest orders are at the top
  orders.reverse();

  var stats = {
    totalRevenue: totalRevenue,
    totalOrders: totalOrders,
    paidOrders: paidOrders,
    pendingOrders: pendingOrders,
    todayRevenue: todayRevenue,
    todayOrders: todayOrders
  };

  return returnJsonResponse({
    success: true,
    spreadsheet_url: ss.getUrl(),
    stats: stats,
    orders: orders,
    settings: getSheetSettings(ss),
    products: getSheetProducts(ss)
  });
}

/**
 * 6. Android Admin App: Update Website Settings (Store Name, WhatsApp, UPI, Headline, etc.)
 */
function handleUpdateSettings(ss, params) {
  var settingsSheet = ss.getSheetByName("Settings");
  if (!settingsSheet) {
    settingsSheet = ss.insertSheet("Settings");
    settingsSheet.appendRow(["key", "value", "description"]);
  }

  var data = settingsSheet.getDataRange().getValues();
  var updatedKeys = [];
  var ignoreKeys = ["action", "view", "format"];

  // Dynamically update any setting passed in params (template_id, brand_name, etc.)
  for (var key in params) {
    if (params.hasOwnProperty(key) && ignoreKeys.indexOf(key) === -1) {
      if (params[key] !== undefined && params[key] !== null) {
        var newVal = String(params[key]).trim();
        var found = false;

        for (var r = 1; r < data.length; r++) {
          if (String(data[r][0]).trim().toLowerCase() === key.toLowerCase()) {
            settingsSheet.getRange(r + 1, 2).setValue(newVal);
            found = true;
            break;
          }
        }

        if (!found) {
          settingsSheet.appendRow([key, newVal, "Updated via API / Android App"]);
        }
        updatedKeys.push(key);
      }
    }
  }

  SpreadsheetApp.flush();

  return returnJsonResponse({
    success: true,
    message: "Settings updated successfully",
    updated_keys: updatedKeys,
    settings: getSheetSettings(ss)
  });
}

/**
 * 6B. Android Admin App: Upload Store Logo / Icon to Google Drive
 */
function handleUploadImage(ss, params) {
  try {
    var base64Data = params.base64 || params.data || "";
    var filename = params.filename || ("store_logo_" + new Date().getTime() + ".jpg");
    var mimeType = params.mimeType || "image/jpeg";

    if (!base64Data) {
      return returnJsonResponse({ success: false, error: "Missing image base64 data" });
    }

    if (base64Data.indexOf(",") > -1) {
      base64Data = base64Data.split(",")[1];
    }

    var decoded = Utilities.base64Decode(base64Data);
    var blob = Utilities.newBlob(decoded, mimeType, filename);

    var folderName = "SheetStore_Uploads";
    var folders = DriveApp.getFoldersByName(folderName);
    var folder;
    if (folders.hasNext()) {
      folder = folders.next();
    } else {
      folder = DriveApp.createFolder(folderName);
    }

    var file = folder.createFile(blob);
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);

    var fileId = file.getId();
    // High-speed Google CDN direct embedding URL
    var fileUrl = "https://lh3.googleusercontent.com/d/" + fileId;

    // Automatically update Settings sheet if updateLogo is true or key is logo_url
    if (params.updateLogo === true || params.updateLogo === "true" || params.setting_key === "logo_url") {
      var settingsSheet = ss.getSheetByName("Settings");
      if (!settingsSheet) {
        settingsSheet = ss.insertSheet("Settings");
        settingsSheet.appendRow(["key", "value", "description"]);
      }
      var data = settingsSheet.getDataRange().getValues();
      var updated = false;
      for (var r = 1; r < data.length; r++) {
        if (String(data[r][0]).trim().toLowerCase() === "logo_url") {
          settingsSheet.getRange(r + 1, 2).setValue(fileUrl);
          updated = true;
          break;
        }
      }
      if (!updated) {
        settingsSheet.appendRow(["logo_url", fileUrl, "Custom logo image URL"]);
      }
      SpreadsheetApp.flush();
    }

    return returnJsonResponse({
      success: true,
      message: "Logo uploaded to Google Drive successfully!",
      fileId: fileId,
      url: fileUrl
    });
  } catch (err) {
    return returnJsonResponse({ success: false, error: err.toString() });
  }
}

/**
 * 7. Android Admin App: Update Product (Price, Title, Active status)
 */
function handleUpdateProduct(ss, params) {
  var pSheet = ss.getSheetByName("Products");
  if (!pSheet) return returnJsonResponse({ error: "Products sheet not found" });

  var prodId = params.id ? String(params.id).trim() : "";
  if (!prodId) return returnJsonResponse({ error: "Missing product id" });

  var data = pSheet.getDataRange().getValues();
  var headers = data[0];
  var found = false;

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0]).trim() === prodId) {
      found = true;
      for (var j = 0; j < headers.length; j++) {
        var h = String(headers[j]).trim().toLowerCase();
        if (params[h] !== undefined && params[h] !== null) {
          pSheet.getRange(i + 1, j + 1).setValue(params[h]);
        }
      }
      break;
    }
  }

  SpreadsheetApp.flush();

  if (found) {
    return returnJsonResponse({ success: true, message: "Product updated successfully", id: prodId });
  } else {
    return returnJsonResponse({ success: false, message: "Product ID not found" });
  }
}

/**
 * 8. Android Admin App: Add New Product
 */
function handleAddProduct(ss, params) {
  var pSheet = ss.getSheetByName("Products");
  if (!pSheet) return returnJsonResponse({ error: "Products sheet not found" });

  var id = params.id || ("prod-" + Math.floor(1000 + Math.random() * 9000));
  var title = params.title || "New Product";
  var category = params.category || "digital";
  var badge = params.badge || "";
  var priceOrig = Number(params.price_original) || 999;
  var priceSell = Number(params.price_selling) || 299;
  var thumbnail = params.thumbnail || "https://images.unsplash.com/photo-1556742049-0a67c5574f73?auto=format&fit=crop&w=600&q=80";
  var description = params.description || "";
  var features = params.features || "";
  var isActive = (params.is_active !== undefined) ? String(params.is_active).toUpperCase() : "TRUE";

  pSheet.appendRow([id, title, category, badge, priceOrig, priceSell, thumbnail, description, features, isActive]);
  SpreadsheetApp.flush();

  return returnJsonResponse({
    success: true,
    message: "Product added successfully",
    id: id
  });
}

/**
 * Helper: Compute SHA-256 Hash of string
 */
function hashPassword(str) {
  if (!str) return "";
  var signature = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, String(str), Utilities.Charset.UTF_8);
  var output = "";
  for (var i = 0; i < signature.length; i++) {
    var val = signature[i];
    if (val < 0) val += 256;
    var byteString = val.toString(16);
    if (byteString.length == 1) byteString = "0" + byteString;
    output += byteString;
  }
  return output;
}

/**
 * Helper: Ensure 'Users' Sheet exists with appropriate headers & styling
 */
function getOrCreateUsersSheet(ss) {
  var sheet = ss.getSheetByName("Users");
  if (!sheet) {
    sheet = ss.insertSheet("Users");
    var headers = [
      "User ID",
      "Date & Time",
      "Full Name",
      "Email Address",
      "Password Hash",
      "WhatsApp / Phone",
      "GitHub Repo URL",
      "Website Live URL",
      "Google Sheet Web App URL",
      "Onboarding Step",
      "Account Status"
    ];
    sheet.appendRow(headers);
    sheet.getRange(1, 1, 1, headers.length)
      .setFontWeight("bold")
      .setBackground("#0F172A")
      .setFontColor("#38BDF8");
    sheet.setFrozenRows(1);
    sheet.setColumnWidth(1, 120);
    sheet.setColumnWidth(2, 160);
    sheet.setColumnWidth(3, 160);
    sheet.setColumnWidth(4, 220);
    sheet.setColumnWidth(5, 180);
    sheet.setColumnWidth(6, 140);
    sheet.setColumnWidth(7, 260);
    sheet.setColumnWidth(8, 260);
    sheet.setColumnWidth(9, 320);
    sheet.setColumnWidth(10, 130);
    sheet.setColumnWidth(11, 120);
  }
  return sheet;
}

/**
 * 9. User Sign Up Handler
 */
function handleSignup(ss, params) {
  var email = params.email ? String(params.email).trim().toLowerCase() : "";
  var password = params.password ? String(params.password).trim() : "";
  var name = params.name ? String(params.name).trim() : "Store Owner";
  var phone = params.phone ? String(params.phone).trim() : "";

  if (!email || !password) {
    return returnJsonResponse({ success: false, message: "Email aur Password dono zaroori hain!" });
  }

  var uSheet = getOrCreateUsersSheet(ss);
  var data = uSheet.getDataRange().getValues();

  // Check if user already exists
  for (var i = 1; i < data.length; i++) {
    if (String(data[i][3]).trim().toLowerCase() === email) {
      return returnJsonResponse({
        success: false,
        message: "Yeh email pehle se registered hai! Kripya Login karein."
      });
    }
  }

  var userId = "#USR-" + Math.floor(100000 + Math.random() * 900000);
  var dateStr = Utilities.formatDate(new Date(), "Asia/Kolkata", "dd/MM/yyyy, hh:mm a");
  var passHash = hashPassword(password);
  var step = 1;
  var status = "ACTIVE";

  uSheet.appendRow([userId, dateStr, name, email, passHash, phone, "", "", "", step, status]);
  SpreadsheetApp.flush();

  return returnJsonResponse({
    success: true,
    message: "Registration successful! Welcome to SheetStore Pro.",
    user: {
      id: userId,
      name: name,
      email: email,
      phone: phone,
      github_repo_url: "",
      website_live_url: "",
      sheet_url: "",
      onboarding_step: step,
      status: status
    }
  });
}

/**
 * Helper: Auto-Provision User Account upon Payment Success (Default Password: 123456)
 */
function autoProvisionUserOnPaymentSuccess(ss, customerName, customerPhone, customerEmail) {
  var name = customerName ? String(customerName).trim() : "Store Owner";
  var rawPhone = customerPhone ? String(customerPhone).trim() : "";
  var cleanPhone = rawPhone.replace(/\D/g, '');
  if (cleanPhone.length > 10) cleanPhone = cleanPhone.slice(-10);

  var email = customerEmail ? String(customerEmail).trim().toLowerCase() : "";
  if (!email && cleanPhone) {
    email = cleanPhone + "@client.store"; // Fallback username
  }

  var uSheet = getOrCreateUsersSheet(ss);
  var data = uSheet.getDataRange().getValues();
  var found = false;
  var existingUser = null;

  for (var i = 1; i < data.length; i++) {
    var r = data[i];
    var rEmail = String(r[3]).trim().toLowerCase();
    var rPhone = String(r[5]).trim().replace(/\D/g, '');
    if (rPhone.length > 10) rPhone = rPhone.slice(-10);

    if ((cleanPhone && rPhone === cleanPhone) || (email && rEmail === email)) {
      found = true;
      existingUser = {
        id: r[0],
        name: r[2],
        login_id: cleanPhone || email,
        is_new: false
      };
      break;
    }
  }

  if (found) {
    return existingUser;
  }

  // Create new user with default password '123456'
  var userId = "#USR-" + Math.floor(100000 + Math.random() * 900000);
  var dateStr = Utilities.formatDate(new Date(), "Asia/Kolkata", "dd/MM/yyyy, hh:mm a");
  var defaultPass = "123456";
  var passHash = hashPassword(defaultPass);
  var step = 1;
  var status = "ACTIVE";

  uSheet.appendRow([userId, dateStr, name, email, passHash, cleanPhone, "", "", "", step, status]);
  SpreadsheetApp.flush();

  // If customer provided an actual email address, send welcome email with credentials
  if (customerEmail && customerEmail.indexOf("@") !== -1 && customerEmail.indexOf("client.store") === -1) {
    try {
      var subject = "🎉 Aapka SheetStore Pro Setup Ready Hai! [Login Details]";
      var body = "Namaste " + name + ",\n\n" +
        "Aapka payment safal ho gaya hai! Aapka SheetStore Pro portal account activate kar diya gaya hai.\n\n" +
        "------------------------------------\n" +
        "Aapke Portal Login Credentials:\n" +
        "------------------------------------\n" +
        "Portal Login URL: https://yuviteltechnologies-art.github.io/freewebsite/auth.html\n" +
        "Login ID (Mobile No): " + (cleanPhone || email) + "\n" +
        "Default Password: " + defaultPass + "\n\n" +
        "Aap dashboard me jaakar apna password kabhi bhi change kar sakte hain.\n\n" +
        "Dhanyawad,\nSheetStore Pro Team";
      MailApp.sendEmail(customerEmail, subject, body);
    } catch (mailErr) {
      // Ignore mail quota error
    }
  }

  return {
    id: userId,
    name: name,
    login_id: cleanPhone || email,
    default_password: defaultPass,
    is_new: true
  };
}

/**
 * 10. User Login Handler (Supports Mobile Number OR Email + Default Password '123456')
 */
function handleLogin(ss, params) {
  var rawIdentifier = params.identifier || params.email || params.phone || "";
  var identifier = String(rawIdentifier).trim().toLowerCase();
  var password = params.password ? String(params.password).trim() : "";

  if (!identifier || !password) {
    return returnJsonResponse({ success: false, message: "Mobile Number / Email aur Password dono zaroori hain!" });
  }

  var uSheet = getOrCreateUsersSheet(ss);
  var data = uSheet.getDataRange().getValues();
  var passHash = hashPassword(password);

  var cleanPhone = identifier.replace(/\D/g, '');
  if (cleanPhone.length > 10) cleanPhone = cleanPhone.slice(-10);

  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var rowEmail = String(row[3]).trim().toLowerCase();
    var rowPhone = String(row[5]).trim().replace(/\D/g, '');
    if (rowPhone.length > 10) rowPhone = rowPhone.slice(-10);

    var matches = (rowEmail && rowEmail === identifier) ||
                  (cleanPhone && rowPhone && rowPhone === cleanPhone) ||
                  (String(row[0]).trim().toLowerCase() === identifier);

    if (matches) {
      var storedPass = String(row[4]).trim();
      // Allow SHA-256 match or direct match
      if (storedPass === passHash || storedPass === password) {
        return returnJsonResponse({
          success: true,
          message: "Login successful!",
          user: {
            id: row[0],
            created_at: row[1],
            name: row[2],
            email: row[3],
            phone: row[5],
            github_repo_url: row[6],
            website_live_url: row[7],
            sheet_url: row[8],
            onboarding_step: Number(row[9]) || 1,
            status: row[10] || "ACTIVE"
          }
        });
      } else {
        return returnJsonResponse({ success: false, message: "Galat password! Agar aapne abhi purchase kiya hai toh default password '123456' try karein." });
      }
    }
  }

  return returnJsonResponse({ 
    success: false, 
    message: "Yeh Mobile/Email registered nahi mila. Portal sirf paid customers ke liye hai — website se purchase karte hi account automatic ban jata hai!" 
  });
}

/**
 * Change Password Handler (Customer can change default '123456' to their own secret password)
 */
function handleChangePassword(ss, params) {
  var rawIdentifier = params.identifier || params.email || params.phone || "";
  var identifier = String(rawIdentifier).trim().toLowerCase();
  var oldPassword = params.old_password ? String(params.old_password).trim() : "";
  var newPassword = params.new_password ? String(params.new_password).trim() : "";

  if (!identifier || !oldPassword || !newPassword) {
    return returnJsonResponse({ success: false, message: "Sabhi fields bharna zaroori hai!" });
  }

  if (newPassword.length < 4) {
    return returnJsonResponse({ success: false, message: "Naya password kam se kam 4 akshar ka hona chahiye!" });
  }

  var uSheet = getOrCreateUsersSheet(ss);
  var data = uSheet.getDataRange().getValues();
  var cleanPhone = identifier.replace(/\D/g, '');
  if (cleanPhone.length > 10) cleanPhone = cleanPhone.slice(-10);

  var oldHash = hashPassword(oldPassword);
  var newHash = hashPassword(newPassword);

  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var rEmail = String(row[3]).trim().toLowerCase();
    var rPhone = String(row[5]).trim().replace(/\D/g, '');
    if (rPhone.length > 10) rPhone = rPhone.slice(-10);

    var matches = (rEmail && rEmail === identifier) ||
                  (cleanPhone && rPhone && rPhone === cleanPhone) ||
                  (String(row[0]).trim().toLowerCase() === identifier);

    if (matches) {
      var storedPass = String(row[4]).trim();
      if (storedPass === oldHash || storedPass === oldPassword) {
        // Update password hash in sheet
        uSheet.getRange(i + 1, 5).setValue(newHash);
        SpreadsheetApp.flush();
        return returnJsonResponse({ 
          success: true, 
          message: "🎉 Password successfully update ho gaya! Ab naye password se login karein." 
        });
      } else {
        return returnJsonResponse({ success: false, message: "Purana password galat hai! Kripya dobara check karein." });
      }
    }
  }

  return returnJsonResponse({ success: false, message: "User account nahi mila!" });
}

/**
 * 11. User Profile Update (GitHub repo, live site URL, sheet URL, step)
 */
function handleUpdateUserProfile(ss, params) {
  var rawIdentifier = params.identifier || params.email || params.phone || params.user_id || "";
  var identifier = String(rawIdentifier).trim().toLowerCase();
  var cleanPhone = identifier.replace(/\D/g, '');
  if (cleanPhone.length > 10) cleanPhone = cleanPhone.slice(-10);

  if (!identifier && !cleanPhone) {
    return returnJsonResponse({ success: false, message: "User identify karne ke liye identifier (mobile ya email) zaroori hai!" });
  }

  var uSheet = getOrCreateUsersSheet(ss);
  var data = uSheet.getDataRange().getValues();
  var userRow = -1;

  for (var i = 1; i < data.length; i++) {
    var rowEmail = String(data[i][3]).trim().toLowerCase();
    var rowPhone = String(data[i][5]).trim().replace(/\D/g, '');
    var rowId = String(data[i][0]).trim().toLowerCase();
    if ((identifier && (rowEmail === identifier || rowId === identifier)) ||
        (cleanPhone && rowPhone && rowPhone === cleanPhone)) {
      userRow = i + 1;
      break;
    }
  }

  if (userRow === -1) {
    return returnJsonResponse({ success: false, message: "User account nahi mila!" });
  }

  if (params.name !== undefined) uSheet.getRange(userRow, 3).setValue(params.name);
  if (params.phone !== undefined) uSheet.getRange(userRow, 6).setValue(params.phone);
  if (params.github_repo_url !== undefined) uSheet.getRange(userRow, 7).setValue(params.github_repo_url);
  if (params.website_live_url !== undefined) uSheet.getRange(userRow, 8).setValue(params.website_live_url);
  if (params.sheet_url !== undefined) uSheet.getRange(userRow, 9).setValue(params.sheet_url);
  if (params.onboarding_step !== undefined) uSheet.getRange(userRow, 10).setValue(params.onboarding_step);
  if (params.status !== undefined) uSheet.getRange(userRow, 11).setValue(params.status);

  // Also update website_url in Settings sheet if available
  if (params.website_live_url) {
    var sSheet = ss.getSheetByName("Settings");
    if (sSheet) {
      var sData = sSheet.getDataRange().getValues();
      var foundSite = false;
      for (var s = 1; s < sData.length; s++) {
        if (String(sData[s][0]).trim().toLowerCase() === "website_url") {
          sSheet.getRange(s + 1, 2).setValue(params.website_live_url);
          foundSite = true;
          break;
        }
      }
      if (!foundSite) {
        sSheet.appendRow(["website_url", params.website_live_url, "User Live Website URL"]);
      }
    }
  }

  SpreadsheetApp.flush();

  var updated = uSheet.getRange(userRow, 1, 1, 11).getValues()[0];
  return returnJsonResponse({
    success: true,
    message: "Profile and website URL successfully saved in Google Sheet!",
    user: {
      id: updated[0],
      created_at: updated[1],
      name: updated[2],
      email: updated[3],
      phone: updated[5],
      github_repo_url: updated[6],
      website_live_url: updated[7],
      sheet_url: updated[8],
      onboarding_step: Number(updated[9]) || 1,
      status: updated[10]
    }
  });
}

/**
 * 12. Verify If GitHub Pages / Website URL is 100% Live
 */
function handleVerifySiteUrl(params) {
  var url = params.url ? String(params.url).trim() : "";
  if (!url) {
    return returnJsonResponse({ success: false, is_live: false, message: "URL provide nahi kiya gaya" });
  }
  if (!/^https?:\/\//i.test(url)) {
    url = "https://" + url;
  }

  try {
    var response = UrlFetchApp.fetch(url, {
      muteHttpExceptions: true,
      followRedirects: true,
      validateHttpsCertificates: true
    });
    var code = response.getResponseCode();
    var isLive = (code >= 200 && code < 400);

    return returnJsonResponse({
      success: true,
      url: url,
      status_code: code,
      is_live: isLive,
      message: isLive ? "Website LIVE hai aur respond kar rahi hai!" : "Website se HTTP " + code + " mila."
    });
  } catch (err) {
    return returnJsonResponse({
      success: false,
      url: url,
      is_live: false,
      error: err.toString(),
      message: "Website tak pahunch nahi sake. URL verify karein."
    });
  }
}

/**
 * Helper: Read Site Settings from 'Settings' Sheet
 */
function getSheetSettings(ss) {
  var settings = {};
  var settingsSheet = ss.getSheetByName("Settings");
  if (settingsSheet) {
    var sRows = settingsSheet.getDataRange().getValues();
    for (var s = 1; s < sRows.length; s++) {
      var k = String(sRows[s][0]).trim().toLowerCase();
      var v = sRows[s][1];
      if (k) {
        settings[k] = v;
      }
    }
  }
  try {
    settings["spreadsheet_url"] = ss.getUrl();
  } catch (e) {}
  return settings;
}

/**
 * Helper: Read Products from 'Products' Sheet
 */
function getSheetProducts(ss) {
  var productsSheet = ss.getSheetByName("Products") || ss.getActiveSheet();
  var values = productsSheet.getDataRange().getValues();
  var products = [];
  
  if (values.length > 1) {
    var headers = values[0];
    for (var i = 1; i < values.length; i++) {
      var row = values[i];
      var item = {};
      var hasData = false;
      
      for (var j = 0; j < headers.length; j++) {
        var key = String(headers[j]).trim().toLowerCase();
        var val = row[j];
        if (val !== "") hasData = true;
        item[key] = val;
      }
      
      if (hasData) {
        if (!item.is_active || String(item.is_active).toUpperCase() === "TRUE") {
          products.push(item);
        }
      }
    }
  }
  return products;
}

/**
 * Helper: Ensure 'Orders' sheet exists with headers, styling, and Checkbox validation
 */
function getOrCreateOrdersSheet(ss) {
  var sheet = ss.getSheetByName("Orders");
  if (!sheet) {
    sheet = ss.insertSheet("Orders");
    var headers = [
      "Order ID",
      "Date & Time",
      "Customer Name",
      "Phone / WhatsApp",
      "Product",
      "Amount (INR)",
      "UTR / UPI Ref",
      "Status",
      "Quick Verify [✓]"
    ];
    sheet.appendRow(headers);
    
    // Header style: Dark slate with white bold text
    sheet.getRange(1, 1, 1, headers.length)
      .setFontWeight("bold")
      .setBackground("#0F172A")
      .setFontColor("#38BDF8");
      
    sheet.setFrozenRows(1);
    sheet.setColumnWidth(1, 120); // Order ID
    sheet.setColumnWidth(2, 170); // Date & Time
    sheet.setColumnWidth(3, 150); // Customer Name
    sheet.setColumnWidth(4, 140); // Phone
    sheet.setColumnWidth(5, 220); // Product
    sheet.setColumnWidth(6, 110); // Amount
    sheet.setColumnWidth(7, 160); // UTR Number
    sheet.setColumnWidth(8, 110); // Status (PENDING/PAID)
    sheet.setColumnWidth(9, 140); // Quick Verify Checkbox
  }
  return sheet;
}

function returnJsonResponse(data) {
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}

/**
 * Run this function once in Apps Script to automatically generate all 3 tabs:
 * 1. 'Settings'
 * 2. 'Products'
 * 3. 'Orders' (with 1-Tap Checkbox verification setup!)
 */
function setupAutoTabs() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  // ----------------------------------------------------
  // 1. Create or Reset 'Settings' Sheet
  // ----------------------------------------------------
  var sSheet = ss.getSheetByName("Settings");
  if (!sSheet) {
    sSheet = ss.insertSheet("Settings");
  }
  sSheet.clear();
  sSheet.appendRow(["key", "value", "description"]);
  sSheet.getRange(1, 1, 1, 3).setFontWeight("bold").setBackground("#1E293B").setFontColor("#38BDF8");
  
  var defaultSettings = [
    ["brand_name", "SheetStore Pro", "Website ka main naam"],
    ["brand_tagline", "Zero Hosting & Domain E-Commerce", "Logo ke niche chota text"],
    ["logo_url", "", "Custom logo image URL (khali chhodne par default icon aayega)"],
    ["announcement_text", "Special Launch Discount: 80% OFF today! Sirf ₹499 me Lifetime Access!", "Top notification bar ka text"],
    ["hero_badge", "Zero Hosting • Zero Domain • 100% Free Lifetime", "Hero section ka chota trust badge"],
    ["hero_title", "Shopify aur WordPress ka Monthly Kharcha Band Karein.", "Main Badi Headline"],
    ["hero_subtitle", "Google Sheet ko apna backend banayein aur direct UPI QR & WhatsApp orders ke sath apni super fast e-commerce website live karein.", "Hero section ka description"],
    ["whatsapp_number", "919999999999", "Customer WhatsApp order number (with country code e.g. 91)"],
    ["upi_id", "payments@upi", "UPI QR code payment address (GPay/PhonePe/Paytm)"],
    ["template_id", "modern-blue", "Active website template (modern-blue, vedic-gold, minimal-light, neon-violet, emerald-pro)"]
  ];
  
  for (var k = 0; k < defaultSettings.length; k++) {
    sSheet.appendRow(defaultSettings[k]);
  }
  sSheet.setColumnWidth(1, 180);
  sSheet.setColumnWidth(2, 350);
  sSheet.setColumnWidth(3, 350);
  
  // ----------------------------------------------------
  // 2. Create or Reset 'Products' Sheet
  // ----------------------------------------------------
  var pSheet = ss.getSheetByName("Products");
  if (!pSheet) {
    pSheet = ss.insertSheet("Products");
  }
  pSheet.clear();
  
  var pHeaders = [
    "id",
    "title",
    "category",
    "badge",
    "price_original",
    "price_selling",
    "thumbnail",
    "description",
    "features",
    "is_active"
  ];
  
  pSheet.appendRow(pHeaders);
  pSheet.getRange(1, 1, 1, pHeaders.length).setFontWeight("bold").setBackground("#1E293B").setFontColor("#38BDF8");
  
  pSheet.appendRow([
    "prod-portfolio-template",
    "Modern 1-Page Portfolio Website Template (.ZIP)",
    "website",
    "Best Seller",
    999,
    299,
    "https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=600&q=80",
    "Super fast, responsive portfolio template built with Tailwind CSS. Perfect for developers, designers & freelancers.",
    "100% Responsive, Dark & Light Mode, 1-Click Deploy to GitHub, Lifetime Updates",
    "TRUE"
  ]);

  pSheet.appendRow([
    "prod-finance-tracker",
    "Automated Financial & Income Tracker (Google Sheet)",
    "sheet",
    "Essential",
    599,
    199,
    "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=600&q=80",
    "Track monthly expenses, freelancing earnings, investments, and savings goals automatically with visual charts.",
    "Automated Monthly Dashboards, Expense Categorizer, Savings Calculator, Mobile App Ready",
    "TRUE"
  ]);

  pSheet.appendRow([
    "prod-canva-reels-bundle",
    "500+ Viral Reels & Social Media Canva Templates Pack",
    "bundle",
    "Mega Saver",
    1999,
    399,
    "https://images.unsplash.com/photo-1611162617474-5b21e879e113?auto=format&fit=crop&w=600&q=80",
    "Ready-to-post aesthetic Instagram posts, carousel guides, and viral reel hooks in Canva. Drag-and-drop editable.",
    "500+ Editable Canva Designs, Trending Business & Growth Niche, High Engagement Layouts, Commercial License",
    "TRUE"
  ]);

  pSheet.appendRow([
    "prod-sheetstore-master-kit",
    "SheetStore Pro Ultimate E-Commerce Website Builder Kit",
    "website",
    "80% OFF",
    2499,
    499,
    "https://images.unsplash.com/photo-1556742049-0a67c5574f73?auto=format&fit=crop&w=600&q=80",
    "Launch your own digital goods store with Google Sheet backend. Zero hosting and zero domain required.",
    "Zero Monthly Fees, WhatsApp & UPI Checkout, Meta Ads Ready, 2-Minute Setup Video",
    "TRUE"
  ]);

  // ----------------------------------------------------
  // 3. Create or Reset 'Orders' Sheet (With Checkbox Column)
  // ----------------------------------------------------
  var oSheet = ss.getSheetByName("Orders");
  if (!oSheet) {
    oSheet = ss.insertSheet("Orders");
  }
  oSheet.clear();
  
  var oHeaders = [
    "Order ID",
    "Date & Time",
    "Customer Name",
    "Phone / WhatsApp",
    "Product",
    "Amount (INR)",
    "UTR / UPI Ref",
    "Status",
    "Quick Verify [✓]"
  ];
  oSheet.appendRow(oHeaders);
  oSheet.getRange(1, 1, 1, oHeaders.length).setFontWeight("bold").setBackground("#0F172A").setFontColor("#38BDF8");
  oSheet.setFrozenRows(1);
  
  // Sample demonstration order row
  oSheet.appendRow([
    "#ORD-8821",
    Utilities.formatDate(new Date(), "Asia/Kolkata", "dd/MM/yyyy, hh:mm a"),
    "Rajesh Sharma (Sample)",
    "919876543210",
    "SheetStore Pro Lifetime Kit",
    499,
    "412984920194",
    "PAID",
    true
  ]);
  
  // Set Checkbox validation on Row 2 column 9
  var sampleCheckbox = oSheet.getRange(2, 9);
  sampleCheckbox.setDataValidation(SpreadsheetApp.newDataValidation().requireCheckbox().build());
  oSheet.getRange(2, 8).setFontColor("#10B981").setFontWeight("bold");

  // Safely delete empty default "Sheet1" if other sheets exist
  var defaultSheet = ss.getSheetByName("Sheet1");
  if (defaultSheet && ss.getSheets().length > 1) {
    try {
      ss.deleteSheet(defaultSheet);
    } catch (e) {
      // Ignore if cannot delete
    }
  }

  // ----------------------------------------------------
  // 4. Create or Ensure 'Users' Sheet
  // ----------------------------------------------------
  getOrCreateUsersSheet(ss);

  SpreadsheetApp.flush();
}
