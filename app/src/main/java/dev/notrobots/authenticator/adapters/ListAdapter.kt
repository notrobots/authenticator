package dev.notrobots.authenticator.adapters

import android.content.Context
import android.widget.ArrayAdapter

class ListAdapter<T>(context: Context, objects: List<T>) : ArrayAdapter<T>(
    context,
    android.R.layout.simple_list_item_1,
    objects
)
