package com.example.meditrack.data

import androidx.room.TypeConverter
import java.sql.Time

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Time? {
        return value?.let { Time(it) }
    }

    @TypeConverter
    fun dateToTimestamp(time: Time?): Long? {
        return time?.time
    }
}
