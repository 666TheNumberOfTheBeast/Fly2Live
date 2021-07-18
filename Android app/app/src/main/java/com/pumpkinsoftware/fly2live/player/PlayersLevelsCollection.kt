package com.pumpkinsoftware.fly2live.player

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import org.bson.types.ObjectId

// MongoDB Realm object schema
open class PlayersLevelsCollection(
    // To work with Realm Sync, the data model must have a primary key field called _id
    @PrimaryKey
    var _id: String = "", // Player ID
    @Required
    var _partition: String = "", // Partition for read & write permissions
    var player_level: Int = 1,
    var player_xp: Long = 0
): RealmObject() {}