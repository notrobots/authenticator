package dev.notrobots.authenticator.models

import androidx.room.*
import androidx.room.ForeignKey.CASCADE

@Entity(
    primaryKeys = ["accountId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["accountId"],
            childColumns = ["accountId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = CASCADE
        )
    ]
)
data class AccountTagCrossRef(
    val accountId: Long,
    val tagId: Long
)

data class AccountWithTags(
    @Embedded
    val account: Account,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "tagId",
        associateBy = Junction(AccountTagCrossRef::class),

        )
    val tags: List<Tag>
)

data class TagWithAccounts(
    @Embedded
    val tag: Tag,
    @Relation(
        parentColumn = "tagId",
        entityColumn = "accountId",
        associateBy = Junction(AccountTagCrossRef::class)
    )
    val accounts: List<Account>
)