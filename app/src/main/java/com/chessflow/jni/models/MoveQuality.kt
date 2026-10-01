package com.chessflow.jni.models

import androidx.annotation.StringRes
import com.chessflow.jni.R

enum class MoveQuality(
    @StringRes val labelRes: Int,
    val emoji: String
) {
    EXCELLENT(R.string.quality_best, "⭐"),
    GOOD(R.string.quality_good, "👍"),
    INACCURACY(R.string.quality_inaccuracy, "❓"),
    MISTAKE(R.string.quality_mistake, "⚠️"),
    BLUNDER(R.string.quality_blunder, "❌");

    companion object {
        fun fromCentipawnLoss(cpLoss: Int): MoveQuality {
            return when {
                cpLoss <= 10 -> EXCELLENT
                cpLoss <= 30 -> GOOD
                cpLoss <= 80 -> INACCURACY
                cpLoss <= 200 -> MISTAKE
                else -> BLUNDER
            }
        }
    }
}