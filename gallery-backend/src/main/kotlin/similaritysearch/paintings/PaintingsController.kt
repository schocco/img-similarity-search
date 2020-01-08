package similaritysearch.paintings

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RequestMapping("/api/paintings")
@RestController
class PaintingsController(val paintingsRepository: PaintingsRepository) {

    @GetMapping
    fun paintings(): Mono<List<Painting>> {
        return paintingsRepository.samplePaintings()
    }

    @GetMapping("{paintingId}/similar-paintings")
    fun similarPaintings(@PathVariable paintingId: String): Mono<List<Painting>> {
        return paintingsRepository.findSimilarPaintings(paintingId)
    }
}
