package dev.notrobots.authenticator.ui.taglist

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.authenticator.db.TagDao
import javax.inject.Inject

@HiltViewModel
class TagListViewModel @Inject constructor(
    val tagDao: TagDao
) : ViewModel() {
    val tags = tagDao.getTagsLive()
}