package com.example.data

import androidx.room.*

@Entity(tableName = "usage_stats")
data class StatsEntity(
    @PrimaryKey val actionName: String,
    val count: Int = 0
)
