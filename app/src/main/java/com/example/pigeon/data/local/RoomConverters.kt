package com.example.pigeon.data.local

import androidx.room.TypeConverter
import com.example.pigeon.domain.model.EventType

class RoomConverters {
    @TypeConverter
    fun fromEventType(type: EventType): String {
        return type.name
    }

    @TypeConverter
    fun toEventType(value: String): EventType {
        return EventType.valueOf(value)
    }
}
