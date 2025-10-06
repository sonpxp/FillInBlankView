package com.sonpxp.blankview.word

class ReviewCalculator {

    fun calculateReviewResults(
        userAnswers: List<String>,
        correctAnswers: List<String>,
        mode: ReviewMode
    ): List<Boolean> {
        return when (mode) {
            ReviewMode.INDIVIDUAL_MATCHING -> calculateIndividualMatching(userAnswers, correctAnswers)
            ReviewMode.ALL_OR_NOTHING -> calculateAllOrNothing(userAnswers, correctAnswers)
        }
    }

    private fun calculateIndividualMatching(
        userAnswers: List<String>,
        correctAnswers: List<String>
    ): List<Boolean> {
        return userAnswers.mapIndexed { index, userAnswer ->
            index < correctAnswers.size && userAnswer == correctAnswers[index]
        }
    }

    private fun calculateAllOrNothing(
        userAnswers: List<String>,
        correctAnswers: List<String>
    ): List<Boolean> {
        val isAllCorrect = userAnswers == correctAnswers
        return List(userAnswers.size) { isAllCorrect }
    }
}