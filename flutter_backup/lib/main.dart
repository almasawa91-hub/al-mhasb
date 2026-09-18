import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';
import 'constants.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const AlMuhasibAlThakiApp());
}

/// المزود الأساسي لإدارة حالة إعدادات التطبيق في المرحلة الأولى
class AppSettingsState extends ChangeNotifier {
  bool _isInitialized = false;

  bool get isInitialized => _isInitialized;

  void initializeApp() {
    _isInitialized = true;
    notifyListeners();
  }
}

/// التطبيق الرئيسي - المحاسب الذكي
class AlMuhasibAlThakiApp extends StatelessWidget {
  const AlMuhasibAlThakiApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AppSettingsState()..initializeApp()),
      ],
      child: MaterialApp(
        title: AppConstants.appName,
        debugShowCheckedModeBanner: false,
        locale: const Locale(AppConstants.defaultLocale),
        supportedLocales: const [
          Locale('ar'),
        ],
        localizationsDelegates: const [
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        theme: ThemeData(
          useMaterial3: true,
          fontFamily: AppConstants.fontFamily,
          colorScheme: ColorScheme.fromSeed(
            seedColor: AppConstants.primaryColor,
            primary: AppConstants.primaryColor,
            secondary: AppConstants.secondaryColor,
            surface: AppConstants.surfaceColor,
            background: AppConstants.backgroundColor,
            error: AppConstants.errorColor,
          ),
          scaffoldBackgroundColor: AppConstants.backgroundColor,
          appBarTheme: const AppBarTheme(
            backgroundColor: AppConstants.primaryColor,
            foregroundColor: AppConstants.textOnPrimary,
            elevation: 2,
            centerTitle: true,
            titleTextStyle: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: AppConstants.textOnPrimary,
              fontFamily: AppConstants.fontFamily,
            ),
          ),
          cardTheme: const CardTheme(
            color: AppConstants.cardColor,
            elevation: 1,
            margin: EdgeInsets.all(AppConstants.spaceS),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.all(Radius.circular(AppConstants.radiusM)),
              side: BorderSide(color: AppConstants.dividerColor, width: 0.8),
            ),
          ),
          elevatedButtonTheme: ElevatedButtonThemeData(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppConstants.primaryColor,
              foregroundColor: AppConstants.textOnPrimary,
              minimumSize: const Size(48, 48),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppConstants.radiusM),
              ),
              textStyle: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
                fontFamily: AppConstants.fontFamily,
              ),
            ),
          ),
          outlinedButtonTheme: OutlinedButtonThemeData(
            style: OutlinedButton.styleFrom(
              foregroundColor: AppConstants.primaryColor,
              minimumSize: const Size(48, 48),
              side: const BorderSide(color: AppConstants.primaryColor, width: 1.5),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppConstants.radiusM),
              ),
            ),
          ),
          inputDecorationTheme: InputDecorationTheme(
            filled: true,
            fillColor: AppConstants.surfaceColor,
            contentPadding: const EdgeInsets.symmetric(
              horizontal: AppConstants.spaceM,
              vertical: AppConstants.spaceM,
            ),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppConstants.radiusM),
              borderSide: const BorderSide(color: AppConstants.borderColor),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppConstants.radiusM),
              borderSide: const BorderSide(color: AppConstants.borderColor),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppConstants.radiusM),
              borderSide: const BorderSide(color: AppConstants.primaryColor, width: 2),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppConstants.radiusM),
              borderSide: const BorderSide(color: AppConstants.errorColor),
            ),
          ),
        ),
        initialRoute: AppConstants.routeDashboard,
        routes: {
          AppConstants.routeDashboard: (context) => const InitialDashboardScreen(),
        },
      ),
    );
  }
}

/// شاشة البداية المؤقتة للمرحلة الأولى - خالية تمامًا من أي بيانات تجريبية (Empty State)
class InitialDashboardScreen extends StatelessWidget {
  const InitialDashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(AppConstants.appName),
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(AppConstants.spaceL),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.account_balance_wallet_outlined,
                size: 72,
                color: AppConstants.primaryColor.withOpacity(0.8),
              ),
              const SizedBox(height: AppConstants.spaceM),
              const Text(
                AppConstants.appName,
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: AppConstants.textPrimaryColor,
                ),
              ),
              const SizedBox(height: AppConstants.spaceS),
              const Text(
                AppStrings.noData,
                style: TextStyle(
                  fontSize: 16,
                  color: AppConstants.textSecondaryColor,
                ),
              ),
              const SizedBox(height: AppConstants.spaceL),
              const Text(
                'المرحلة الأولى: تم ضبط بيئة العمل والهيكلية بنجاح.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 14,
                  color: AppConstants.textSecondaryColor,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
