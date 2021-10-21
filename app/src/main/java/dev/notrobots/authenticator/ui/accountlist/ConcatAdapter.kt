package dev.notrobots.authenticator.ui.accountlist

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.notrobots.androidstuff.util.error
import dev.notrobots.androidstuff.util.swap
import java.util.*

val ConcatAdapter.selectedAccounts
    get() = (adapters as List<AccountListAdapter>).flatMap { it.selectedAccounts }

val ConcatAdapter.selectedGroups
    get() = groups.filter { it.isSelected }

val ConcatAdapter.accounts
    get() = groupsWithAccounts.flatMap { it.accounts }

val ConcatAdapter.groups
    get() = groupsWithAccounts.map { it.group }

val ConcatAdapter.groupsWithAccounts
    get() = (adapters as List<AccountListAdapter>).map { it.groupWithAccounts }

val ConcatAdapter.editMode
    get() = if (adapters.isNotEmpty()) (adapters.first() as AccountListAdapter).editMode else AccountListAdapter.EditMode.Disabled

var ConcatAdapter.showPins
    get() = if (adapters.isNotEmpty()) (adapters.first() as AccountListAdapter).showPins else true
    set(value) {
        for (adapter in adapters) {
            adapter as AccountListAdapter
            adapter.showPins = value
        }
    }

fun ConcatAdapter.clearSelectedAccounts() {
    for (adapter in adapters) {
        adapter as AccountListAdapter
        adapter.clearSelected()
    }
}

fun ConcatAdapter.clearSelectedGroups() {
    for (group in groups) {
        group.isSelected = false
    }
    notifyAllDataSetChanged()
}

fun ConcatAdapter.setEditMode(editMode: AccountListAdapter.EditMode) {
    for (adapter in adapters) {
        adapter as AccountListAdapter
        adapter.editMode = editMode
    }
}

fun ConcatAdapter.clearAdapters() {
    for (adapter in adapters) {
        removeAdapter(adapter)
    }
}

fun ConcatAdapter.addAllAdapters(adapters: List<RecyclerView.Adapter<*>>) {
    for (adapter in adapters) {
        addAdapter(adapter)
    }
}

fun ConcatAdapter.swap(from: Int, to: Int) {
    getMutableAdapters()?.let {
        Collections.swap(it, from, to)

        if (from != to) {
            swap(groups[from], groups[to], { it.order }, { t, v -> t.order = v })
        }

        notifyItemMoved(from, to)
    }
}

fun ConcatAdapter.getMutableAdapters(): List<*>? {
    // Since the adapters field is an immutable list, we try and get the private field and use that instead
    // The field is in ConcatAdapter -> mController(ConcatAdapterController) -> mWrappers(List<NestedAdapterWrapper>)
    // This is an hack and it should be replaced with the public APIs
    val concatAdapterControllerClass = Class.forName("androidx.recyclerview.widget.ConcatAdapterController")
    val controllerField = ConcatAdapter::class.java.declaredFields.find { it.name == "mController" }
    val wrappersField = concatAdapterControllerClass.declaredFields.find { it.name == "mWrappers" }

    controllerField?.isAccessible = true
    wrappersField?.isAccessible = true

    val controller = controllerField?.get(this)

    return wrappersField?.get(controller) as? List<*>
}

fun ConcatAdapter.notifyAllDataSetChanged() {
    for (adapter in adapters) {
        adapter.notifyDataSetChanged()
    }
}

fun ConcatAdapter.selectAllGroups() {
    for (group in groups) {
        if (!group.isDefault) {
            group.isSelected = true
        }
    }
    for (adapter in adapters) {
        adapter.notifyItemChanged(0)
    }
}