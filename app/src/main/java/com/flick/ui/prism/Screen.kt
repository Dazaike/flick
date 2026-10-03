package com.flick.ui.prism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flick.ui.theme.Prism
import com.flick.ui.theme.PrismText
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Full-screen page with a glass top bar. [content] and the top bar/FAB all receive the flat page
 * background layer as their backdrop; it never records the content, so glass cannot sample itself.
 */
@Composable
fun PrismScreen(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.(Backdrop) -> Unit = {},
    floatingAction: (@Composable (Backdrop) -> Unit)? = null,
    content: @Composable (padding: PaddingValues, contentBackdrop: Backdrop) -> Unit,
) {
    val pageBackdrop = rememberLayerBackdrop()
    // Controls inside the content must never sample a layer that records the content itself
    // (self-reference recurses in RenderNode tree preparation and overflows the render thread stack).
    val density = LocalDensity.current
    val top = with(density) { WindowInsets.statusBars.getTop(this).toDp() } + 64.dp
    val bottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() } +
        (if (floatingAction != null) 88.dp else 0.dp)

    Box(modifier.fillMaxSize().background(Prism.background)) {
        Box(Modifier.fillMaxSize().layerBackdrop(pageBackdrop).background(Prism.background))
        content(PaddingValues(top = top, bottom = bottom), pageBackdrop)
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrismText(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions(pageBackdrop) }
        }
        if (floatingAction != null) {
            Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)) {
                floatingAction(pageBackdrop)
            }
        }
    }
}
