package com.wamad.petaljong.presentation

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.widget.EditText
import android.text.InputType
import com.wamad.petaljong.domain.*

/** Separate Activity and controller: no preview object is ever installed in GameStore. */
class LevelPreviewActivity : MainActivity() {
    internal var controller: ProgressController? = null
    override fun onRetainNonConfigurationInstance(): Any? = controller
}

internal object DeveloperPreview {
    private const val EXTRA = "dev_preview_level"
    fun level(activity: MainActivity) = activity.intent.getLongExtra(EXTRA, 0L)
    fun active(activity: MainActivity) = activity is LevelPreviewActivity
    fun install(activity: MainActivity, card: View, opening: () -> Unit) {
        if (active(activity)) return
        card.setOnLongClickListener {
            val input = EditText(activity).apply {
                hint = "Preview level"
                inputType = InputType.TYPE_CLASS_NUMBER
                contentDescription = hint
            }
            val dialog = AlertDialog.Builder(activity).setTitle("Preview level").setView(input)
                .setPositiveButton("Open Preview", null).setNegativeButton("Cancel", null).create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val level = input.text.toString().toLongOrNull()
                    if (level == null || level <= 0) input.error = "Enter a positive level number"
                    else {
                        opening()
                        dialog.dismiss()
                        activity.startActivity(Intent(activity, LevelPreviewActivity::class.java).putExtra(EXTRA, level))
                    }
                }
            }
            dialog.show()
            true
        }
    }
    /** Called on MainActivity's background loader; production bounds and validation apply. */
    fun load(activity: MainActivity): ProgressController {
        activity as LevelPreviewActivity
        (activity.lastNonConfigurationInstance as? ProgressController)?.let { activity.controller = it; return it }
        val target = level(activity)
        require(target > 0) { "Enter a positive level number" }
        return createPreviewSession(target).also { activity.controller = it }
    }
}
