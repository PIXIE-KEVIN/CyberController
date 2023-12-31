package com.omarea.vtools.activities

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Switch
import androidx.core.content.PermissionChecker
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.shell_utils.AppErrorLogcatUtils
import com.omarea.store.SpfConfig
import com.omarea.utils.CommonCmds
import java.util.Locale
import android.content.res.Configuration
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

        // Obtener el array de IDs de los idiomas desde configs.xml
           val languageIds = resources.getStringArray(R.array.languages)

// Crear una lista para almacenar los nombres de los idiomas
val languageNames = ArrayList<String>()

// Iterar sobre los IDs de los idiomas y obtener los nombres correspondientes
for (languageId in languageIds) {
    val resourceId = resources.getIdentifier(languageId.toLowerCase(Locale.ROOT), "string", packageName)
    if (resourceId != 0) {
        val languageName = getString(resourceId)
        languageNames.add(languageName)
    }
}

// Utilizar los nombres de los idiomas en lugar de los IDs definidos en el archivo de recursos
           val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames.toTypedArray())
             adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            val spinnerLanguage = findViewById<Spinner>(R.id.spinnerLanguage)
              spinnerLanguage.adapter = adapter

              spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
              val selectedLanguage = languageIds[position]

        // Configurar el Locale según el idioma seleccionado
            val locale = when (selectedLanguage) {
            "Español" -> Locale("es")
            // Agrega más casos según los idiomas que tengas en el array
            else -> Locale.getDefault() // Idioma predeterminado si no coincide con ninguno
        }

        // Actualizar la configuración del idioma
            val config = Configuration(resources.configuration)
              config.setLocale(locale)
              resources.updateConfiguration(config, resources.displayMetrics)

        // Reiniciar la actividad para aplicar el cambio de idioma
              recreate()
    }

         override fun onNothingSelected(p0: AdapterView<*>?) {
                // No es necesario hacer nada aquí
            }
        }
        // Fin de añadir Spinner

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

            EventBus.publish(EventType.SERVICE_DEBUG)
        }

        settings_help_icon.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_HELP_ICON, true)
        settings_help_icon.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_HELP_ICON, (it as Switch).isChecked).apply()
        }

        settings_auto_exit.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_EXIT, true)
        settings_auto_exit.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_EXIT, (it as Switch).isChecked).apply()
        }

        settings_black_notification.isChecked = spf.getBoolean(SpfConfig.GLOBAL_NIGHT_BLACK_NOTIFICATION, false)
        settings_black_notification.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_NIGHT_BLACK_NOTIFICATION, (it as Switch).isChecked).apply()
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
            spf.edit().remove(SpfConfig.GLOBAL_SPF_THEME).apply()
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