package com.unaj.obratrack.core.ui

import com.unaj.obratrack.core.ui.generated.resources.Res
import com.unaj.obratrack.core.ui.generated.resources.ic_add
import com.unaj.obratrack.core.ui.generated.resources.ic_add_photo
import com.unaj.obratrack.core.ui.generated.resources.ic_arrow_back
import com.unaj.obratrack.core.ui.generated.resources.ic_calendar
import com.unaj.obratrack.core.ui.generated.resources.ic_check_circle
import com.unaj.obratrack.core.ui.generated.resources.ic_circle_outline
import com.unaj.obratrack.core.ui.generated.resources.ic_delete
import com.unaj.obratrack.core.ui.generated.resources.ic_edit
import com.unaj.obratrack.core.ui.generated.resources.ic_error
import com.unaj.obratrack.core.ui.generated.resources.ic_logout
import com.unaj.obratrack.core.ui.generated.resources.ic_obras
import com.unaj.obratrack.core.ui.generated.resources.ic_pause_circle
import com.unaj.obratrack.core.ui.generated.resources.ic_play_circle
import com.unaj.obratrack.core.ui.generated.resources.ic_refresh
import com.unaj.obratrack.core.ui.generated.resources.ic_visibility
import com.unaj.obratrack.core.ui.generated.resources.ic_visibility_off
import com.unaj.obratrack.core.ui.generated.resources.ic_warning
import org.jetbrains.compose.resources.DrawableResource

/**
 * Íconos compartidos por las pantallas. La clase `Res` de este módulo es interna, y además
 * chocaría por nombre con la de cada feature: por eso se exponen acá como recursos.
 */
object Iconos {
    val Add: DrawableResource get() = Res.drawable.ic_add
    val AddPhoto: DrawableResource get() = Res.drawable.ic_add_photo
    val ArrowBack: DrawableResource get() = Res.drawable.ic_arrow_back
    val Calendar: DrawableResource get() = Res.drawable.ic_calendar
    val CheckCircle: DrawableResource get() = Res.drawable.ic_check_circle
    val CircleOutline: DrawableResource get() = Res.drawable.ic_circle_outline
    val Delete: DrawableResource get() = Res.drawable.ic_delete
    val Edit: DrawableResource get() = Res.drawable.ic_edit
    val Error: DrawableResource get() = Res.drawable.ic_error
    val Logout: DrawableResource get() = Res.drawable.ic_logout
    val Obras: DrawableResource get() = Res.drawable.ic_obras
    val PauseCircle: DrawableResource get() = Res.drawable.ic_pause_circle
    val PlayCircle: DrawableResource get() = Res.drawable.ic_play_circle
    val Refresh: DrawableResource get() = Res.drawable.ic_refresh
    val Visibility: DrawableResource get() = Res.drawable.ic_visibility
    val VisibilityOff: DrawableResource get() = Res.drawable.ic_visibility_off
    val Warning: DrawableResource get() = Res.drawable.ic_warning
}
