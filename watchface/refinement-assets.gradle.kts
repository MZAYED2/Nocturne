import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.Line2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.*

val refinedResources = layout.buildDirectory.dir("generated/refinement/res")
val generateRefinedDial = tasks.register("generateRefinedDial") {
    val originals = file("src/main/res/drawable")
    val numeralFont = file("src/main/res/font/ref_serif.ttf")
    val functionalFont = file("src/main/res/font/functional_medium.ttf")
    val moonSourceFile = rootProject.file("tools/moon-source.jpg")
    val generatedDirectory = refinedResources.get().asFile
    inputs.dir(originals)
    inputs.file(numeralFont)
    inputs.file(functionalFont)
    if (moonSourceFile.exists()) {
        inputs.file(moonSourceFile)
    }
    outputs.dir(generatedDirectory)
    doLast {
        val output = generatedDirectory.resolve("drawable").apply { mkdirs() }
        val font = Font.createFont(Font.TRUETYPE_FONT, numeralFont).deriveFont(Font.PLAIN, 106f)
        val functional = Font.createFont(Font.TRUETYPE_FONT, functionalFont)
        val context = java.awt.font.FontRenderContext(null, true, true)
        for ((text, size, width) in listOf(Triple("99,999", 68f, 340), Triple("100%", 52f, 166),
                Triple("Sep", 34f, 164), Triple("31", 45f, 164))) {
            val bounds = functional.deriveFont(size).createGlyphVector(context, text).visualBounds
            check(bounds.width + 16 < width) { "Functional text does not fit: $text" }
        }
        fun save(name: String, image: BufferedImage) {
            ImageIO.write(image, "png", output.resolve("$name.png"))
        }
        // Rasterize measured upright glyphs in dial coordinates; retain independent PNG layers.
        for (theme in 0..4) for (style in 0..2) for (ambient in listOf(false, true)) {
            val image = BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB)
            val g = image.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val bounds = mutableListOf<java.awt.geom.Rectangle2D>()
            for (hour in 1..12) {
                if (style == 2 || (style == 1 && hour % 3 != 0)) continue
                val glyph = font.createGlyphVector(g.fontRenderContext, hour.toString())
                val b = glyph.visualBounds
                val angle = Math.toRadians(hour * 30.0 - 90.0)
                var radius = if (hour >= 10) 418.0 else 432.0
                var tx: Double
                var ty: Double
                while (true) {
                    tx = 500 + cos(angle) * radius - b.centerX
                    ty = 500 + sin(angle) * radius - b.centerY
                    val inside = listOf(b.minX to b.minY, b.minX to b.maxY, b.maxX to b.minY, b.maxX to b.maxY)
                        .all { (x, y) -> hypot(x + tx - 500, y + ty - 500) <= 468 }
                    if (inside) break
                    radius -= 1
                    check(radius > 350) { "Numeral $hour does not fit" }
                }
                bounds += java.awt.geom.Rectangle2D.Double(b.x + tx - 6, b.y + ty - 6, b.width + 12, b.height + 12)
                g.color = Color(0xF1, 0xE6, 0xD2)
                g.drawGlyphVector(glyph, tx.toFloat(), ty.toFloat())
            }
            for (minute in 0 until 60) {
                val major = minute % 5 == 0
                val angle = Math.toRadians(minute * 6.0 - 90)
                val endRadius = 483.0
                var startRadius = if (major) 461.0 else 470.0
                val ex = 500 + cos(angle) * endRadius
                val ey = 500 + sin(angle) * endRadius
                while (bounds.any { it.intersectsLine(500 + cos(angle) * startRadius, 500 + sin(angle) * startRadius, ex, ey) }) {
                    startRadius += 1
                    check(startRadius < endRadius) { "Marker $minute collides with a numeral" }
                }
                g.stroke = BasicStroke(if (major) 5f else 2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g.color = if (major) Color(0xF1, 0xE6, 0xD2, if (ambient) 150 else 255) else Color(0x86, 0x9B, 0xA5, if (ambient) 105 else 190)
                g.draw(Line2D.Double(500 + cos(angle) * startRadius, 500 + sin(angle) * startRadius, ex, ey))
            }
            g.dispose()
            save("refined_${theme}_dial_${style}${if (ambient) "_aod" else ""}", image)
        }
        // One opacity stage only: normalize the source alpha, then apply 35% visibility
        // and local readability masks in final dial coordinates. WFF alpha remains 255.
        for (theme in 0..4) for (style in 0..2) {
            val source = ImageIO.read(originals.resolve("uc_${theme}_orn_${style}.png"))
            var maxAlpha = 0
            for (y in 0 until source.height) for (x in 0 until source.width) {
                maxAlpha = max(maxAlpha, source.getRGB(x, y).ushr(24))
            }
            val ornament = BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB)
            val painter = ornament.createGraphics()
            painter.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            if (style == 2) {
                // Ornate has a large transparent border unlike the balanced asset.
                var left = source.width
                var top = source.height
                var right = 0
                var bottom = 0
                for (y in 0 until source.height) for (x in 0 until source.width) {
                    if (source.getRGB(x, y).ushr(24) > maxAlpha / 10) {
                        left = min(left, x)
                        top = min(top, y)
                        right = max(right, x)
                        bottom = max(bottom, y)
                    }
                }
                painter.drawImage(source, 0, -100, 700, 1100, left, top, right + 1, bottom + 1, null)
            } else {
                painter.drawImage(source, 0, -100, 1700, 1200, null)
            }
            painter.dispose()
            val dial = ImageIO.read(output.resolve("refined_${theme}_dial_0.png"))
            for (y in 0 until 1000) for (x in 0 until 1000) {
                val c = Color(ornament.getRGB(x, y), true)
                if (c.alpha == 0 || x >= 500) {
                    ornament.setRGB(x, y, 0)
                    continue
                }
                val edgeFade = ((500.0 - x) / 65.0).coerceIn(0.0, 1.0)
                val batteryDistance = hypot(x - 285.0, y - 505.0)
                val batteryFade = 0.35 + 0.65 * ((batteryDistance - 95.0) / 30.0).coerceIn(0.0, 1.0)
                var nearbyInk = 0.0
                for (dy in -12..12 step 4) for (dx in -12..12 step 4) {
                    val sx = (x + dx).coerceIn(0, 999)
                    val sy = (y + dy).coerceIn(0, 999)
                    val ink = dial.getRGB(sx, sy).ushr(24) / 255.0
                    nearbyInk = max(nearbyInk, ink * (1 - hypot(dx.toDouble(), dy.toDouble()) / 20).coerceAtLeast(0.0))
                }
                val textFade = 1 - nearbyInk * 0.65
                val alpha = (c.alpha * 89.0 / maxAlpha.coerceAtLeast(1) * edgeFade * batteryFade * textFade).roundToInt().coerceIn(0, 89)
                ornament.setRGB(x, y, Color(c.red, c.green, c.blue, alpha).rgb)
            }
            save("visible_${theme}_orn_${style}", ornament)
            println("Ornament $theme/$style: source max alpha=$maxAlpha; effective maximum=35%")
        }
        // Restore only the old interior channel using the same row's metal gradient,
        // then punch the smaller rounded opening. Silhouette and WFF pivots stay intact.
        for (theme in 0..4) for (kind in listOf("hour", "minute")) for (ambient in listOf(false, true)) {
            val suffix = if (ambient) "_aod" else ""
            val image = ImageIO.read(originals.resolve("uc_${theme}_$kind$suffix.png"))
            val w = image.width
            val h = image.height
            val oldTop = if (kind == "hour") 0.135 else 0.115
            val oldBottom = if (kind == "hour") 0.69 else 0.74
            val slotLength = if (kind == "hour") 0.40 * (250.0 / 280) * h else 0.50 * (350.0 / 380) * h
            val slotWidth = w * 0.23
            // Keep ambient geometry intact; active openings sit near the outer tip.
            val slotCenterY = if (ambient) h * 0.44 else h * 0.08 + slotLength / 2
            val slot = RoundRectangle2D.Double((w - slotWidth) / 2, slotCenterY - slotLength / 2, slotWidth, slotLength, slotWidth, slotWidth)
            for (y in (oldTop * h).toInt()..(oldBottom * h).toInt()) {
                val left = Color(image.getRGB((w * 0.20).toInt(), y), true)
                val right = Color(image.getRGB((w * 0.80).toInt(), y), true)
                for (x in (w * 0.28).toInt()..(w * 0.72).toInt()) {
                    val t = (x.toDouble() / w - 0.20) / 0.60
                    fun channel(a: Int, b: Int) = (a + (b - a) * t).roundToInt().coerceIn(0, 255)
                    image.setRGB(x, y, Color(channel(left.red, right.red), channel(left.green, right.green), channel(left.blue, right.blue), 255).rgb)
                }
            }
            val g = image.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.composite = java.awt.AlphaComposite.Clear
            g.fill(slot)
            g.dispose()
            if (kind == "hour" && theme == 0 && !ambient) {
                for (y in 0 until h) for (x in 0 until w) {
                    val c = Color(image.getRGB(x, y), true)
                    image.setRGB(x, y, Color((c.red * 0.90).roundToInt(), (c.green * 0.90).roundToInt(), (c.blue * 0.90).roundToInt(), c.alpha).rgb)
                }
            }
            save("refined_${theme}_$kind$suffix", image)
        }
        val battery = BufferedImage(128, 88, BufferedImage.TYPE_INT_ARGB)
        val bg = battery.createGraphics()
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        bg.color = Color(0xF1, 0xE6, 0xD2)
        bg.stroke = BasicStroke(7f)
        bg.draw(RoundRectangle2D.Double(7.0, 14.0, 102.0, 60.0, 10.0, 10.0))
        bg.fill(RoundRectangle2D.Double(112.0, 30.0, 10.0, 28.0, 3.0, 3.0))
        bg.fill(RoundRectangle2D.Double(19.0, 26.0, 22.0, 36.0, 3.0, 3.0))
        bg.dispose()
        save("refined_battery", battery)

        // --- 3D Realistic Lunar Surface Generator for Sage Nocturne and Lunar Phase ---
        val moonSource = if (moonSourceFile.exists()) ImageIO.read(moonSourceFile) else null

        fun generateSphericalMoon(size: Int, phase: Int, isSmall: Boolean): BufferedImage {
            val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val R = size / 2.0 - 1.0
            val cx = size / 2.0
            val cy = size / 2.0
            val t = phase / 28.0

            // Astronomical Sun Vector:
            // t = 0.0 -> New Moon (back-lit, sz = -1)
            // t = 0.25 -> First Quarter (Waxing, lit from right, sx = +1)
            // t = 0.50 -> Full Moon (front-lit, sz = +1)
            // t = 0.75 -> Last Quarter (Waning, lit from left, sx = -1)
            val sx = sin(2.0 * Math.PI * t)
            val sy = 0.0
            val sz = -cos(2.0 * Math.PI * t)

            val srcW = moonSource?.width ?: 1
            val srcH = moonSource?.height ?: 1
            val srcR = 510.0
            val srcCx = 512.0
            val srcCy = 512.0

            fun sampleBilinear(u: Double, v: Double): Double {
                if (moonSource == null) return 0.80
                val w = moonSource.width
                val h = moonSource.height
                val x0 = Math.floor(u).toInt().coerceIn(0, w - 1)
                val y0 = Math.floor(v).toInt().coerceIn(0, h - 1)
                val x1 = (x0 + 1).coerceIn(0, w - 1)
                val y1 = (y0 + 1).coerceIn(0, h - 1)
                val fx = u - Math.floor(u)
                val fy = v - Math.floor(v)
                fun lum(rgb: Int): Double {
                    val r = (rgb shr 16) and 0xFF
                    val g = (rgb shr 8) and 0xFF
                    val b = rgb and 0xFF
                    return (r * 0.299 + g * 0.587 + b * 0.114) / 255.0
                }
                val c00 = lum(moonSource.getRGB(x0, y0))
                val c10 = lum(moonSource.getRGB(x1, y0))
                val c01 = lum(moonSource.getRGB(x0, y1))
                val c11 = lum(moonSource.getRGB(x1, y1))
                val top = c00 * (1 - fx) + c10 * fx
                val bottom = c01 * (1 - fx) + c11 * fx
                return top * (1 - fy) + bottom * fy
            }

            for (y in 0 until size) {
                val dy = y + 0.5 - cy
                for (x in 0 until size) {
                    val dx = x + 0.5 - cx
                    val dist = hypot(dx, dy)
                    if (dist > R + 0.5) continue

                    val nx = dx / R
                    val ny = dy / R
                    val nz = sqrt(max(0.0, 1.0 - nx * nx - ny * ny))

                    val cosIncident = nx * sx + ny * sy + nz * sz

                    // Smooth Bilinear Texture sampling from genuine lunar topography
                    val texVal = sampleBilinear(srcCx + nx * srcR, srcCy + ny * srcR)

                    // Contrast boost for crisp craters and maria definition
                    val cTex = ((texVal - 0.5) * (if (isSmall) 1.25 else 1.15) + 0.5).coerceIn(0.0, 1.0)

                    // Sharp terminator transition with sub-pixel anti-aliasing
                    val transWidth = 0.035
                    val sunlitFade = ((cosIncident / transWidth) + 0.5).coerceIn(0.0, 1.0)
                    // Lommel-Seeliger scattering curve for realistic lunar regolith reflection
                    val sunlitLum = (0.28 + 0.72 * max(0.0, cosIncident)).coerceIn(0.0, 1.0)

                    // Earthshine on unlit side (brighter near New Moon, as Earth is full)
                    val earthshineBase = if (isSmall) 0.14 else 0.10
                    val earthshine = earthshineBase * (0.75 + 0.25 * (1.0 - cos(2.0 * Math.PI * t))) * (0.6 + 0.4 * nz)

                    val illumination = sunlitFade * sunlitLum + (1.0 - sunlitFade) * earthshine

                    // Subtle limb rim / ash light along the outer perimeter so New Moon has an elegant visible contour
                    val distToEdge = R - dist
                    val rimStrength = if (isSmall) 0.32 else 0.22
                    val rim = if (distToEdge in 0.0..2.5) {
                        val curve = (1.0 - abs(distToEdge - 1.0) / 1.5).coerceIn(0.0, 1.0)
                        rimStrength * curve * (1.0 - sunlitFade * 0.85)
                    } else 0.0

                    val finalLuminance = (cTex * illumination + rim).coerceIn(0.0, 1.0)

                    // Tone mapping:
                    // Sunlit: luminous moonlight off-white
                    // Dark side: deep lunar charcoal (earthshine)
                    val rOut = (finalLuminance * 238.0 + 8.0).roundToInt().coerceIn(0, 255)
                    val gOut = (finalLuminance * 234.0 + 8.0).roundToInt().coerceIn(0, 255)
                    val bOut = (finalLuminance * 225.0 + 9.0).roundToInt().coerceIn(0, 255)

                    // Anti-aliased outer edge
                    val edgeAlpha = if (dist > R - 0.75) {
                        ((R + 0.5 - dist) / 1.25).coerceIn(0.0, 1.0)
                    } else 1.0

                    val alphaOut = (edgeAlpha * 255.0).roundToInt().coerceIn(0, 255)
                    img.setRGB(x, y, Color(rOut, gOut, bOut, alphaOut).rgb)
                }
            }
            return img
        }

        for (phase in 0..28) {
            val smallMoon = generateSphericalMoon(224, phase, isSmall = true)
            save("refined_moon_" + phase.toString().padStart(2, '0'), smallMoon)

            val bigMoon = generateSphericalMoon(920, phase, isSmall = false)
            save("lunar_moon_" + phase.toString().padStart(2, '0'), bigMoon)
        }

        // --- Generate Lunar Phase AOD Dial ---
        val lunarAodDial = BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB)
        val lg = lunarAodDial.createGraphics()
        lg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        lg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        val lunarDialBounds = mutableListOf<java.awt.geom.Rectangle2D>()
        for (hour in 1..12) {
            val glyph = font.createGlyphVector(lg.fontRenderContext, hour.toString())
            val b = glyph.visualBounds
            val angle = Math.toRadians(hour * 30.0 - 90.0)
            val radius = if (hour >= 10) 418.0 else 432.0
            val tx = 500 + cos(angle) * radius - b.centerX
            val ty = 500 + sin(angle) * radius - b.centerY
            lunarDialBounds += java.awt.geom.Rectangle2D.Double(b.x + tx - 8, b.y + ty - 8, b.width + 16, b.height + 16)
            lg.color = Color(0xDC, 0xE3, 0xEA, 180)
            lg.drawGlyphVector(glyph, tx.toFloat(), ty.toFloat())
        }
        for (minute in 0 until 60) {
            val major = minute % 5 == 0
            val angle = Math.toRadians(minute * 6.0 - 90)
            val endRadius = 483.0
            var startRadius = if (major) 461.0 else 470.0
            val ex = 500 + cos(angle) * endRadius
            val ey = 500 + sin(angle) * endRadius
            while (lunarDialBounds.any { it.intersectsLine(500 + cos(angle) * startRadius, 500 + sin(angle) * startRadius, ex, ey) }) {
                startRadius += 1
                if (startRadius >= endRadius) break
            }
            if (startRadius < endRadius) {
                lg.stroke = BasicStroke(if (major) 4.5f else 2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                lg.color = if (major) Color(0xDC, 0xE3, 0xEA, 140) else Color(0x71, 0x80, 0x96, 95)
                lg.draw(Line2D.Double(500 + cos(angle) * startRadius, 500 + sin(angle) * startRadius, ex, ey))
            }
        }
        lg.dispose()
        save("lunar_dial_aod", lunarAodDial)

        // --- Generate Lunar Phase Hands with Dual Contrast ---
        fun createLunarHand(w: Int, h: Int, isMinute: Boolean, isAmbient: Boolean): BufferedImage {
            val hand = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            val g = hand.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val tipY = 8.0
            val pivotY = if (isMinute) h * 0.921053 else h * 0.892857
            val tailY = h * 0.98
            val midW = w * 0.78
            val baseW = w * 0.52

            val path = java.awt.geom.Path2D.Double()
            path.moveTo(w / 2.0, tipY)
            path.lineTo((w + midW) / 2.0, h * 0.25)
            path.lineTo((w + baseW) / 2.0, pivotY)
            path.lineTo((w + baseW * 0.6) / 2.0, tailY)
            path.lineTo((w - baseW * 0.6) / 2.0, tailY)
            path.lineTo((w - baseW) / 2.0, pivotY)
            path.lineTo((w - midW) / 2.0, h * 0.25)
            path.closePath()

            // Outer dark charcoal border for contrast against bright lit moon
            g.color = if (isAmbient) Color(0x10, 0x14, 0x18, 220) else Color(0x15, 0x18, 0x1D)
            g.stroke = BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.draw(path)

            // Inner luminous moon-glow body for contrast against dark shadow moon
            g.color = if (isAmbient) Color(0xDC, 0xE3, 0xEA, 220) else Color(0xF4, 0xF6, 0xF9)
            g.fill(path)

            // Center spine
            g.color = if (isAmbient) Color(0x71, 0x80, 0x96, 120) else Color(0x94, 0xA3, 0xB8)
            g.stroke = BasicStroke(1.5f)
            g.draw(Line2D.Double(w / 2.0, tipY + 12, w / 2.0, pivotY - 10))

            g.dispose()
            return hand
        }

        val lunarHour = createLunarHand(80, 560, isMinute = false, isAmbient = false)
        val lunarHourAod = createLunarHand(80, 560, isMinute = false, isAmbient = true)
        val lunarMinute = createLunarHand(68, 760, isMinute = true, isAmbient = false)
        val lunarMinuteAod = createLunarHand(68, 760, isMinute = true, isAmbient = true)

        save("lunar_hour", lunarHour)
        save("lunar_hour_aod", lunarHourAod)
        save("lunar_minute", lunarMinute)
        save("lunar_minute_aod", lunarMinuteAod)

        // Lunar Second Hand
        val lunarSec = BufferedImage(32, 790, BufferedImage.TYPE_INT_ARGB)
        val sg = lunarSec.createGraphics()
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        sg.color = Color(0x15, 0x18, 0x1D)
        sg.stroke = BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        sg.draw(Line2D.Double(16.0, 770.0, 16.0, 12.0))
        sg.color = Color(0xFA, 0xFA, 0xFB)
        sg.stroke = BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        sg.draw(Line2D.Double(16.0, 770.0, 16.0, 60.0))
        sg.color = Color(0xE5, 0x3E, 0x3E)
        sg.stroke = BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        sg.draw(Line2D.Double(16.0, 60.0, 16.0, 12.0))
        sg.dispose()
        save("lunar_second", lunarSec)

        // Lunar Center Hub
        val lunarHub = BufferedImage(92, 92, BufferedImage.TYPE_INT_ARGB)
        val hg = lunarHub.createGraphics()
        hg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        hg.color = Color(0x15, 0x18, 0x1D)
        hg.fillOval(4, 4, 84, 84)
        hg.color = Color(0xF4, 0xF6, 0xF9)
        hg.fillOval(14, 14, 64, 64)
        hg.color = Color(0x4A, 0x55, 0x68)
        hg.fillOval(32, 32, 28, 28)
        hg.dispose()
        save("lunar_hub", lunarHub)
    }
}
tasks.named("preBuild").configure { dependsOn(generateRefinedDial) }
