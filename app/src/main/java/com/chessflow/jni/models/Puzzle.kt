package com.chessflow.jni.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Puzzle(
    var puzzleId: Long = 0,
    var fen: String = "",

    // Best move
    var bestMove: String = "",
    var bestMoveSan: String = "",
    var bestMoveEvalCp: Int = 0,

    // Played move
    var playedMove: String = "",
    var playedMoveSan: String = "",
    var playedMoveEvalCp: Int = 0,

    // Evaluations
    var evalDifference: Int = 0,

    // Metadata
    var difficulty: String = "",
    var puzzleType: String = "",
    var moveNumber: Int = 0,
    var sideToMove: String = "",

    // Source game info
    var sourceGame: SourceGame = SourceGame()
) {
    constructor() : this(
        0, "", "", "", 0,
        "", "", 0,
        0, "", "", 0, "", SourceGame()
    )
}

data class SourceGame(
    var White: String = "",
    var Black: String = "",
    var Event: String = "",
    var Date: String = ""
)
