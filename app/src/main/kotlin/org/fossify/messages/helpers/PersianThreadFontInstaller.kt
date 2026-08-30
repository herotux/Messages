package org.fossify.messages.helpers

import android.app.Activity
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import org.fossify.messages.R

object PersianThreadFontInstaller {
    private const val TAG_OFFSET = 2

    fun install(activity: Activity) {
        if (!BankAccountsFeature.isPersian(activity)) return
        val font = runCatching { ResourcesCompat.getFont(activity, R.font.vazirmatn_regular) }.getOrNull() ?: return
        val list = activity.findViewById<RecyclerView>(R.id.thread_messages_list) ?: return
        val tagKey = R.id.bank_card_feature_tag + TAG_OFFSET
        if (list.getTag(tagKey) == true) {
            applyChildren(list, font)
            return
        }

        list.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                applyFontTree(view, font)
            }

            override fun onChildViewDetachedFromWindow(view: View) = Unit
        })

        list.adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = repost()
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = repost()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = repost()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = repost()
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = repost()

            private fun repost() {
                list.post { applyChildren(list, font) }
            }
        })
        list.setTag(tagKey, true)
        applyChildren(list, font)
    }

    private fun applyChildren(list: RecyclerView, font: Typeface) {
        for (i in 0 until list.childCount) {
            applyFontTree(list.getChildAt(i), font)
        }
    }

    private fun applyFontTree(root: View, font: Typeface) {
        if (root is TextView) {
            root.typeface = font
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                applyFontTree(root.getChildAt(i), font)
            }
        }
    }
}
