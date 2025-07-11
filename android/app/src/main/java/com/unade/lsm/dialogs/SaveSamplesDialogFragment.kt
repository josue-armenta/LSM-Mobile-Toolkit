package com.unade.lsm.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.unade.lsm.R

class SaveSamplesDialogFragment(private val callback: () -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.dialog_save_samples)
                .setMessage(R.string.dialog_save_samples_message)
                .setPositiveButton(R.string.save) { _, _ -> callback() }
                .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}