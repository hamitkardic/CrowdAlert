package com.example.crowdalert.ui.settings

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crowdalert.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    currentThemeMode: AppThemeMode,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    var selectedLocaleTag by remember { mutableStateOf(AppSettings.activeLocaleTag(context)) }
    var isLanguageExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_to_map),
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LanguageSelector(
                selectedLocaleTag = selectedLocaleTag,
                expanded = isLanguageExpanded,
                onExpandedChange = { isLanguageExpanded = it },
                onLocaleSelected = { localeTag ->
                    selectedLocaleTag = localeTag
                    isLanguageExpanded = false
                    AppSettings.setLocaleTag(context, localeTag)
                    (context as? Activity)?.recreate()
                },
            )

            HorizontalDivider()

            SettingsSectionHeader(text = stringResource(R.string.settings_theme_header))
            ThemeOption(
                label = stringResource(R.string.settings_theme_light),
                selected = currentThemeMode == AppThemeMode.Light,
                onClick = {
                    AppSettings.setThemeMode(context, AppThemeMode.Light)
                    onThemeModeSelected(AppThemeMode.Light)
                },
            )
            ThemeOption(
                label = stringResource(R.string.settings_theme_dark),
                selected = currentThemeMode == AppThemeMode.Dark,
                onClick = {
                    AppSettings.setThemeMode(context, AppThemeMode.Dark)
                    onThemeModeSelected(AppThemeMode.Dark)
                },
            )
            ThemeOption(
                label = stringResource(R.string.settings_theme_device),
                selected = currentThemeMode == AppThemeMode.Device,
                onClick = {
                    AppSettings.setThemeMode(context, AppThemeMode.Device)
                    onThemeModeSelected(AppThemeMode.Device)
                },
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text(stringResource(R.string.auth_sign_out))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SelectableSettingsRow(
        label = label,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun LanguageSelector(
    selectedLocaleTag: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLocaleSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_language_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = languageOptions.selectedLabel(selectedLocaleTag),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector =
                    if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                languageOptions.forEach { option ->
                    LanguageDropdownOption(
                        label = stringResource(option.labelRes),
                        selected = selectedLocaleTag == option.localeTag,
                        onClick = { onLocaleSelected(option.localeTag) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageDropdownOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SelectableSettingsRow(
        label = label,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun SelectableSettingsRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun List<LanguageOption>.selectedLabel(localeTag: String): String =
    stringResource(
        firstOrNull { it.localeTag == localeTag }?.labelRes
            ?: R.string.settings_language_english,
    )

private data class LanguageOption(
    val localeTag: String,
    @param:StringRes val labelRes: Int,
)

private val languageOptions =
    listOf(
        LanguageOption("en", R.string.settings_language_english),
        LanguageOption("de", R.string.settings_language_german),
        LanguageOption("pl", R.string.settings_language_polish),
        LanguageOption("fr", R.string.settings_language_french),
        LanguageOption("tr", R.string.settings_language_turkish),
    )
