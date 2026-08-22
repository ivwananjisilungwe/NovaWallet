package com.novawallet.novawallet_app

import io.flutter.embedding.android.FlutterFragmentActivity

/// local_auth (biometric unlock) requires FlutterFragmentActivity:
/// AndroidX BiometricPrompt can only be attached to a FragmentActivity.
class MainActivity : FlutterFragmentActivity()
