package com.flick.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flick.ui.screens.addbookmark.AddBookmarkScreen
import com.flick.ui.screens.addbookmark.AppPickerScreen
import com.flick.ui.screens.addbookmark.BookmarkTypeOption
import com.flick.ui.screens.addbookmark.ContactPickerMode
import com.flick.ui.screens.addbookmark.ContactPickerScreen
import com.flick.ui.screens.addbookmark.FolderCreateScreen
import com.flick.ui.screens.addbookmark.PhoneEntryMode
import com.flick.ui.screens.addbookmark.PhoneNumberEntryScreen
import com.flick.ui.screens.addbookmark.SettingsPanelPickerScreen
import com.flick.ui.screens.addbookmark.ShortcutPickerScreen
import com.flick.ui.screens.addbookmark.UrlEntryScreen
import com.flick.ui.screens.addbookmark.WidgetPickerScreen
import com.flick.ui.screens.bookmarklist.BookmarkListScreen
import com.flick.ui.screens.iconpicker.IconPackPickerScreen
import com.flick.ui.screens.settings.AppSettingsScreen
import com.flick.ui.theme.DURATION_MEDIUM
import com.flick.ui.theme.MotionConfig
import com.flick.ui.theme.ThemePreferences
import com.flick.ui.theme.flickTween

object FlickDestinations {
    const val BOOKMARK_LIST = "bookmark_list"
    const val ICON_PACK_PICKER = "icon_pack_picker"
    const val APP_SETTINGS = "app_settings"
    const val ADD_BOOKMARK = "add_bookmark/{categoryId}"
    const val APP_PICKER = "app_picker/{categoryId}"
    const val APP_SHORTCUT_APP_PICKER = "app_shortcut_app_picker/{categoryId}"
    const val SHORTCUT_PICKER = "shortcut_picker/{categoryId}/{packageName}"
    const val WIDGET_PICKER = "widget_picker/{categoryId}"
    const val URL_ENTRY = "url_entry/{categoryId}"
    const val SETTINGS_PANEL_PICKER = "settings_panel_picker/{categoryId}"
    const val PHONE_ENTRY = "phone_entry/{categoryId}/{mode}"
    const val CONTACT_PICKER = "contact_picker/{categoryId}/{mode}"
    const val FOLDER_CREATE = "folder_create/{categoryId}"

    fun addBookmark(categoryId: Long) = "add_bookmark/$categoryId"
    fun appPicker(categoryId: Long) = "app_picker/$categoryId"
    fun appShortcutAppPicker(categoryId: Long) = "app_shortcut_app_picker/$categoryId"
    fun shortcutPicker(categoryId: Long, packageName: String) = "shortcut_picker/$categoryId/$packageName"
    fun widgetPicker(categoryId: Long) = "widget_picker/$categoryId"
    fun urlEntry(categoryId: Long) = "url_entry/$categoryId"
    fun settingsPanelPicker(categoryId: Long) = "settings_panel_picker/$categoryId"
    fun phoneEntry(categoryId: Long, mode: PhoneEntryMode) = "phone_entry/$categoryId/${mode.name}"
    fun contactPicker(categoryId: Long, mode: ContactPickerMode) = "contact_picker/$categoryId/${mode.name}"
    fun folderCreate(categoryId: Long) = "folder_create/$categoryId"
}

private val categoryIdArg = navArgument("categoryId") { type = NavType.LongType }
private val modeArg = navArgument("mode") { type = NavType.StringType }

