package com.goodusestudios.pressbench.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodusestudios.pressbench.model.Peel
import com.goodusestudios.pressbench.R
import com.goodusestudios.pressbench.model.RecipeStage
import com.goodusestudios.pressbench.model.RecipeStageType
import com.goodusestudios.pressbench.model.Setup
import com.goodusestudios.pressbench.model.SetupStatus
import com.goodusestudios.pressbench.model.TemperatureUnit
import com.goodusestudios.pressbench.ui.theme.LocalPressBenchPalette

private val SmallShape = RoundedCornerShape(12.dp)
val CardShape = RoundedCornerShape(18.dp)
val LargeShape = RoundedCornerShape(24.dp)

@Composable
fun NativeSurface(
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.then(if (raised) Modifier.shadow(8.dp, CardShape) else Modifier),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        content = content,
    )
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    val palette = LocalPressBenchPalette.current
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp).clip(shape).background(
            if (enabled) Brush.verticalGradient(listOf(palette.brand2, MaterialTheme.colorScheme.primary))
            else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)),
        ),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        icon?.let { Icon(it, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun TonalAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        icon?.let { Icon(it, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun OutlineAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        icon?.let { Icon(it, null, Modifier.size(19.dp)); Spacer(Modifier.width(7.dp)) }
        Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun PressMachineIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.pressbench_logo_mark),
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun StatusBadge(status: SetupStatus, t: Translator) {
    val palette = LocalPressBenchPalette.current
    val proven = status == SetupStatus.PROVEN
    Surface(
        color = if (proven) palette.successSoft else palette.warmSoft,
        contentColor = if (proven) palette.success else palette.warm,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(if (proven) palette.success else palette.warning))
            Spacer(Modifier.width(5.dp))
            Text(t(if (proven) "status.proven" else "status.trial"), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
fun MetricStrip(
    firstValue: String,
    firstLabel: String,
    secondValue: String,
    secondLabel: String,
    thirdValue: String,
    thirdLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        MetricCell(firstValue, firstLabel, Modifier.weight(1f))
        MetricCell(secondValue, secondLabel, Modifier.weight(1f), good = true)
        MetricCell(thirdValue, thirdLabel, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCell(value: String, label: String, modifier: Modifier, good: Boolean = false) {
    val palette = LocalPressBenchPalette.current
    Surface(
        modifier = modifier.heightIn(min = 78.dp),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(value, modifier = Modifier.fillMaxWidth(), fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.Black, color = if (good) palette.success else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(label, modifier = Modifier.fillMaxWidth(), fontSize = 9.sp, lineHeight = 13.sp, fontWeight = FontWeight.ExtraBold, color = palette.ink3, maxLines = 2, overflow = TextOverflow.Clip, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun FactGrid(setup: Setup, temperatureUnit: TemperatureUnit, locale: String, t: Translator, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FactCard(Icons.Filled.Thermostat, formatTemperature(setup.temperature, temperatureUnit, locale), t("common.temperature"), Modifier.weight(1f))
        FactCard(Icons.Filled.Timer, "${formatNumber(setup.seconds, locale)}s", t("common.durationSeconds"), Modifier.weight(1f))
        FactCard(Icons.Filled.Compress, pressureText(setup.pressure, t), t("common.pressure"), Modifier.weight(1f))
    }
}

@Composable
private fun FactCard(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    val palette = LocalPressBenchPalette.current
    Surface(modifier = modifier.heightIn(min = 92.dp), shape = SmallShape, color = palette.surface2) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(3.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SetupCard(
    setup: Setup,
    temperatureUnit: TemperatureUnit,
    locale: String,
    t: Translator,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFavorite: Boolean = false,
    onFavorite: () -> Unit = {},
) {
    val palette = LocalPressBenchPalette.current
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.width(76.dp).height(84.dp).clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(palette.surface3, palette.warmSoft)))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { SetupArtwork(setup, Modifier.size(70.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(localTitle(setup, t), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${localMaterial(setup, t)} · ${setup.machine}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (showFavorite) {
                            IconButton(onClick = onFavorite, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                                Icon(
                                    if (setup.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    t("setup.favorite"),
                                    tint = if (setup.favorite) palette.warm else palette.ink3,
                                )
                            }
                        }
                        StatusBadge(setup.status, t)
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        SetupFactCell(Icons.Filled.Thermostat, formatTemperature(setup.temperature, temperatureUnit, locale), Modifier.weight(1f))
                        SetupFactCell(Icons.Filled.Timer, "${formatNumber(setup.seconds, locale)}s", Modifier.weight(1f))
                        SetupFactCell(Icons.Filled.Compress, pressureText(setup.pressure, t), Modifier.weight(1f), "●".repeat(setup.pressure.level) + "○".repeat(3 - setup.pressure.level))
                    }
                    if (setup.status == SetupStatus.PROVEN) {
                        Spacer(Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ProofPill(Icons.Filled.Check, formatNumber(setup.provenRuns, locale))
                            ProofPill(Icons.Filled.ContentCopy, formatNumber(setup.provenPieces, locale))
                        }
                    }
                }
            }
            if (!setup.sourceKey.isNullOrBlank() && setup.checkedAt != null) {
                HorizontalDivider(Modifier.padding(top = 9.dp, bottom = 7.dp), color = MaterialTheme.colorScheme.outline)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, null, Modifier.size(15.dp), tint = palette.success)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${t("report.sourceChecked")} · ${formatShortDate(setup.checkedAt, locale)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.ink2,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(palette.surface3), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupFactCell(icon: ImageVector, value: String, modifier: Modifier, dots: String? = null) {
    val palette = LocalPressBenchPalette.current
    Surface(modifier = modifier.heightIn(min = 58.dp), shape = RoundedCornerShape(11.dp), color = palette.surface2, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(value, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, color = palette.dark, textAlign = TextAlign.Center)
                dots?.let { Text(it, fontSize = 10.sp, lineHeight = 11.sp, letterSpacing = 1.sp, maxLines = 1, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun ProofPill(icon: ImageVector, value: String) {
    val palette = LocalPressBenchPalette.current
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(palette.successSoft).padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(13.dp), tint = palette.success)
        Spacer(Modifier.width(3.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = palette.success)
    }
}

@Composable
fun RecipeStrip(setup: Setup, active: Int = -1, complete: Boolean = false, t: Translator, modifier: Modifier = Modifier) {
    val stages = setup.recipeStages()
    val narrow = LocalConfiguration.current.screenWidthDp <= 360
    if (narrow) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            stages.withIndex().chunked(2).forEach { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    group.forEach { (index, stage) -> RecipeStageCell(stage, setup, index, active, complete, t, Modifier.weight(1f)) }
                    if (group.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    } else {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stages.forEachIndexed { index, stage -> RecipeStageCell(stage, setup, index, active, complete, t, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun RecipeStageCell(stage: RecipeStage, setup: Setup, index: Int, active: Int, complete: Boolean, t: Translator, modifier: Modifier) {
    val done = complete || index < active
    val selected = index == active
    val palette = LocalPressBenchPalette.current
    val label = when (stage.type) {
        RecipeStageType.PREPRESS -> t("stage.prepress")
        RecipeStageType.PRESS -> t("stage.press")
        RecipeStageType.PEEL -> t("stage.peel")
        RecipeStageType.POSTPRESS -> "${t("stage.press")} 2"
    }
    val value = if (stage.type == RecipeStageType.PEEL) t("peel.${setup.peel.name.lowercase()}") else stage.value
    Surface(
        modifier = modifier.heightIn(min = 64.dp),
        color = when { done -> palette.successSoft; selected -> MaterialTheme.colorScheme.primaryContainer; else -> palette.surface2 },
        contentColor = when { done -> palette.success; selected -> MaterialTheme.colorScheme.primary; else -> palette.ink2 },
        shape = SmallShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(if (stage.type == RecipeStageType.PEEL) Icons.Filled.Check else Icons.Filled.Timer, null, Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SetupArtwork(setup: Setup, modifier: Modifier = Modifier) {
    val palette = LocalPressBenchPalette.current
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier.aspectRatio(1f)) {
        drawCircle(Color.White.copy(alpha = .72f), radius = size.minDimension * .41f, center = center)
        val fill = if (setup.transfer == "HTV") palette.dark else primary
        val accent = if (setup.transfer == "HTV") palette.warm else palette.brand3
        val s = size.minDimension / 82f
        val shirt = Path().apply {
            moveTo(21*s,24*s); lineTo(34*s,17*s); lineTo(41*s,25*s); lineTo(48*s,17*s)
            lineTo(61*s,24*s); lineTo(54*s,37*s); lineTo(47*s,33*s); lineTo(47*s,64*s)
            lineTo(35*s,64*s); lineTo(35*s,33*s); lineTo(28*s,37*s); close()
        }
        drawPath(shirt, fill)
        drawRoundRect(accent, topLeft = Offset(33*s,35*s), size = Size(16*s,13*s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5f*s))
    }
}

@Composable
fun PressArtwork(modifier: Modifier = Modifier) {
    val palette = LocalPressBenchPalette.current
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier.aspectRatio(1f)) {
        val s = size.minDimension / 250f
        translate((size.width-size.minDimension)/2, (size.height-size.minDimension)/2) {
            drawOval(Color.Black.copy(alpha=.18f), topLeft=Offset(49*s,207*s), size=Size(168*s,28*s))
            drawRoundRect(palette.dark, Offset(61*s,168*s), Size(139*s,29*s), androidx.compose.ui.geometry.CornerRadius(11*s))
            drawRoundRect(palette.cream, Offset(76*s,158*s), Size(110*s,14*s), androidx.compose.ui.geometry.CornerRadius(6*s))
            drawRoundRect(primary, Offset(77*s,111*s), Size(109*s,43*s), androidx.compose.ui.geometry.CornerRadius(9*s))
            drawRoundRect(palette.cream, Offset(89*s,121*s), Size(86*s,23*s), androidx.compose.ui.geometry.CornerRadius(5*s))
            val arm = Path().apply { moveTo(88*s,108*s); lineTo(112*s,57*s); lineTo(134*s,57*s); lineTo(116*s,108*s); close() }
            drawPath(arm, palette.cream)
            rotate(5f, pivot=Offset(154*s,59*s)) { drawRoundRect(palette.warm, Offset(109*s,48*s), Size(90*s,21*s), androidx.compose.ui.geometry.CornerRadius(8*s)) }
            drawRoundRect(palette.dark, Offset(53*s,197*s), Size(154*s,13*s), androidx.compose.ui.geometry.CornerRadius(6*s))
            drawRoundRect(palette.brand3, Offset(61*s,148*s), Size(137*s,4*s), androidx.compose.ui.geometry.CornerRadius(2*s))
            drawCircle(palette.brand3, 4*s, Offset(99*s,181*s)); drawCircle(palette.warm, 4*s, Offset(113*s,181*s))
        }
    }
}

@Composable
fun ProgressSteps(labels: List<String>, current: Int, modifier: Modifier = Modifier) {
    val palette = LocalPressBenchPalette.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        labels.forEachIndexed { index, label ->
            val done = index < current
            val active = index == current
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                shape = RoundedCornerShape(10.dp),
                color = when { done -> palette.successSoft; active -> MaterialTheme.colorScheme.primaryContainer; else -> palette.surface2 },
                contentColor = when { done -> palette.success; active -> MaterialTheme.colorScheme.primary; else -> palette.ink3 },
            ) {
                Box(Modifier.fillMaxSize().padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ClickableRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    danger: Boolean = false,
    toggleValue: Boolean? = null,
) {
    val palette = LocalPressBenchPalette.current
    val interaction = remember { MutableInteractionSource() }
    val actionModifier = if (toggleValue == null) {
        Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
    } else {
        Modifier.toggleable(value = toggleValue, interactionSource = interaction, indication = null, role = Role.Switch) { onClick() }
    }
    Row(
        modifier.fillMaxWidth().then(actionModifier).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(SmallShape).background(if (danger) palette.dangerSoft else MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(21.dp), tint = if (danger) palette.danger else MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (danger) palette.danger else MaterialTheme.colorScheme.onSurface, maxLines = 2)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = palette.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        if (trailing != null) trailing() else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.ink3)
    }
}

@Composable
fun AdReservation(label: String, modifier: Modifier = Modifier, height: Dp = 58.dp) {
    val palette = LocalPressBenchPalette.current
    Box(
        modifier.fillMaxWidth().height(height).background(palette.surface2).border(1.dp, MaterialTheme.colorScheme.outline),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.widthIn(max = 320.dp).fillMaxWidth().heightIn(max = 50.dp).fillMaxHeight()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = palette.ink3, letterSpacing = 1.1.sp)
            Spacer(Modifier.width(7.dp))
            Text("320×50", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = palette.ink3)
        }
    }
}

val PlayIcon = Icons.Filled.PlayArrow
val PauseIcon = Icons.Filled.Pause
