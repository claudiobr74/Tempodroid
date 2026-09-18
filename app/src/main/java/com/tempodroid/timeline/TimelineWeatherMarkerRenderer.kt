package com.tempodroid.timeline

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.PathParser
import com.tempodroid.sky.DayPeriod
import com.tempodroid.weather.WeatherCondition

/**
 * Ícones do marcador "agora", derivados dos vetores da seção
 * "08 · Timeline & Moon Refinement" do Figma.
 *
 * Os paths usam o viewport 24×24 dos SVGs aprovados e recebem cor por período
 * para manter o mesmo desenho sem introduzir assets raster.
 */
object TimelineWeatherMarkerRenderer {

    internal enum class Glyph {
        SUN,
        CLOUD_SUN,
        CLOUD,
        RAIN,
        STORM,
        MOON,
        CLOUD_MOON,
    }

    internal fun glyphFor(condition: WeatherCondition, period: DayPeriod): Glyph {
        val night = period == DayPeriod.NIGHT
        return when (condition) {
            WeatherCondition.CLEAR -> if (night) Glyph.MOON else Glyph.SUN
            WeatherCondition.PARTLY_CLOUDY -> if (night) Glyph.CLOUD_MOON else Glyph.CLOUD_SUN
            WeatherCondition.CLOUDY, WeatherCondition.FOG -> Glyph.CLOUD
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> Glyph.RAIN
            WeatherCondition.STORM -> Glyph.STORM
        }
    }

    internal fun colorFor(condition: WeatherCondition, period: DayPeriod): Int {
        val twilight = period == DayPeriod.DAWN ||
            period == DayPeriod.SUNSET ||
            period == DayPeriod.TWILIGHT
        if (twilight) return COLOR_TWILIGHT

        if (period == DayPeriod.NIGHT) {
            return when (condition) {
                WeatherCondition.PARTLY_CLOUDY,
                WeatherCondition.CLOUDY,
                WeatherCondition.FOG,
                -> COLOR_NIGHT_MUTED
                else -> COLOR_NIGHT
            }
        }

        return when (condition) {
            WeatherCondition.CLEAR -> COLOR_DAY_SUN
            WeatherCondition.PARTLY_CLOUDY -> COLOR_DAY_PARTLY
            WeatherCondition.CLOUDY, WeatherCondition.FOG -> COLOR_DAY_CLOUD
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> COLOR_DAY_RAIN
            WeatherCondition.STORM -> COLOR_TWILIGHT
        }
    }

    internal fun bubbleAccent(period: DayPeriod): Int =
        if (period == DayPeriod.NIGHT) COLOR_NIGHT else COLOR_NOW_ACCENT

