package org.icpclive.cds.plugins.codeforces

import kotlinx.datetime.Clock
import kotlinx.serialization.*
import org.icpclive.api.ContestStatus
import org.icpclive.cds.common.*
import org.icpclive.cds.plugins.codeforces.api.data.CFHack
import org.icpclive.cds.plugins.codeforces.api.data.CFSubmission
import org.icpclive.cds.plugins.codeforces.api.results.CFStandings
import org.icpclive.cds.plugins.codeforces.api.results.CFStatusWrapper
import org.icpclive.cds.settings.*
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Serializable
@SerialName("cf")
public class CFSettings(
    public val contestId: Int,
    @Contextual public val apiKey: Credential,
    @Contextual public val apiSecret: Credential,
    public val asManager: Boolean = true,
) : CDSSettings() {
    override fun toDataSource() = CFDataSource(this)
}

internal class CFDataSource(val settings: CFSettings) : FullReloadContestDataSource(5.seconds) {
    private val contestInfo = CFContestInfo()
    private val apiKey = settings.apiKey.value
    private val apiSecret = settings.apiSecret.value

    private fun apiRequestUrl(
        method: String,
        params: Map<String, String>
    ): String {
        val sortedParams = params.toSortedMap()
        sortedParams["time"] = Clock.System.now().epochSeconds.toString()
        sortedParams["apiKey"] = apiKey
        val rand = (Random.nextInt(900000) + 100000).toString()
        sortedParams["apiSig"] = rand + hash(sortedParams.toQuery("$rand/$method?", "#$apiSecret"))
        return sortedParams.toQuery("https://codeforces.com/api/$method?")
    }

    private val standingsLoader = jsonUrlLoader<CFStatusWrapper<CFStandings>>(networkSettings = settings.network) {
        apiRequestUrl(
            "contest.standings",
            mapOf(
                "contestId" to settings.contestId.toString(),
                "asManager" to settings.asManager.toString()
            )
        )
    }.map {
        it.unwrap()
    }

    private val statusLoader = jsonUrlLoader<CFStatusWrapper<List<CFSubmission>>>(networkSettings = settings.network) {
        apiRequestUrl(
            "contest.status",
            mapOf(
                "contestId" to settings.contestId.toString(),
                "asManager" to settings.asManager.toString()
            )
        )
    }.map {
        it.unwrap()
    }

    private val hacksLoader = jsonUrlLoader<CFStatusWrapper<List<CFHack>>>(networkSettings = settings.network) {
        apiRequestUrl(
            "contest.hacks",
            mapOf(
                "contestId" to settings.contestId.toString(),
                "asManager" to settings.asManager.toString()
            )
        )
    }.map {
        it.unwrap()
    }


    override suspend fun loadOnce(): ContestParseResult {
        // can change inside previous if, so we do recheck, not else.
        contestInfo.updateStandings(standingsLoader.load())
        val runs = if (contestInfo.status == ContestStatus.BEFORE) emptyList() else contestInfo.parseSubmissions(statusLoader.load())
        val hacks = if (contestInfo.status == ContestStatus.BEFORE) emptyList() else contestInfo.parseHacks(hacksLoader.load())
        return ContestParseResult(contestInfo.toApi(), runs + hacks, emptyList())
    }

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        private fun hash(s: String): String =
            MessageDigest.getInstance("SHA-512")
                .digest(s.toByteArray())
                .toHexString()
        private fun SortedMap<String, String>.toQuery(prefix: String, postfix: String="") =
            entries.joinToString(prefix = prefix, postfix = postfix, separator = "&") {
                "${it.key}=${it.value}"
            }
    }
}