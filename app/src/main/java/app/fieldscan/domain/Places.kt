package app.fieldscan.domain

/** The scene a frame is shot in: what the trail camera sees around the creature. */
enum class Scene { BRIDGE, WELL, LAMP, STOP, STAIRS, DOOR, FOREST, WATER, STREET, YARD, OPEN, TOWER }

/**
 * Kinds of places creatures live in, each read from OpenStreetMap tags. [glyph] is drawn on the scanner,
 * [noise] is the sound of the trace ("…хлюп…").
 */
enum class Habitat(val ru: String, val en: String, val glyph: String, val scene: Scene, val noiseRu: String, val noiseEn: String) {
    BRIDGE("Мост", "Bridge", "⌒", Scene.BRIDGE, "хлюп", "splash"),
    STEPS("Лестница", "Stairs", "≡", Scene.STAIRS, "шарк", "scuff"),
    SPRING("Вода из камня", "Spring", "◊", Scene.WELL, "звон", "chime"),
    STOP("Остановка", "Bus stop", "▭", Scene.STOP, "гул", "hum"),
    PHARMACY("Аптека", "Pharmacy", "+", Scene.DOOR, "шурх", "rustle"),
    LAMP("Фонарь", "Street lamp", "☼", Scene.LAMP, "тик", "tick"),
    TREE("Дерево", "Tree", "♣", Scene.FOREST, "скрип", "creak"),
    PLAYGROUND("Площадка", "Playground", "△", Scene.YARD, "скрип", "squeak"),
    CROSSING("Перекрёсток", "Crossroads", "✕", Scene.STREET, "шорох", "hiss"),
    WORSHIP("Храм", "Church", "†", Scene.TOWER, "бом", "toll"),
    MEMORIAL("Памятник", "Monument", "▲", Scene.OPEN, "стук", "knock"),
    TOWER("Башня", "Tower", "▮", Scene.TOWER, "свист", "whistle"),
    PIER("Причал", "Pier", "⊥", Scene.WATER, "плеск", "lap"),
    UNDERPASS("Переход", "Underpass", "∩", Scene.STREET, "эхо", "echo"),
    POSTBOX("Почтовый ящик", "Postbox", "✉", Scene.DOOR, "шелест", "rustle"),
    BENCH("Скамейка", "Bench", "▬", Scene.YARD, "вздох", "sigh"),
    VIEWPOINT("Смотровая", "Viewpoint", "◇", Scene.OPEN, "ветер", "wind"),
    WATER("У воды", "Waterside", "≈", Scene.WATER, "плеск", "lap"),
    HOME("Дом", "Home", "⌂", Scene.YARD, "тук", "knock");

    fun name(ru: Boolean) = if (ru) this.ru else en
}

/**
 * A place from the map that may hold a creature. [id] is the OSM id ("n123", "w456"), stable for years.
 * [a] and [b] are the ends of a line (a bridge, a staircase) when it has them; [flow] is the bearing the
 * water runs under a bridge; [size] is length in metres (or the step count for stairs); [year] when known.
 */
data class Place(
    val id: String,
    val habitat: Habitat,
    val at: Geo,
    val name: String = "",
    val a: Geo? = null,
    val b: Geo? = null,
    val flow: Double? = null,
    val size: Double = 0.0,
    val year: Int? = null,
    val ways: Int = 0,
) {
    /** The middle of a line place, or the point itself. */
    val mid: Geo get() = if (a != null && b != null) Geo((a.lat + b.lat) / 2, (a.lon + b.lon) / 2) else at
}

/** The sky right now as rites see it. */
data class Weather(
    val temp: Double = 8.0,
    val fog: Boolean = false,
    val rain: Boolean = false,
    val snow: Boolean = false,
    val wind: Double = 2.0,
    /** Where the wind blows from, degrees. */
    val windFrom: Double = 270.0,
    val at: Long = 0,
) {
    val frost get() = temp <= 0.0
}
