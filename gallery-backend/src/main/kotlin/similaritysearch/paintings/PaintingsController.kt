package similaritysearch.paintings

import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RequestMapping("/api/paintings")
@RestController
class PaintingsController(val paintingsRepository: PaintingsRepository) {

    @GetMapping
    fun paintings(): Mono<List<Painting>> {
        return paintingsRepository.samplePaintings()
    }

    @GetMapping("{paintingId}")
    fun painting(@PathVariable paintingId: String): Mono<Painting> {
        return paintingsRepository.getPaintingById(paintingId)
    }

    @GetMapping("{paintingId}/similar-paintings")
    fun similarPaintings(@PathVariable paintingId: String, @RequestParam feature: String): Mono<List<Painting>> {
        return paintingsRepository.findSimilarPaintings(paintingId, feature)
    }
}
