package com.goodusestudios.pressbench.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goodusestudios.pressbench.PressBenchViewModel
import com.goodusestudios.pressbench.R
import com.goodusestudios.pressbench.ui.theme.LocalPressBenchPalette

@Composable
fun OnboardingScreen(
    viewModel: PressBenchViewModel,
    t: Translator,
    onOpenLegal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var safety by rememberSaveable { mutableStateOf(false) }
    var terms by rememberSaveable { mutableStateOf(false) }
    val sharedLabelStyle = MaterialTheme.typography.labelLarge

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val expandedWidth = maxWidth >= 600.dp
        val compactHeight = maxHeight < 700.dp

        val horizontalPadding = if (expandedWidth) 32.dp else 24.dp
        val topPadding = when {
            compactHeight -> 12.dp
            expandedWidth -> 40.dp
            else -> 24.dp
        }
        val bottomPadding = if (compactHeight) 20.dp else 24.dp
        val logoSize = when {
            compactHeight -> 72.dp
            expandedWidth -> 104.dp
            else -> 96.dp
        }
        val logoToHeadline = if (compactHeight) 12.dp else 24.dp
        val headlineToLegal = if (compactHeight) 16.dp else 32.dp
        val legalToChecks = if (compactHeight) 12.dp else 16.dp
        val checksToAction = if (compactHeight) 16.dp else 20.dp

        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = horizontalPadding,
                    top = topPadding,
                    end = horizontalPadding,
                    bottom = bottomPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = LocalPressBenchPalette.current.dark,
            ) {
                Image(
                    painter = painterResource(R.drawable.pressbench_logo_mark),
                    contentDescription = null,
                    modifier = Modifier
                        .size(logoSize)
                        .padding(8.dp),
                )
            }
            Spacer(Modifier.height(logoToHeadline))
            Text(
                text = t("onboarding.welcome.title"),
                style = if (expandedWidth) {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.displaySmall
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(headlineToLegal))
            NativeSurface(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    LegalLink(t("common.privacyPolicy"), sharedLabelStyle) {
                        onOpenLegal("privacy")
                    }
                    LegalLink(t("common.termsOfUse"), sharedLabelStyle) {
                        onOpenLegal("terms")
                    }
                    LegalLink(t("common.safetyNotice"), sharedLabelStyle) {
                        onOpenLegal("safety")
                    }
                }
            }
            Spacer(Modifier.height(legalToChecks))
            NativeSurface(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    CheckRow(
                        text = t("onboarding.legal.ackSafety"),
                        checked = safety,
                        style = sharedLabelStyle,
                        onChecked = { safety = it },
                    )
                    CheckRow(
                        text = t("onboarding.legal.acceptTerms"),
                        checked = terms,
                        style = sharedLabelStyle,
                        onChecked = { terms = it },
                    )
                }
            }
            Spacer(Modifier.height(checksToAction))
            PrimaryAction(
                text = t("onboarding.openApp"),
                onClick = viewModel::finishOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                enabled = safety && terms,
            )
        }
    }
}

@Composable
private fun LegalLink(
    text: String,
    style: TextStyle,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(
            text = text,
            style = style,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CheckRow(
    text: String,
    checked: Boolean,
    style: TextStyle,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onChecked,
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
        )
        Text(
            text = text,
            style = style,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
        )
    }
}