@Composable
fun FlickNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context.applicationContext) }
    val animationsEnabled by themePreferences.animationsEnabled.collectAsState(initial = true)
    val animationIntensity by themePreferences.animationIntensity.collectAsState(initial = 1f)
    val motion = MotionConfig(enabled = animationsEnabled, intensity = animationIntensity)

    fun forwardEnter(): EnterTransition =
        fadeIn(motion.flickTween(DURATION_MEDIUM)) +
            slideInHorizontally(motion.flickTween(DURATION_MEDIUM)) { fullWidth -> fullWidth / 4 }

    fun forwardExit(): ExitTransition =
        fadeOut(motion.flickTween(DURATION_MEDIUM)) +
            slideOutHorizontally(motion.flickTween(DURATION_MEDIUM)) { fullWidth -> -fullWidth / 4 }

    fun popEnter(): EnterTransition =
        fadeIn(motion.flickTween(DURATION_MEDIUM)) +
            slideInHorizontally(motion.flickTween(DURATION_MEDIUM)) { fullWidth -> -fullWidth / 4 }

    fun popExit(): ExitTransition =
        fadeOut(motion.flickTween(DURATION_MEDIUM)) +
            slideOutHorizontally(motion.flickTween(DURATION_MEDIUM)) { fullWidth -> fullWidth / 4 }

    fun NavGraphBuilder.flickComposable(
        route: String,
        arguments: List<NamedNavArgument> = emptyList(),
        content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
    ) {
        composable(
            route = route,
            arguments = arguments,
            enterTransition = { forwardEnter() },
            exitTransition = { forwardExit() },
            popEnterTransition = { popEnter() },
            popExitTransition = { popExit() },
            content = content
        )
    }

    NavHost(
        navController = navController,
        startDestination = FlickDestinations.BOOKMARK_LIST,
        modifier = modifier
    ) {
        flickComposable(FlickDestinations.BOOKMARK_LIST) {
            BookmarkListScreen(
                onAddBookmark = { categoryId ->
                    navController.navigate(FlickDestinations.addBookmark(categoryId))
                },
                onOpenIconPacks = { navController.navigate(FlickDestinations.ICON_PACK_PICKER) },
                onOpenSettings = { navController.navigate(FlickDestinations.APP_SETTINGS) }
            )
        }

        flickComposable(FlickDestinations.ICON_PACK_PICKER) {
            IconPackPickerScreen(onDone = { navController.popBackStack() })
        }

        flickComposable(FlickDestinations.APP_SETTINGS) {
            AppSettingsScreen()
        }

        flickComposable(FlickDestinations.ADD_BOOKMARK, arguments = listOf(categoryIdArg)) { entry ->
            val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
            AddBookmarkScreen(
                onTypeSelected = { type ->
                    val route = when (type) {
                        BookmarkTypeOption.APP -> FlickDestinations.appPicker(categoryId)
                        BookmarkTypeOption.APP_SHORTCUT -> FlickDestinations.appShortcutAppPicker(categoryId)
                        BookmarkTypeOption.WIDGET -> FlickDestinations.widgetPicker(categoryId)
                        BookmarkTypeOption.URL -> FlickDestinations.urlEntry(categoryId)
                        BookmarkTypeOption.SETTINGS_PANEL -> FlickDestinations.settingsPanelPicker(categoryId)
                        BookmarkTypeOption.CALL_CONTACT -> FlickDestinations.contactPicker(categoryId, ContactPickerMode.CALL)
                        BookmarkTypeOption.MESSAGE_CONTACT -> FlickDestinations.contactPicker(categoryId, ContactPickerMode.MESSAGE)
                        BookmarkTypeOption.DIAL_NUMBER -> FlickDestinations.phoneEntry(categoryId, PhoneEntryMode.DIAL)
                        BookmarkTypeOption.DIRECT_CALL -> FlickDestinations.phoneEntry(categoryId, PhoneEntryMode.DIRECT_CALL)
                        BookmarkTypeOption.SEND_SMS -> FlickDestinations.phoneEntry(categoryId, PhoneEntryMode.SEND_SMS)
                        BookmarkTypeOption.FOLDER -> FlickDestinations.folderCreate(categoryId)
                    }
                    navController.navigate(route)
                }
            )
        }

        flickComposable(FlickDestinations.APP_PICKER, arguments = listOf(categoryIdArg)) { entry ->
            val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
            AppPickerScreen(
                categoryId = categoryId,
                onAdded = { navController.popBackStack(FlickDestinations.BOOKMARK_LIST, inclusive = false) }
            )
        }

        flickComposable(FlickDestinations.APP_SHORTCUT_APP_PICKER, arguments = listOf(categoryIdArg)) { entry ->
            val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
            AppPickerScreen(
                categoryId = categoryId,
                onAdded = { navController.popBackStack(FlickDestinations.BOOKMARK_LIST, inclusive = false) },
                onAppSelected = { app ->
                    navController.navigate(FlickDestinations.shortcutPicker(categoryId, app.packageName))
                }
            )
        }

        flickComposable(
            FlickDestinations.SHORTCUT_PICKER,
            arguments = listOf(categoryIdArg, navArgument("packageName") { type = NavType.StringType })
        ) { entry ->
            val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
            val packageName = entry.arguments?.getString("packageName").orEmpty()
            ShortcutPickerScreen(
                categoryId = categoryId,
                packageName = packageName,
                onAdded = { navController.popBackStack(FlickDestinations.BOOKMARK_LIST, inclusive = false) }
            )
        }

        flickComposable(FlickDestinations.WIDGET_PICKER, arguments = listOf(categoryIdArg)) { entry ->
            val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
            WidgetPickerScreen(
                categoryId = categoryId,
                onAdded = { navController.popBackStack(FlickDestinations.BOOKMARK_LIST, inclusive = false) }
            )
        }

        flickComposable(FlickDestinations.URL_ENTRY, arguments = listOf(categoryIdArg)) { entry ->
            val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
            UrlEntryScreen(
                categoryId = categoryId,
                onAdded = { navController.popBackStack(FlickDestinations.BOOKMARK_LIST, inclusive = false) }
            )
        }

        flickComposable(FlickDestinations.SETTINGS_PANEL_PICKER, arguments = listOf(categoryIdArg)) { entry ->
            val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
            SettingsPanelPickerScreen(
                categoryId = categoryId,
                onAdded = { navController.popBackStack(FlickDestinations.BOOKMARK_LIST, inclusive = false) }
            )
        }

        flickComposable(FlickDestinations.PHONE_ENTRY, arguments = listOf(categoryIdArg, modeArg)) { entry ->
            val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
            val mode = PhoneEntryMode.valueOf(entry.arguments?.getString("mode") ?: PhoneEntryMode.DIAL.name)
            PhoneNumberEntryScreen(
                categoryId = categoryId,
                mode = mode,
                onAdded = { navController.popBackStack(FlickDestinations.BOOKMARK_LIST, inclusive = false) }
            )
        }

        flickComposable(FlickDestinations.CONTACT_PICKER, arguments = listOf(categoryIdArg, modeArg)) { entry ->
            val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
            val mode = ContactPickerMode.valueOf(entry.arguments?.getString("mode") ?: ContactPickerMode.CALL.name)
            ContactPickerScreen(
                categoryId = categoryId,
                mode = mode,
                onAdded = { navController.popBackStack(FlickDestinations.BOOKMARK_LIST, inclusive = false) }
            )
        }

        flickComposable(FlickDestinations.FOLDER_CREATE, arguments = listOf(categoryIdArg)) { entry ->
            val categoryId = entry.arguments?.getLong("categoryId") ?: 0L
            FolderCreateScreen(
                categoryId = categoryId,
                onAdded = { navController.popBackStack(FlickDestinations.BOOKMARK_LIST, inclusive = false) }
            )
        }
    }
}
