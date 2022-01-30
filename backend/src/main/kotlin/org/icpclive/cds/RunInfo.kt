package org.icpclive.cds

interface RunInfo : Comparable<RunInfo> {
    val id: Int
    val isAccepted: Boolean
    val isAddingPenalty: Boolean
    val isJudged: Boolean
    val result: String
    val problem: ProblemInfo?
    val problemId: Int
    val teamId: Int
    val isReallyUnknown: Boolean
    val percentage: Double
    val time: Long
    val lastUpdateTime: Long
    val isFirstSolvedRun: Boolean
        get() = EventsLoader.instance.contestData!!.firstSolvedRun[problemId] === this

    override fun compareTo(other: RunInfo): Int {
        return time.compareTo(other.time)
    }

    fun toApi() = org.icpclive.api.RunInfo(
        id,
        isAccepted,
        isJudged,
        result,
        problemId,
        teamId,
        isReallyUnknown,
        percentage,
        time,
        lastUpdateTime,
        isFirstSolvedRun
    )
}