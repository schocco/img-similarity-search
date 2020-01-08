package similaritysearch.paintings

data class Painting(
        val id: String,
        val name: String,
        val artist: String,
        val genre: String,
        val style: String,
        val score: Number
) {
}
