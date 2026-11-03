package app.fieldscan.domain

/** The build of a creature, for the trail-camera frame. */
enum class Shape { HUNCH, TALL, HAIR, CRAWLER, SMALL, FOG, QUAD, GNARL, PERCH, SQUAT }

/**
 * How a creature is drawn: a skeleton [shape] with proportions, then fur, horns, ears, eyes. Nothing here is
 * a picture; the frame is grown from these numbers and the seed of the place where it was caught.
 */
data class Body(
    val shape: Shape,
    val size: Float = 1f,
    /** Grey of the body in the IR frame, 0..255. */
    val tone: Int = 70,
    /** Share of the outline that grows hair, 0..1. */
    val fur: Float = 0.4f,
    val furLen: Float = 7f,
    /** Hair hangs down instead of standing out. */
    val droop: Boolean = false,
    val horns: Int = 0,
    val ears: Int = 0,
    val tail: Boolean = false,
    val eyes: Int = 2,
    /** The head glows instead of the eyes shining. */
    val glow: Boolean = false,
    /** Head tilt / lean, -1..1. */
    val lean: Float = 0f,
    /** Arm length factor. */
    val arms: Float = 1f,
    /** How ragged the outline is. */
    val amp: Float = 6f,
    /** Seen through: fog-like creatures are half transparent. */
    val alpha: Float = 1f,
)
