package pl.szczodrzynski.edziennik.ui.aximo

/**
 * Twenty muted dark Aximo presets.
 * Colors are intentionally subdued so the UI stays comfortable in long school-day use.
 */
enum class AximoAppearanceStyle(
    val title: String,
    val background: Int,
    val surface: Int,
    val surfaceAlt: Int,
    val accent: Int,
    val accentSoft: Int,
    val text: Int,
) {
    MIDNIGHT("Midnight", 0xFF11101A.toInt(), 0xFF1D1930.toInt(), 0xFF252034.toInt(), 0xFF7563B8.toInt(), 0xFF3A3152.toInt(), 0xFFF0ECF5.toInt()),
    DEEP_BLUE("Deep Blue", 0xFF0F141D.toInt(), 0xFF182231.toInt(), 0xFF223044.toInt(), 0xFF5E7FA8.toInt(), 0xFF30445D.toInt(), 0xFFEAF0F7.toInt()),
    OCEAN("Ocean", 0xFF0E171A.toInt(), 0xFF17262B.toInt(), 0xFF21373C.toInt(), 0xFF5A8C91.toInt(), 0xFF315358.toInt(), 0xFFEAF4F4.toInt()),
    FOREST("Forest", 0xFF101712.toInt(), 0xFF1A2920.toInt(), 0xFF24382B.toInt(), 0xFF668B70.toInt(), 0xFF38513F.toInt(), 0xFFEDF5EE.toInt()),
    MOSS("Moss", 0xFF151710.toInt(), 0xFF252A1B.toInt(), 0xFF333823.toInt(), 0xFF92965F.toInt(), 0xFF4D5036.toInt(), 0xFFF3F3E8.toInt()),
    PLUM("Plum", 0xFF17111A.toInt(), 0xFF291B30.toInt(), 0xFF38243F.toInt(), 0xFF966B9E.toInt(), 0xFF533A59.toInt(), 0xFFF4ECF5.toInt()),
    BURGUNDY("Burgundy", 0xFF1A1114.toInt(), 0xFF2A1B20.toInt(), 0xFF3A242B.toInt(), 0xFF9A6572.toInt(), 0xFF593942.toInt(), 0xFFF6ECEF.toInt()),
    ROSE("Dark Rose", 0xFF191217.toInt(), 0xFF2A1B23.toInt(), 0xFF39242F.toInt(), 0xFFA06D83.toInt(), 0xFF5B3B4A.toInt(), 0xFFF6EDF1.toInt()),
    TEAL("Muted Teal", 0xFF101719.toInt(), 0xFF1A2829.toInt(), 0xFF26393A.toInt(), 0xFF61928D.toInt(), 0xFF385650.toInt(), 0xFFECF5F4.toInt()),
    SLATE("Slate", 0xFF121519.toInt(), 0xFF20252C.toInt(), 0xFF2C323A.toInt(), 0xFF778493.toInt(), 0xFF444D59.toInt(), 0xFFEEF1F4.toInt()),
    GRAPHITE("Graphite", 0xFF121212.toInt(), 0xFF202020.toInt(), 0xFF2B2B2B.toInt(), 0xFF858585.toInt(), 0xFF444444.toInt(), 0xFFF0F0F0.toInt()),
    COFFEE("Coffee", 0xFF18130F.toInt(), 0xFF292019.toInt(), 0xFF382B22.toInt(), 0xFF9A795F.toInt(), 0xFF5A4435.toInt(), 0xFFF5EEE8.toInt()),
    SANDSTONE("Sandstone Dark", 0xFF191612.toInt(), 0xFF29231C.toInt(), 0xFF393126.toInt(), 0xFFA18A68.toInt(), 0xFF5E503D.toInt(), 0xFFF6F0E5.toInt()),
    NAVY("Navy", 0xFF0F131B.toInt(), 0xFF192130.toInt(), 0xFF242F41.toInt(), 0xFF637EA5.toInt(), 0xFF34455E.toInt(), 0xFFECF1F8.toInt()),
    INDIGO("Indigo", 0xFF12121B.toInt(), 0xFF201F32.toInt(), 0xFF2D2B45.toInt(), 0xFF7773AA.toInt(), 0xFF423F64.toInt(), 0xFFF0EFF7.toInt()),
    STEEL("Steel", 0xFF121517.toInt(), 0xFF20272A.toInt(), 0xFF2C3539.toInt(), 0xFF71858D.toInt(), 0xFF424F54.toInt(), 0xFFEDF2F3.toInt()),
    AMBER("Dark Amber", 0xFF18150F.toInt(), 0xFF29231A.toInt(), 0xFF393022.toInt(), 0xFFA08658.toInt(), 0xFF5D4C31.toInt(), 0xFFF6F0E5.toInt()),
    AURORA("Aurora", 0xFF11151A.toInt(), 0xFF1C2430.toInt(), 0xFF27323B.toInt(), 0xFF668C8C.toInt(), 0xFF3C5558.toInt(), 0xFFEDF5F4.toInt()),
    VIOLET("Soft Violet", 0xFF15111A.toInt(), 0xFF241B30.toInt(), 0xFF332642.toInt(), 0xFF8C70A6.toInt(), 0xFF4E3C5F.toInt(), 0xFFF3EDF6.toInt()),
    AXIMO("Aximo Original", 0xFF11101A.toInt(), 0xFF1D1730.toInt(), 0xFF241D36.toInt(), 0xFF7657FF.toInt(), 0xFF493D62.toInt(), 0xFFF2EEF8.toInt());

    companion object {
        fun fromOrdinal(value: Int): AximoAppearanceStyle =
            entries.getOrElse(value.coerceIn(0, entries.lastIndex)) { AXIMO }
    }
}
