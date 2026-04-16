package com.example.intelligent_messaging_app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intelligent_messaging_app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.isLoggedIn.collect {
                _isLoggedIn.value = it
            }
        }
    }

    fun onNameChanged(newName: String) {
        _name.value = newName
    }

    fun login() {
        val currentName = _name.value.trim()
        if (currentName.isNotEmpty()) {
            viewModelScope.launch {
                userPreferencesRepository.saveUserName(currentName)
            }
        }
    }
}
