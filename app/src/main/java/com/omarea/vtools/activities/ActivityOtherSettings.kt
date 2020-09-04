package com.omarea.vtools.activities

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Switch
import androidx.core.content.PermissionChecker
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.shell_utils.AppErrorLogcatUtils
import com.omarea.store.SpfConfig
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_other_settings.*

class ActivityOtherSettings : ActivityBase() {
    private lateinit var spf: SharedPreferences
    private var myHandler = Handler(Looper.getMainLooper())

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()

        settings_disable_selinux.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)
        settings_theme_wallpaper.isChecked = spf.getInt(SpfConfig.GLOBAL_SPF_THEME, 1) == 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_settings)

        setBackArrow()

        settings_disable_selinux.setOnClickListener {
            if (settings_disable_selinux.isChecked) {
                KeepShellPublic.doCmdSync(CommonCmds.DisableSELinux)
                myHandler.postDelayed({
                    spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, settings_disable_selinux.isChecked).apply()
                }, 10000)
            } else {
                KeepShellPublic.doCmdSync(CommonCmds.ResumeSELinux)
                spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, settings_disable_selinux.isChecked).apply()
            }
        }
        settings_logcat.setOnClickListener {
            val log = AppErrorLogcatUtils().catLogInfo()
            settings_log_content.visibility = View.VISIBLE
            settings_log_content.setText(log)
            settings_log_content.setSelection(0, log.length)
        }

        settings_debug_layer.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_SCENE_LOG, false)
        settings_debug_layer.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_SCENE_LOG, (it as Switch).isChecked).apply()
        }

        settings_classic_mode.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_SCENE_CLASSIC, false)
        settings_classic_mode.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_SCENE_CLASSIC, (it as Switch).isChecked).apply()
        }

        settings_auto_exit.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_EXIT, true)
        settings_auto_exit.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_EXIT, (it as Switch).isChecked).apply()
        }
    }

    private fun checkPermission(context: Context, permission: String): Boolean = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED

    private fun hasRWPermission(): Boolean {
        return checkPermission(this.applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                &&
                checkPermission(this.applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun onThemeClick(view: View) {
        val tag = view.tag.toString().toInt()
        if (tag == 10 && spf.getInt(SpfConfig.GLOBAL_SPF_THEME, 1) == 10) {
            spf.edit().putInt(SpfConfig.GLOBAL_SPF_THEME, 1).apply()
            this.recreate()
        } else {
            if (tag == 10 && !hasRWPermission()) {
                DialogHelper.helpInfo(view.context, "", getString(R.string.wallpaper_rw_permission))
                (view as Switch).isChecked = false
            } else {
                spf.edit().putInt(SpfConfig.GLOBAL_SPF_THEME, tag).apply()
                this.recreate()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, settings_disable_selinux.isChecked).apply()
    }

    public override fun onPause() {
        super.onPause()
    }
}
