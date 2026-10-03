package com.flick.ui.prism

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flick.R
import com.flick.ui.theme.LocalContentColor

/** Stroked 24dp icon set (1.75 stroke, round caps), tinted at runtime like SVG `currentColor`. */
object PrismIcons {
    @DrawableRes val ArrowUp = R.drawable.ic_arrow_up
    @DrawableRes val ArrowDown = R.drawable.ic_arrow_down
    @DrawableRes val ArrowLeft = R.drawable.ic_arrow_left
    @DrawableRes val ArrowRight = R.drawable.ic_arrow_right
    @DrawableRes val ChevronUp = R.drawable.ic_chevron_up
    @DrawableRes val ChevronDown = R.drawable.ic_chevron_down
    @DrawableRes val ChevronLeft = R.drawable.ic_chevron_left
    @DrawableRes val ChevronRight = R.drawable.ic_chevron_right
    @DrawableRes val Search = R.drawable.ic_search
    @DrawableRes val Close = R.drawable.ic_close
    @DrawableRes val Menu = R.drawable.ic_menu
    @DrawableRes val Check = R.drawable.ic_check
    @DrawableRes val Plus = R.drawable.ic_plus
    @DrawableRes val Minus = R.drawable.ic_minus
    @DrawableRes val Settings = R.drawable.ic_settings
    @DrawableRes val Sliders = R.drawable.ic_sliders
    @DrawableRes val Eye = R.drawable.ic_eye
    @DrawableRes val EyeOff = R.drawable.ic_eye_off
    @DrawableRes val Alert = R.drawable.ic_alert
    @DrawableRes val Info = R.drawable.ic_info
    @DrawableRes val Trash = R.drawable.ic_trash
    @DrawableRes val History = R.drawable.ic_history
    @DrawableRes val App = R.drawable.ic_app
    @DrawableRes val Call = R.drawable.ic_call
    @DrawableRes val Dialpad = R.drawable.ic_dialpad
    @DrawableRes val Folder = R.drawable.ic_folder
    @DrawableRes val FolderPlus = R.drawable.ic_folder_plus
    @DrawableRes val Globe = R.drawable.ic_globe
    @DrawableRes val Sms = R.drawable.ic_sms
    @DrawableRes val Palette = R.drawable.ic_palette
    @DrawableRes val Play = R.drawable.ic_play
    @DrawableRes val Edit = R.drawable.ic_edit
    @DrawableRes val ViewList = R.drawable.ic_view_list
    @DrawableRes val Widgets = R.drawable.ic_widgets
    @DrawableRes val Text = R.drawable.ic_text
    @DrawableRes val Grid = R.drawable.ic_grid
    @DrawableRes val Bell = R.drawable.ic_bell
    @DrawableRes val Sidebar = R.drawable.ic_sidebar


}

@Composable
fun PrismIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current,
) {
    Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
