package org.fossify.messages.helpers

import android.content.Context
import android.view.ActionProvider
import android.view.MenuItem
import android.widget.Toast

class DebugLogExportActionProvider(context: Context) : ActionProvider(context) {
    override fun onCreateActionView(): android.view.View? = null

    override fun onPerformDefaultAction(): Boolean {
        val exported = DebugLog.exportToDownloads(context)
        Toast.makeText(
            context,
            if (exported) "Debug log exported to Downloads/messages_debug.log"
            else "No debug log available to export",
            Toast.LENGTH_LONG
        ).show()
        return true
    }
}
