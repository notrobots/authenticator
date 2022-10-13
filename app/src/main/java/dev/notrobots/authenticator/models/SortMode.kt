package dev.notrobots.authenticator.models

enum class SortMode(val sortingDirection: Int) {
    Custom(-1),
    NameAscending(SortMode.SORT_ASC),
    NameDescending(SortMode.SORT_DESC),
    LabelAscending(SortMode.SORT_ASC),
    LabelDescending(SortMode.SORT_DESC),
    IssuerAscending(SortMode.SORT_ASC),
    IssuerDescending(SortMode.SORT_DESC),
    TagAscending(SortMode.SORT_ASC),
    TagDescending(SortMode.SORT_DESC);

    companion object {
        private const val SORT_ASC = 0
        private const val SORT_DESC = 1
    }
}