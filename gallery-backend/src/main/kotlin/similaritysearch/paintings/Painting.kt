package similaritysearch.paintings

data class Painting(
        val id: String,
        val title: String,
        val date : String?,
        val artist: String,
        val genre: String,
        val style: String,
        val score: Number
) {
}
