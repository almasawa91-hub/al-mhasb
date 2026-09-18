import 'package:flutter/material.dart';

/// ثوابت التطبيق العامة - المحاسب الذكي
class AppConstants {
  AppConstants._();

  // معلومات التطبيق
  static const String appName = 'المحاسب الذكي';
  static const String appNameEn = 'AL MUHASIB AL THAKI';
  static const String appVersion = '1.0.0';
  static const String appBuildNumber = '1';

  // إعدادات قاعدة البيانات
  static const String dbName = 'al_muhasib.db';
  static const int dbVersion = 1;

  // إعدادات اللغة والاتجاه
  static const String defaultLocale = 'ar';
  static const String fontFamily = 'Cairo';

  // الألوان الأساسية وفقًا للمواصفات
  static const Color primaryColor = Color(0xFF1565C0); // أزرق رئيسي #1565C0
  static const Color primaryDarkColor = Color(0xFF0D47A1);
  static const Color primaryLightColor = Color(0xFF1E88E5);
  static const Color secondaryColor = Color(0xFF00897B); // أخضر التركواز الثانوي
  static const Color accentColor = Color(0xFF0288D1);

  // ألوان الخلفيات والأسطح
  static const Color backgroundColor = Color(0xFFFFFFFF); // أبيض ناصع #FFFFFF
  static const Color surfaceColor = Color(0xFFF8F9FA);
  static const Color cardColor = Color(0xFFFFFFFF);
  static const Color dividerColor = Color(0xFFE0E0E0);
  static const Color borderColor = Color(0xFFCFD8DC);

  // ألوان الحالات
  static const Color successColor = Color(0xFF2E7D32);
  static const Color errorColor = Color(0xFFD32F2F);
  static const Color warningColor = Color(0xFFF57F17);
  static const Color infoColor = Color(0xFF0288D1);

  // ألوان النصوص
  static const Color textPrimaryColor = Color(0xFF212121);
  static const Color textSecondaryColor = Color(0xFF757575);
  static const Color textLightColor = Color(0xFFBDBDBD);
  static const Color textOnPrimary = Color(0xFFFFFFFF);

  // المقاسات والمسافات (Spacing System)
  static const double spaceXS = 4.0;
  static const double spaceS = 8.0;
  static const double spaceM = 16.0;
  static const double spaceL = 24.0;
  static const double spaceXL = 32.0;

  // الحواف الدائرية (Border Radius)
  static const double radiusS = 6.0;
  static const double radiusM = 10.0;
  static const double radiusL = 16.0;

  // مسار التوجيه (Routes)
  static const String routeDashboard = '/';
  static const String routeSales = '/sales';
  static const String routeSalesList = '/sales/list';
  static const String routePurchases = '/purchases';
  static const String routePurchasesList = '/purchases/list';
  static const String routeVouchers = '/vouchers';
  static const String routeVouchersList = '/vouchers/list';
  static const String routeCustomers = '/customers';
  static const String routeAddCustomer = '/customers/add';
  static const String routeSuppliers = '/suppliers';
  static const String routeAddSupplier = '/suppliers/add';
  static const String routeItems = '/items';
  static const String routeAddItem = '/items/add';
  static const String routeWarehouses = '/warehouses';
  static const String routeAddWarehouse = '/warehouses/add';
  static const String routeAccounts = '/accounts';
  static const String routeCashboxes = '/cashboxes';
  static const String routeAddCashbox = '/cashboxes/add';
  static const String routeCurrencies = '/currencies';
  static const String routeAddCurrency = '/currencies/add';
  static const String routeReports = '/reports';
  static const String routeSettings = '/settings';
  static const String routeBackup = '/backup';
}

/// ثوابت النصوص الثابتة للواجهة (UI Labels فقط - لا بيانات وهمية)
class AppStrings {
  AppStrings._();

  static const String noData = 'لا توجد بيانات';
  static const String save = 'حفظ';
  static const String cancel = 'إلغاء';
  static const String delete = 'حذف';
  static const String edit = 'تعديل';
  static const String search = 'بحث';
  static const String add = 'إضافة';
  static const String confirm = 'تأكيد';
  static const String retry = 'إعادة المحاولة';
  static const String loading = 'جاري التحميل...';
  static const String errorOccurred = 'حدث خطأ أثناء العملية';

  static const String sales = 'المبيعات';
  static const String purchases = 'المشتريات';
  static const String vouchers = 'قبض/صرف';
  static const String accounts = 'الحسابات';
  static const String customers = 'العملاء';
  static const String suppliers = 'الموردون';
  static const String items = 'الأصناف';
  static const String warehouses = 'المخازن';
  static const String cashboxes = 'الصناديق';
  static const String currencies = 'العملات';
  static const String reports = 'التقارير';
  static const String settings = 'الإعدادات';
}
