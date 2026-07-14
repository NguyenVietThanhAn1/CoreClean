package com.coreclean.app.presentation.contact

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coreclean.app.R
import com.coreclean.app.domain.CrashReporter
import com.coreclean.app.domain.model.Contact
import com.coreclean.app.domain.model.ContactDuplicateGroup
import com.coreclean.app.domain.repository.ContactRepository
import com.coreclean.app.domain.usecase.contact.MergeContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactUiState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val allContacts: List<Contact> = emptyList(),
    val duplicates: List<ContactDuplicateGroup> = emptyList(),
    val incomplete: List<Contact> = emptyList(),
    val mergingGroupIndex: Int? = null,
    @StringRes val messageRes: Int? = null,
    val mergedCount: Int = 0
)

@HiltViewModel
class ContactViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ContactRepository,
    private val mergeContacts: MergeContactsUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactUiState())
    val uiState: StateFlow<ContactUiState> = _uiState

    init { load() }

    fun load() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _uiState.value = _uiState.value.copy(isLoading = false, hasPermission = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasPermission = true)
            val all        = repository.getAllContacts()
            val duplicates = repository.findDuplicates()
            val incomplete = repository.findIncomplete()
            _uiState.value = _uiState.value.copy(
                isLoading     = false,
                hasPermission = true,
                allContacts   = all,
                duplicates    = duplicates,
                incomplete    = incomplete
            )
        }
    }

    fun startMerge(groupIndex: Int) {
        _uiState.value = _uiState.value.copy(mergingGroupIndex = groupIndex)
    }

    fun cancelMerge() {
        _uiState.value = _uiState.value.copy(mergingGroupIndex = null)
    }

    fun confirmMerge(contacts: List<Contact>) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(mergingGroupIndex = null)
        val result = mergeContacts.invoke(contacts)
        result.exceptionOrNull()?.let { crashReporter.captureException(it) }
        _uiState.value = _uiState.value.copy(
            messageRes  = if (result.isSuccess) R.string.contact_merge_success else R.string.contact_merge_error,
            mergedCount = contacts.size
        )
        load()
    }

    fun dismissMergeMessage() {
        _uiState.value = _uiState.value.copy(messageRes = null)
    }
}
