package org.icpclive.events.WF.json

import org.icpclive.events.ProblemInfo
import java.awt.Color

/**
 * Created by Aksenov239 on 3/5/2018.
 */
class WFProblemInfo(
    languages: Int, letter: String, name: String, color: Color,
    val id: Int, val testCount: Int
) : ProblemInfo(letter, name, color) {
    val submissions = IntArray(languages)
}