package similaritysearch.paintings

import org.apache.http.HttpHost
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.*
import org.elasticsearch.script.Script
import org.elasticsearch.script.Script.DEFAULT_SCRIPT_LANG
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink


@Repository
class PaintingsRepository {

    private var client: RestHighLevelClient = RestHighLevelClient(
            RestClient.builder(HttpHost("localhost", 9200, "http")))

    /**
     * Returns a random sample of paintings
     */
    fun samplePaintings(): Mono<List<Painting>> {
        return Mono.create {
            val searchRequest = SearchRequest().indices(PAINTINGS_INDEX)
            val searchSourceBuilder = SearchSourceBuilder()
            searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(50).docValueField("Painting")
            searchRequest.source(searchSourceBuilder)
            val listener: ActionListener<SearchResponse> = createListener(it)
            client.searchAsync(searchRequest, RequestOptions.DEFAULT, listener)
        }
    }

    fun getPaintingById(paintingId: String): Mono<Painting> {
        return Mono.create { monoSink ->
            val listener = object : ActionListener<GetResponse> {
                override fun onResponse(getResponse: GetResponse) {
                    val map = getResponse.source["Painting"] as Map<*, *>
                    monoSink.success(Painting(
                            id = paintingId,
                            title = map.getOrDefault("title", "") as String,
                            date = map["date"] as String?,
                            artist = map.getOrDefault("artist", "") as String,
                            genre = map.getOrDefault("genre", "") as String,
                            style = map.getOrDefault("style", "") as String,
                            score = 1.0))
                }

                override fun onFailure(e: Exception) {
                    monoSink.error(e)
                }

            }
            val getResponse = client.getAsync(GetRequest(PAINTINGS_INDEX, paintingId), RequestOptions.DEFAULT, listener)

        }
    }


    fun findSimilarPaintings(paintingId: String, vectorType: String): Mono<List<Painting>> {
        return Mono.create {
            val getResponse = client.get(GetRequest(PAINTINGS_INDEX, paintingId), RequestOptions.DEFAULT)
            if (getResponse.isSourceEmpty) {
                it.success(emptyList())
            }
            val painting = getResponse.source.get("Painting") as Map<*, *>
            val knownVector: List<Double> = (painting["vectorFeatures"] as Map<*, *>)[vectorType] as List<Double>
            if (knownVector.none { value -> value > 0 }) {
                it.error(IllegalArgumentException("query vector is all zeros"))
                return@create
            }
            val script = Script(
                    ScriptType.INLINE,
                    DEFAULT_SCRIPT_LANG,
                    """double score = cosineSimilarity(params.queryVector, doc['Painting.vectorFeatures.$vectorType']) + 1.0;
                       score >= 0 ? score : 0
                    """.trimIndent(),
                    mapOf("queryVector" to knownVector))
            val searchRequest = SearchRequest()
                    .indices(PAINTINGS_INDEX)
                    .source(
                            SearchSourceBuilder.searchSource()
                                    .query(scriptScoreQuery(QueryBuilders.boolQuery()
                                            .mustNot(matchQuery("_id", paintingId))
                                            .must(existsQuery("Painting.vectorFeatures.$vectorType"))
                                            , script))
                                    .size(NUM_RESULTS)
                                    .docValueField("Painting")
                                    .fetchSource(null, arrayOf("vectorFeatures"))
                    )
            client.searchAsync(searchRequest, RequestOptions.DEFAULT, createListener(it))
        }
    }


    companion object {
        private const val PAINTINGS_INDEX = "paintings"
        private const val NUM_RESULTS = 16

        private fun mapToPainting(hit: SearchHit): Painting {
            val map = hit.sourceAsMap["Painting"] as Map<*, *>
            return Painting(
                    id = hit.id as String,
                    title = map.getOrDefault("title", "") as String,
                    date = map["date"] as String?,
                    artist = map.getOrDefault("artist", "") as String,
                    genre = map.getOrDefault("genre", "") as String,
                    style = map.getOrDefault("style", "") as String,
                    score = hit.score)
        }

        private fun createListener(monoSink: MonoSink<List<Painting>>): ActionListener<SearchResponse> {
            return object : ActionListener<SearchResponse> {
                override fun onResponse(searchResponse: SearchResponse) {
                    monoSink.success(searchResponse.hits.map(::mapToPainting))
                }

                override fun onFailure(e: Exception) {
                    monoSink.error(e)
                }

            }
        }
    }
}
