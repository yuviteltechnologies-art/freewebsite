/**
 * SheetStore Pro - Google Account Auto-Provisioner (Tareeqa B)
 * 
 * ==============================================================================
 * KAISE DEPLOY KAREIN (Apps Script Dashboard Me):
 * ==============================================================================
 * 1. script.google.com par jayein aur naya project kholein (ya existing provisioner script kholein)
 * 2. Ye pura code paste karein
 * 3. Upar "Deploy" -> "New deployment" (ya "Manage deployments" -> Edit -> "New version") dabayein
 * 4. Select type: "Web app"
 * 5. Configuration:
 *    - Description: "SheetStore Auto Provisioner"
 *    - Execute as: "User accessing the web app" (YE CRITICAL HAI! Isse merchant ki Drive me sheet banegi)
 *    - Who has access: "Anyone with Google account" (Anyone)
 * 6. "Deploy" dabayein aur Web App URL copy karein.
 * ==============================================================================
 */

function doGet(e) {
  // Aapki Master Sheet Template ID
  var templateId = "1UrKX7co3In04ijuxYP8HUgHYVD8YVgQOoJL41itO1uc";
  
  // Dashboard URL jahan wapas redirect karna hai
  var redirectUrl = (e && e.parameter && e.parameter.redirect_url) 
                    ? e.parameter.redirect_url 
                    : "https://yuviteltechnologies-art.github.io/freewebsite/dashboard.html";

  try {
    // 1. Visitor ke personal Google Drive me Master Sheet ki ek private copy banayein
    var templateFile = DriveApp.getFileById(templateId);
    var newCopy = templateFile.makeCopy("Digital Store - Products & Orders (Personal Copy)");
    
    // 2. Sharing Permissions: Anyone with link can EDIT
    // Isse aapka central backend API merchant ke customers ke live orders & products isme save kar payega
    newCopy.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.EDIT);
    
    var newSheetId = newCopy.getId();
    var newSheetUrl = newCopy.getUrl();
    
    // Final return URL with query parameters
    var returnUrl = redirectUrl + (redirectUrl.indexOf('?') > -1 ? '&' : '?') + 
                    'sheet_id=' + encodeURIComponent(newSheetId) + 
                    '&sheet_url=' + encodeURIComponent(newSheetUrl);

    // 3. Success HTML response (Mobile & Desktop Popup Compatible)
    var html = '<!DOCTYPE html>' +
    '<html lang="hi">' +
    '<head>' +
    '  <meta charset="UTF-8">' +
    '  <meta name="viewport" content="width=device-width, initial-scale=1.0">' +
    '  <title>Connecting SheetStore Pro...</title>' +
    '  <script src="https://cdn.tailwindcss.com"><\/script>' +
    '  <style>' +
    '    @keyframes pulse-ring {' +
    '      0% { transform: scale(0.95); opacity: 0.8; }' +
    '      50% { transform: scale(1.05); opacity: 1; }' +
    '      100% { transform: scale(0.95); opacity: 0.8; }' +
    '    }' +
    '    .pulse-anim { animation: pulse-ring 2s infinite ease-in-out; }' +
    '  </style>' +
    '</head>' +
    '<body class="bg-slate-950 text-slate-100 flex items-center justify-center min-h-screen p-4 font-sans">' +
    '  <div class="max-w-md w-full bg-slate-900 border border-emerald-500/40 rounded-3xl p-6 sm:p-8 shadow-2xl text-center space-y-6">' +
    '    <div class="relative w-20 h-20 mx-auto flex items-center justify-center">' +
    '      <div class="absolute inset-0 bg-emerald-500/20 rounded-full blur-xl pulse-anim"></div>' +
    '      <div class="w-16 h-16 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-400 flex items-center justify-center shadow-lg relative z-10 text-white text-3xl font-black">' +
    '        \u2713' +
    '      </div>' +
    '    </div>' +
    '    <div class="space-y-2">' +
    '      <span class="px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-bold border border-emerald-500/20">' +
    '        100% Personal Drive \u2022 Zero Manual Code' +
    '      </span>' +
    '      <h2 class="text-xl sm:text-2xl font-black text-white">Google Sheet Ban Gayi Hai!</h2>' +
    '      <p class="text-xs sm:text-sm text-slate-300">' +
    '        Aapki Google Sheet aapke <strong class="text-emerald-400">personal Google Drive</strong> me safely create ho chuki hai. Dashboard se auto-connect kiya ja raha hai...' +
    '      </p>' +
    '    </div>' +
    '    <div class="bg-slate-950/80 rounded-2xl p-4 border border-slate-800 text-left text-xs space-y-2">' +
    '      <div class="flex items-center justify-between text-slate-400">' +
    '        <span>Sheet ID:</span>' +
    '        <span class="font-mono text-emerald-400 font-bold">' + newSheetId.substring(0, 12) + '...</span>' +
    '      </div>' +
    '      <div class="flex items-center justify-between text-slate-400">' +
    '        <span>Owner / Storage:</span>' +
    '        <span class="text-white font-medium">Aapka Apna Google Account</span>' +
    '      </div>' +
    '    </div>' +
    '    <div class="pt-2 space-y-3">' +
    '      <a id="returnBtn" href="' + returnUrl + '" target="_top"' +
    '         class="w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-slate-950 font-black text-sm flex items-center justify-center gap-2 shadow-lg shadow-emerald-500/25 transition active:scale-95">' +
    '        <span>Dashboard Par Wapas Jayein \u2192</span>' +
    '      </a>' +
    '      <p class="text-[11px] text-slate-400">Auto-redirect 2 seconds me ho raha hai...</p>' +
    '    </div>' +
    '  </div>' +
    '  <script>' +
    '    (function() {' +
    '      var sheetId = "' + newSheetId + '";' +
    '      var sheetUrl = "' + newSheetUrl + '";' +
    '      var returnUrl = "' + returnUrl + '";' +
    '      try {' +
    '        if (window.opener && !window.opener.closed) {' +
    '          window.opener.postMessage({' +
    '            type: "SHEETSTORE_AUTO_SETUP_COMPLETE",' +
    '            sheetId: sheetId,' +
    '            sheetUrl: sheetUrl' +
    '          }, "*");' +
    '          setTimeout(function() {' +
    '            try { window.close(); } catch(e) {}' +
    '          }, 1200);' +
    '        }' +
    '      } catch(err) {' +
    '        console.warn("Opener postMessage error:", err);' +
    '      }' +
    '      setTimeout(function() {' +
    '        try {' +
    '          var btn = document.getElementById("returnBtn");' +
    '          if (btn) { btn.click(); } else { window.top.location.href = returnUrl; }' +
    '        } catch(e) {' +
    '          window.top.location.href = returnUrl;' +
    '        }' +
    '      }, 1800);' +
    '    })();' +
    '  <\/script>' +
    '</body>' +
    '</html>';

    return HtmlService.createHtmlOutput(html)
      .setTitle("SheetStore Google Connect")
      .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL)
      .addMetaTag('viewport', 'width=device-width, initial-scale=1');

  } catch (error) {
    var errorHtml = '<!DOCTYPE html>' +
    '<html lang="hi">' +
    '<head>' +
    '  <meta charset="UTF-8">' +
    '  <meta name="viewport" content="width=device-width, initial-scale=1">' +
    '  <title>Connection Error</title>' +
    '  <script src="https://cdn.tailwindcss.com"><\/script>' +
    '</head>' +
    '<body class="bg-slate-950 text-slate-100 flex items-center justify-center min-h-screen p-4 font-sans">' +
    '  <div class="max-w-md w-full bg-slate-900 border border-red-500/40 rounded-3xl p-6 text-center space-y-4">' +
    '    <div class="w-16 h-16 rounded-2xl bg-red-500/20 text-red-400 border border-red-500/30 mx-auto flex items-center justify-center text-3xl font-black">' +
    '      \u2715' +
    '    </div>' +
    '    <h2 class="text-xl font-bold text-white">Sheet Copy Nahi Ho Payi</h2>' +
    '    <p class="text-xs text-red-300 font-mono bg-red-950/60 p-3 rounded-xl border border-red-500/30 text-left overflow-auto max-h-32">' +
    '      ' + error.toString() +
    '    </p>' +
    '    <p class="text-xs text-slate-400 leading-relaxed">' +
    '      Kripya ensure karein ki aapka Google Drive storage full na ho aur Google account permissions allow ki gayi ho.' +
    '    </p>' +
    '    <a href="' + redirectUrl + '" target="_top" class="inline-block py-2.5 px-6 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-bold text-xs">' +
    '      Wapas Dashboard Jayein' +
    '    </a>' +
    '  </div>' +
    '</body>' +
    '</html>';
    return HtmlService.createHtmlOutput(errorHtml)
      .setTitle("SheetStore Connection Error")
      .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL)
      .addMetaTag('viewport', 'width=device-width, initial-scale=1');
  }
}

