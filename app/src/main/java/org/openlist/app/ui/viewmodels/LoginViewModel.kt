package org.openlist.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.openlist.app.data.repository.OpenListRepository
import org.openlist.app.data.repository.PreferencesRepository
import org.openlist.app.data.repository.Result
import javax.inject.Inject

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isCheckingSession: Boolean = true
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: OpenListRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            val isLoggedIn = preferencesRepository.getIsLoggedIn()
            val serverUrl = preferencesRepository.getServerUrl()
            val username = preferencesRepository.getUsername()

            _uiState.update {
                it.copy(
                    isCheckingSession = false,
                    isLoggedIn = isLoggedIn,
                    serverUrl = serverUrl,
                    username = username
                )
            }
        }
    }

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, error = null) }
    }

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "请填写所有字段") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.login(state.serverUrl, state.username, state.password)) {
                is Result.Success -> {
                    if (result.data.code == 0) {
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = result.data.message) }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update {
                LoginUiState(isCheckingSession = false, isLoggedIn = false)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