    fun draw(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        sizePx: Float,
        condition: WeatherCondition,
        period: DayPeriod,
    ) {
        if (sizePx <= 0f) return
        val glyph = glyphFor(condition, period)
        val path = parsedPaths.getOrPut(glyph) {
            requireNotNull(PathParser.createPathFromPathData(pathData(glyph))) {
                "Path inválido para $glyph"
            }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorFor(condition, period)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val scale = sizePx / 24f
        canvas.save()
        canvas.translate(cx - sizePx / 2f, cy - sizePx / 2f)
        canvas.scale(scale, scale)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun pathData(glyph: Glyph): String = when (glyph) {
        Glyph.SUN -> SUN_PATH
        Glyph.CLOUD_SUN -> CLOUD_SUN_PATH
        Glyph.CLOUD -> CLOUD_PATH
        Glyph.RAIN -> RAIN_PATH
        Glyph.STORM -> STORM_PATH
        Glyph.MOON -> MOON_PATH
        Glyph.CLOUD_MOON -> CLOUD_MOON_PATH
    }

    private val parsedPaths = mutableMapOf<Glyph, Path>()

    private val COLOR_DAY_SUN = 0xFFFFC740.toInt()
    private val COLOR_DAY_PARTLY = 0xFFE2E8F0.toInt()
    private val COLOR_DAY_CLOUD = 0xFFCBD5E1.toInt()
    private val COLOR_DAY_RAIN = 0xFF38BDF8.toInt()
    private val COLOR_TWILIGHT = 0xFFFF9256.toInt()
    private val COLOR_NIGHT = 0xFF38BDF8.toInt()
    private val COLOR_NIGHT_MUTED = 0xFF64748B.toInt()
    private val COLOR_NOW_ACCENT = 0xFFFFD45A.toInt()

    private const val SUN_PATH =
        "M12 1.99902V3.99918M12 20.0005V22.0006M4.92912 4.92895L6.33923 6.33906" +
            "M17.6606 17.6604L19.0707 19.0705M1.9992 11.9998H3.99936M20.0006 11.9998H22.0008" +
            "M6.33923 17.6604L4.92912 19.0705M19.0707 4.92895L17.6606 6.33906" +
            "M16.0003 11.9998C16.0003 14.2091 14.2093 16.0001 12 16.0001" +
            "C9.79068 16.0001 7.99968 14.2091 7.99968 11.9998" +
            "C7.99968 9.79051 9.79068 7.9995 12 7.9995C14.2093 7.9995 16.0003 9.79051 16.0003 11.9998Z"

    private const val CLOUD_SUN_PATH =
        "M12.0007 1.99902V3.99918M4.93032 4.92895L6.34034 6.33906M20.0008 11.9998H22.0008" +
            "M19.0709 4.92895L17.6609 6.33906M15.9482 12.6494" +
            "C16.0713 11.9039 15.9807 11.1387 15.6869 10.4425" +
            "C15.3931 9.74633 14.9082 9.14758 14.2882 8.71561" +
            "C13.6682 8.28365 12.9386 8.03612 12.1838 8.0017" +
            "C11.429 7.96728 10.6798 8.14737 10.0231 8.52112" +
            "M13.0007 22.0006H7.00061C6.05458 22.0004 5.128 21.7318 4.32851 21.226" +
            "C3.52902 20.7202 2.88942 19.998 2.484 19.1431" +
            "C2.07858 18.2883 1.92397 17.336 2.03813 16.3968" +
            "C2.15229 15.4576 2.53054 14.5701 3.12894 13.8373" +
            "C3.72734 13.1045 4.52134 12.5565 5.41872 12.257" +
            "C6.3161 11.9575 7.28003 11.9187 8.19857 12.1452" +
            "C9.1171 12.3717 9.95254 12.8541 10.6078 13.5365" +
            "C11.2632 14.2189 11.7114 15.0732 11.9007 16.0001H13.0007" +
            "C13.7963 16.0001 14.5594 16.3162 15.122 16.8789" +
            "C15.6846 17.4415 16.0007 18.2047 16.0007 19.0004" +
            "C16.0007 19.7961 15.6846 20.5592 15.122 21.1219" +
            "C14.5594 21.6845 13.7963 22.0006 13.0007 22.0006Z"

    private const val CLOUD_PATH =
        "M9.00014 19.0006H17.5006C18.6941 19.0006 19.8387 18.5265 20.6827 17.6825" +
            "C21.5267 16.8384 22.0008 15.6937 22.0008 14.5001" +
            "C22.0008 13.3065 21.5267 12.1618 20.6827 11.3178" +
            "C19.8387 10.4738 18.6941 9.9996 17.5006 9.9996H15.7105" +
            "C15.3396 8.75543 14.63 7.63892 13.6611 6.77483" +
            "C12.6922 5.91074 11.5021 5.33312 10.2239 5.1065" +
            "C8.94558 4.87988 7.62948 5.01321 6.42261 5.49157" +
            "C5.21575 5.96993 4.16567 6.77448 3.38971 7.81533" +
            "C2.61375 8.85617 2.14249 10.0923 2.02859 11.3856" +
            "C1.91468 12.6789 2.16262 13.9783 2.7447 15.1388" +
            "C3.32678 16.2993 4.22007 17.275 5.32474 17.957" +
            "C6.42941 18.6389 7.70194 19.0003 9.00014 19.0006Z"

    private const val RAIN_PATH =
        "M4.00133 14.8991C3.25841 14.14 2.69797 13.2217 2.36245 12.2139" +
            "C2.02694 11.206 1.92515 10.135 2.06479 9.08201" +
            "C2.20444 8.029 2.58186 7.02157 3.16847 6.13604" +
            "C3.75507 5.25051 4.53548 4.51011 5.45058 3.97091" +
            "C6.36567 3.43171 7.39146 3.10785 8.45024 3.02387" +
            "C9.50902 2.93989 10.573 3.09798 11.5617 3.48618" +
            "C12.5503 3.87438 13.4377 4.4825 14.1565 5.26448" +
            "C14.8753 6.04646 15.4068 6.9818 15.7107 7.99964H17.5006" +
            "C18.466 7.99953 19.4059 8.30998 20.1814 8.88514" +
            "C20.9569 9.4603 21.5269 10.2697 21.8071 11.1937" +
            "C22.0873 12.1177 22.063 13.1073 21.7377 14.0164" +
            "C21.4123 14.9255 20.8033 15.7059 20.0004 16.2422" +
            "M16.0003 13.9998V20.0002M8.00076 13.9998V20.0002M12.0005 15.9999V22.0004"

    private const val STORM_PATH =
        "M6.00118 16.3262C4.94748 15.8258 4.03553 15.07 3.34821 14.1274" +
            "C2.66089 13.1848 2.22 12.0854 2.06561 10.929" +
            "C1.91122 9.77265 2.04823 8.59604 2.46418 7.50612" +
            "C2.88014 6.41619 3.56184 5.44751 4.44734 4.68813" +
            "C5.33285 3.92874 6.39407 3.40271 7.53455 3.15787" +
            "C8.67503 2.91302 9.85861 2.95712 10.9777 3.28615" +
            "C12.0968 3.61518 13.116 4.21871 13.9425 5.04188" +
            "C14.7691 5.86506 15.3768 6.88177 15.7106 7.99963H17.5005" +
            "C18.6519 7.99762 19.7602 8.43713 20.5975 9.22771" +
            "C21.4347 10.0183 21.937 11.0998 22.0011 12.2496" +
            "C22.0651 13.3993 21.686 14.5299 20.9418 15.4087" +
            "C20.1976 16.2874 19.1449 16.8473 18.0004 16.9733" +
            "M13.0004 11.9997L10.0006 17H14.0003L11.0005 22.0004"

    private const val MOON_PATH =
        "M19.2729 17.2992C20.2965 15.8939 20.8911 14.2223 20.9849 12.4863" +
            "C21.0069 12.0814 20.5259 11.8704 20.1819 12.0854" +
            "C19.0313 12.8044 17.6709 13.1123 16.3228 12.9588" +
            "C14.9747 12.8053 13.7184 12.1994 12.759 11.2401" +
            "C11.7996 10.2808 11.1937 9.02474 11.0402 7.67681" +
            "C10.8866 6.32888 11.1946 4.96871 11.9137 3.81829" +
            "C12.1287 3.47532 11.9167 2.99338 11.5117 3.01538" +
            "C9.77551 3.10927 8.10371 3.70398 6.69836 4.72762" +
            "C5.29302 5.75126 4.2143 7.16 3.5926 8.78355" +
            "C2.97091 10.4071 2.83286 12.176 3.19513 13.8763" +
            "C3.55741 15.5766 4.40451 17.1356 5.63403 18.3648" +
            "C6.86354 19.5941 8.42284 20.4409 10.1234 20.8029" +
            "C11.824 21.165 13.5931 21.0268 15.2168 20.405" +
            "C16.8405 19.7832 18.2493 18.7045 19.2729 17.2992Z"

    private const val CLOUD_MOON_PATH =
        "M18.3761 14.5131C19.2347 14.1425 19.9948 13.5763 20.5958 12.8598" +
            "C21.1967 12.1432 21.6218 11.2961 21.8371 10.3861" +
            "C21.9851 9.76107 21.1781 9.41607 20.5891 9.67207" +
            "C19.8532 9.99039 19.0387 10.0802 18.2511 9.92987" +
            "C17.4635 9.77954 16.7393 9.39598 16.1724 8.82897" +
            "C15.6055 8.26197 15.2221 7.53767 15.0719 6.75007" +
            "C14.9217 5.96247 15.0117 5.14791 15.3301 4.41207" +
            "C15.5851 3.82307 15.2401 3.01707 14.6141 3.16407" +
            "C13.3802 3.45726 12.2708 4.13394 11.4455 5.09695" +
            "C10.6201 6.05996 10.1211 7.25976 10.0201 8.52407" +
            "M13.0006 16.001C13.7962 16.001 14.5593 16.317 15.1219 16.8797" +
            "C15.6845 17.4423 16.0006 18.2053 16.0006 19.001" +
            "C16.0006 19.7966 15.6845 20.5597 15.1219 21.1223" +
            "C14.5593 21.6849 13.7962 22.001 13.0006 22.001H7.00057" +
            "C6.05454 22.0008 5.12798 21.7322 4.32849 21.2264" +
            "C3.52901 20.7207 2.88942 19.9985 2.484 19.1437" +
            "C2.07858 18.289 1.92397 17.3367 2.03813 16.3976" +
            "C2.15229 15.4585 2.53053 14.571 3.12893 13.8383" +
            "C3.72733 13.1056 4.52132 12.5576 5.41869 12.2581" +
            "C6.31606 11.9587 7.27999 11.9199 8.19851 12.1464" +
            "C9.11704 12.3728 9.95246 12.8552 10.6078 13.5375" +
            "C11.2631 14.2198 11.7114 15.0741 11.9006 16.001H13.0006Z"
}
