package com.unade.lsm.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "samples_file")
data class SamplesFile(
    @PrimaryKey
    val name: String,
    var uploaded: Boolean = false
)