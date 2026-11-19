package io.github.samolego.canta.core

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.app_list_export_failed
import io.github.samolego.canta.generated.resources.app_list_exported
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

class AndroidCantaPlatform(
    private val context: Context,
) : CantaPlatform {

    companion object {
        private const val TAG = "AndroidCantaPlatform"
    }

    @Volatile
    private var biometricActivity: FragmentActivity? = null

    private val exportScope = CoroutineScope(Dispatchers.Main)
    private var appListLauncher: ActivityResultLauncher<String>? = null
    private var pendingAppList: String? = null

    override val supportsAppListExport = true

    /** Attach the activity from `onCreate`, before it reaches STARTED. */
    fun attachActivity(activity: FragmentActivity) {
        biometricActivity = activity
        appListLauncher = activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/markdown"),
        ) { uri ->
            val markdown = pendingAppList
            pendingAppList = null
            if (uri != null && markdown != null) {
                exportScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val output = checkNotNull(context.contentResolver.openOutputStream(uri))
                            output.bufferedWriter(Charsets.UTF_8).use { it.write(markdown) }
                        }
                    }
                    result.exceptionOrNull()?.let { LogUtils.e(TAG, "Failed to save app list", it) }
                    val message = if (result.isSuccess) {
                        Res.string.app_list_exported
                    } else {
                        Res.string.app_list_export_failed
                    }
                    showMessage(getString(message))
                }
            }
        }
    }

    /** Detach the activity when it is destroyed. */
    fun detachActivity(activity: FragmentActivity) {
        if (biometricActivity === activity) {
            biometricActivity = null
            appListLauncher = null
        }
    }

    override fun saveAppList(markdown: String) {
        val launcher = appListLauncher ?: return
        if (pendingAppList != null) return
        pendingAppList = markdown
        try {
            launcher.launch("Applist.md")
        } catch (e: ActivityNotFoundException) {
            pendingAppList = null
            LogUtils.e(TAG, "No document picker available", e)
            exportScope.launch { showMessage(getString(Res.string.app_list_export_failed)) }
        }
    }

    override fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun requireBiometric(title: String, subtitle: String, onSuccess: () -> Unit) {
        val activity = biometricActivity
        if (activity == null) {
            // Fail closed: skipping authentication would bypass the setting.
            LogUtils.e(TAG, "No FragmentActivity attached; cannot authenticate")
            return
        }
        val executor = ContextCompat.getMainExecutor(context)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    LogUtils.e(
                        TAG,
                        "Biometric error. Code: $errorCode, message: $errString"
                    )
                }

                override fun onAuthenticationFailed() {
                    LogUtils.w(TAG, "Biometric authentication failed!")
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    override fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Canta", text))
    }

    override fun readClipboard(): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).text?.toString()
    }
}