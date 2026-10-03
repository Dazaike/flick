package com.flick.ui.prism

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flick.ui.theme.Prism
import com.flick.ui.theme.PrismText
import com.kyant.shapes.RoundedRectangle
import kotlin.math.max

@Composable
fun PrismListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = Prism.colors
    val haptics = LocalHaptics.current
    val press = rememberPressState(enabled = onClick != null)
    val rowModifier = if (onClick != null) {
        modifier
            .clip(RoundedRectangle(16.dp))
            .drawBehind {
                val a = colors.fillWeak.alpha * max(press.hover, press.progress * 1.6f)
                if (a > 0f) drawRect(colors.fillWeak.copy(alpha = a))
            }
            .pressInput(press)
            .clickable(interactionSource = press.interactionSource, indication = null, role = Role.Button) {
                haptics.perform(HapticKind.Tick)
                onClick()
            }
    } else {
        modifier
    }
    Row(
        rowModifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.padding(end = 16.dp), contentAlignment = Alignment.Center) { leading() }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PrismText(headline, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (supporting != null) PrismText(supporting, fontSize = 14.sp, color = Prism.subText)
        }
        if (trailing != null) {
            Box(Modifier.padding(start = 16.dp), contentAlignment = Alignment.Center) { trailing() }
        }
    }
}
