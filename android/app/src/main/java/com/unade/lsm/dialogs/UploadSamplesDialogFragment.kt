package com.unade.lsm.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.unade.lsm.R

class UploadSamplesDialogFragment(private val callback: () -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.dialog_upload_samples)
                .setMessage(R.string.dialog_upload_samples_message)
                .setPositiveButton(R.string.sync) { _, _ -> callback() }
                .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}