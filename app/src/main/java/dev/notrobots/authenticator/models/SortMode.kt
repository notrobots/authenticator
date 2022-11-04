package dev.notrobots.authenticator.models

enum class SortMode(
    val sortingDirection: Int,
    val sortingBy: Int
) {
    Custom(-1, -1),
    NameAscending(SortMode.SORT_ASC, SortMode.ORDER_BY_NAME),
    NameDescending(SortMode.SORT_DESC, SortMode.ORDER_BY_NAME),
    LabelAscending(SortMode.SORT_ASC, SortMode.ORDER_BY_LABEL),
    LabelDescending(SortMode.SORT_DESC, SortMode.ORDER_BY_LABEL),
    IssuerAscending(SortMode.SORT_ASC, SortMode.ORDER_BY_ISSUER),
    IssuerDescending(SortMode.SORT_DESC, SortMode.ORDER_BY_ISSUER),

    //TODO: Remove the tag sorting
    @Deprecated("Use filter or search function")
    TagAscending(SortMode.SORT_ASC, -1),

    @Deprecated("Use filter or search function")
    TagDescending(SortMode.SORT_DESC, -1);

    companion object {
        const val SORT_ASC = 0
        const val SORT_DESC = 1
        const val ORDER_BY_NAME = 0
        const val ORDER_BY_LABEL = 1
        const val ORDER_BY_ISSUER = 2
    }
}