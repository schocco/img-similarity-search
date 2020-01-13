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
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder
import org.elasticsearch.script.Script
import org.elasticsearch.script.Script.DEFAULT_SCRIPT_LANG
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
            val functionScoreQuery = QueryBuilders.functionScoreQuery(RandomScoreFunctionBuilder())
            searchSourceBuilder.query(functionScoreQuery).size(40).docValueField("Painting")
            searchRequest.source(searchSourceBuilder)
            val listener: ActionListener<SearchResponse> = createSearchListener(it)
            client.searchAsync(searchRequest, RequestOptions.DEFAULT, listener)
        }
    }

    fun getPaintingById(paintingId: String): Mono<Painting> {
        val result: Mono<Map<*, *>> = Mono.create { monoSink ->
            val listener = createGetListener(monoSink)
            client.getAsync(GetRequest(PAINTINGS_INDEX, paintingId), RequestOptions.DEFAULT, listener)
        }
        return result.map { map ->
            Painting(
                    id = paintingId,
                    title = map.getOrDefault("title", "") as String,
                    date = map["date"] as String?,
                    artist = map.getOrDefault("artist", "") as String,
                    genre = map.getOrDefault("genre", "") as String,
                    style = map.getOrDefault("style", "") as String,
                    score = 1.0)
        }
    }

    fun findSimilarPaintings(paintingId: String, vectorType: String): Mono<List<Painting>> {
        val getRequestResult: Mono<Map<*, *>> = Mono.create { monoSink ->
            val listener = createGetListener(monoSink)
            client.getAsync(GetRequest(PAINTINGS_INDEX, paintingId), RequestOptions.DEFAULT, listener)
        }
        return getRequestResult.flatMap { result -> findSimilarPaintings(result, vectorType) }
    }

    private fun findSimilarPaintings(painting: Map<*, *>, vectorType: String): Mono<List<Painting>> {
        val cosineSimilarityScript = "double score = cosineSimilarity(params.queryVector, doc['Painting.vectorFeatures.$vectorType']) + 1.0; score >= 0 ? score : 0"
        val l2normScript = "double score = 1 / (1 + l2norm(params.queryVector, doc['Painting.vectorFeatures.$vectorType'])); score >= 0 ? score : 0"

        return Mono.create<List<Painting>> {
            val knownVector: List<Double> = (painting["vectorFeatures"] as Map<*, *>)[vectorType] as List<Double>
            if (knownVector.none { value -> value > 0 }) {
                it.error(IllegalArgumentException("query vector is all zeros"))
                return@create
            }
            val script = Script(ScriptType.INLINE, DEFAULT_SCRIPT_LANG, l2normScript, mapOf("queryVector" to knownVector))
            val searchRequest = SearchRequest()
                    .indices(PAINTINGS_INDEX)
                    .source(
                            SearchSourceBuilder.searchSource()
                                    .query(scriptScoreQuery(QueryBuilders.boolQuery()
                                            .mustNot(matchQuery("_id", painting["filename"]))
                                            .must(existsQuery("Painting.vectorFeatures.$vectorType"))
                                            , script))
                                    .size(NUM_RESULTS)
                                    .docValueField("Painting")
                                    .fetchSource(null, arrayOf("vectorFeatures"))
                    )
            client.searchAsync(searchRequest, RequestOptions.DEFAULT, createSearchListener(it))
        }

    }


    companion object {
        private const val PAINTINGS_INDEX = "paintings"
        private const val NUM_RESULTS = 16
        private val LOGGER: Logger = LoggerFactory.getLogger(PaintingsRepository::class.java)

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

        private fun createGetListener(monoSink: MonoSink<Map<*, *>>): ActionListener<GetResponse> {
            return object : ActionListener<GetResponse> {
                override fun onResponse(getResponse: GetResponse) {
                    val map = getResponse.source["Painting"] as Map<*, *>
                    monoSink.success(map)
                }

                override fun onFailure(e: Exception) {
                    monoSink.error(e)
                }
            }
        }

        private fun createSearchListener(monoSink: MonoSink<List<Painting>>): ActionListener<SearchResponse> {
            return object : ActionListener<SearchResponse> {
                override fun onResponse(searchResponse: SearchResponse) {
                    LOGGER.info("Query took ${searchResponse.took.millis} ms")
                    monoSink.success(searchResponse.hits.map(::mapToPainting))
                }

                override fun onFailure(e: Exception) {
                    monoSink.error(e)
                }

            }
        }
    }
}
