package dev.notrobots.authenticator.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import java.io.Serializable

//data class AccountWithTags(
//    @Embedded
//    val account: Account,
//    @Relation(
//        parentColumn = "accountId",
//        entityColumn = "tagId",
//        associateBy = Junction(AccountTagCrossRef::class)
//    )
//    val tags: List<Tag>
//) : Serializable