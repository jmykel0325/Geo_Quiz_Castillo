package com.example.geoquiz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

private const val TAG = "QuizActivity"
private const val KEY_INDEX = "index"
private const val KEY_ANSWERED = "answered_array"
private const val KEY_CORRECT_COUNT = "correct_count"
private const val KEY_TOTAL_ANSWERED = "total_answered"
private const val KEY_CHEATED = "cheated_array"

const val EXTRA_ANSWER_IS_TRUE = "com.example.geoquiz.answer_is_true"
const val EXTRA_ANSWER_SHOWN = "com.example.geoquiz.answer_shown"

class MainActivity : ComponentActivity() {

    private lateinit var trueButton: Button
    private lateinit var falseButton: Button
    private lateinit var prevButton: ImageButton
    private lateinit var mNextButton: ImageButton
    private lateinit var cheatButton: Button
    private lateinit var mQuestionTextView: TextView
    private var answeredLabelTextView: TextView? = null
    private lateinit var quizContainer: View
    private lateinit var scoreContainer: View
    private lateinit var finalScoreValueTextView: TextView
    private lateinit var tryAgainButton: Button
    private lateinit var progressContainer: LinearLayout
    private val stepCircleViews = mutableListOf<View>()
    private val stepNumberTexts = mutableListOf<TextView>()
    private val stepCheckIcons = mutableListOf<ImageView>()

    private val mQuestionBank: Array<Questions> = arrayOf(
        Questions(R.string.question_australia, true),
        Questions(R.string.question_oceans, true),
        Questions(R.string.question_mideast, false),
        Questions(R.string.question_africa, false),
        Questions(R.string.question_americas, true),
        Questions(R.string.question_asia, true)
    )

    private var mCurrentIndex = 0
    private var mCorrectAnswers = 0
    private var mTotalAnswered = 0

    private val mAnswered: BooleanArray = BooleanArray(mQuestionBank.size)
    private val mCorrect: BooleanArray = BooleanArray(mQuestionBank.size)
    private val mCheated: BooleanArray = BooleanArray(mQuestionBank.size)

    private val cheatLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.getBooleanExtra(EXTRA_ANSWER_SHOWN, false) == true) {
                mCheated[mCurrentIndex] = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate(Bundle) called")
        setContentView(R.layout.activity_quiz)

        if (savedInstanceState != null) {
            mCurrentIndex = savedInstanceState.getInt(KEY_INDEX, 0)
            mCorrectAnswers = savedInstanceState.getInt(KEY_CORRECT_COUNT, 0)
            mTotalAnswered = savedInstanceState.getInt(KEY_TOTAL_ANSWERED, 0)

            val answeredArray = savedInstanceState.getBooleanArray(KEY_ANSWERED)
            if (answeredArray != null) {
                val limit = minOf(mAnswered.size, answeredArray.size)
                for (i in 0 until limit) {
                    val answered = answeredArray[i]
                    mAnswered[i] = answered
                    mQuestionBank[i].isAnswered = answered
                }
            }

            val cheatedArray = savedInstanceState.getBooleanArray(KEY_CHEATED)
            if (cheatedArray != null) {
                val limit = minOf(mCheated.size, cheatedArray.size)
                for (i in 0 until limit) {
                    mCheated[i] = cheatedArray[i]
                }
            }
        }

        mQuestionTextView = findViewById(R.id.question_text_view)
        answeredLabelTextView = findViewById(R.id.answered_label_text_view)
        quizContainer = findViewById(R.id.quiz_container)
        scoreContainer = findViewById(R.id.score_container)
        finalScoreValueTextView = findViewById(R.id.final_score_value_text_view)
        tryAgainButton = findViewById(R.id.try_again_button)
        progressContainer = findViewById(R.id.progressContainer)
        mQuestionTextView.setOnClickListener {
            mCurrentIndex = (mCurrentIndex + 1) % mQuestionBank.size
            Log.d(TAG, "Question TextView clicked, mCurrentIndex = $mCurrentIndex")
            updateQuestion()
        }

        trueButton = findViewById(R.id.true_button)
        trueButton.setOnClickListener {
            checkAnswer(true)
        }

        falseButton = findViewById(R.id.false_button)
        falseButton.setOnClickListener {
            checkAnswer(false)
        }

        cheatButton = findViewById(R.id.cheat_button)
        cheatButton.setOnClickListener {
            val answerIsTrue = mQuestionBank[mCurrentIndex].isAnswerTrue
            val intent = Intent(this, CheatActivity::class.java).apply {
                putExtra(EXTRA_ANSWER_IS_TRUE, answerIsTrue)
            }
            cheatLauncher.launch(intent)
        }

        prevButton = findViewById(R.id.prev_button)
        prevButton.setOnClickListener {
            mCurrentIndex = (mCurrentIndex - 1 + mQuestionBank.size) % mQuestionBank.size
            Log.d(TAG, "Prev button pressed, mCurrentIndex = $mCurrentIndex")
            updateQuestion()
        }

        mNextButton = findViewById(R.id.next_button)
        mNextButton.setOnClickListener {
            mCurrentIndex = (mCurrentIndex + 1) % mQuestionBank.size
            Log.d(TAG, "Next button pressed, mCurrentIndex = $mCurrentIndex")
            updateQuestion()
        }

        tryAgainButton.setOnClickListener {
            resetQuiz()
        }

        // Build step indicator once, then keep it in sync with state
        buildProgressIndicator(mQuestionBank.size)

        showQuiz()
        updateQuestion()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() called")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.i(TAG, "onSaveInstanceState")
        outState.putInt(KEY_INDEX, mCurrentIndex)
        outState.putInt(KEY_CORRECT_COUNT, mCorrectAnswers)
        outState.putInt(KEY_TOTAL_ANSWERED, mTotalAnswered)

        val answeredArray = BooleanArray(mAnswered.size)
        for (i in mAnswered.indices) {
            answeredArray[i] = mAnswered[i]
        }
        outState.putBooleanArray(KEY_ANSWERED, answeredArray)

        val cheatedArray = BooleanArray(mCheated.size)
        for (i in mCheated.indices) {
            cheatedArray[i] = mCheated[i]
        }
        outState.putBooleanArray(KEY_CHEATED, cheatedArray)
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
    }

    private fun showQuiz() {
        quizContainer.visibility = View.VISIBLE
        scoreContainer.visibility = View.GONE
    }

    private fun showFinalScore() {
        val totalQuestions = mQuestionBank.size
        val finalCorrect = mCorrect.count { it }
        finalScoreValueTextView.text = "$finalCorrect/$totalQuestions"

        quizContainer.visibility = View.GONE
        scoreContainer.visibility = View.VISIBLE
    }

    private fun resetQuiz() {
        mCurrentIndex = 0
        mCorrectAnswers = 0
        mTotalAnswered = 0

        for (i in mQuestionBank.indices) {
            mAnswered[i] = false
            mCorrect[i] = false
            mCheated[i] = false
            mQuestionBank[i].isAnswered = false
        }

        showQuiz()
        updateQuestion()
    }

    private fun updateQuestion() {
        Log.d(TAG, "updateQuestion() called, mCurrentIndex = $mCurrentIndex")
        Log.d(TAG, "updateQuestion() stack trace", Exception())

        val question = mQuestionBank[mCurrentIndex].textResId
        mQuestionTextView.setText(question)
        
        val isAnsweredFlag = mQuestionBank[mCurrentIndex].isAnswered
        val alreadyAnswered = mAnswered[mCurrentIndex] || isAnsweredFlag

        answeredLabelTextView?.visibility = if (alreadyAnswered) android.view.View.VISIBLE else android.view.View.GONE

        if (alreadyAnswered) {
            trueButton.isEnabled = false
            falseButton.isEnabled = false
        } else {
            trueButton.isEnabled = !isAnsweredFlag
            falseButton.isEnabled = !isAnsweredFlag
        }

        // Update visual step progress for current/answered questions
        updateProgressIndicator()
    }

    private fun checkAnswer(userPressedTrue: Boolean) {
        val answerIsTrue = mQuestionBank[mCurrentIndex].isAnswerTrue

        mQuestionBank[mCurrentIndex].isAnswered = true
        mAnswered[mCurrentIndex] = true
        mTotalAnswered++

        if (mCheated[mCurrentIndex]) {
            // Chapter 5: cheating makes this question worth 0 points.
            mCorrect[mCurrentIndex] = false
            Toast.makeText(this, R.string.judgment_toast, Toast.LENGTH_SHORT).show()
        } else {
            val isCorrect = (userPressedTrue == answerIsTrue)
            mCorrect[mCurrentIndex] = isCorrect

            val messageResId = if (isCorrect) {
                mCorrectAnswers++
                R.string.correct_toast
            } else {
                R.string.incorrect_toast
            }
            Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
        }

        trueButton.isEnabled = false
        falseButton.isEnabled = false

        if (mTotalAnswered == mQuestionBank.size) {
            val scorePercent = (mCorrectAnswers.toDouble() / mQuestionBank.size * 100).toInt()
            val scoreMessage = "Quiz Complete! Your score is $scorePercent%"
            Toast.makeText(this, scoreMessage, Toast.LENGTH_LONG).show()

            showFinalScore()
        }

        // Update visual step progress when a question is answered
        updateProgressIndicator()
    }

    private fun buildProgressIndicator(total: Int) {
        stepCircleViews.clear()
        stepNumberTexts.clear()
        stepCheckIcons.clear()
        progressContainer.removeAllViews()

        val density = resources.displayMetrics.density
        val circleSize = (32 * density).toInt()
        val circleMargin = (4 * density).toInt()
        val lineWidth = (24 * density).toInt()
        val lineHeight = (2 * density).toInt()

        for (i in 0 until total) {
            val stepLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val circleFrame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(circleSize, circleSize).apply {
                    setMargins(circleMargin, circleMargin, circleMargin, circleMargin)
                }
                background = getDrawable(R.drawable.circle_outlined)
            }

            val numberText = TextView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                text = (i + 1).toString()
                setTextColor(resources.getColor(android.R.color.white, theme))
            }

            val checkIcon = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                setImageResource(R.drawable.ic_check)
                visibility = View.GONE
            }

            circleFrame.addView(numberText)
            circleFrame.addView(checkIcon)

            stepLayout.addView(circleFrame)

            if (i < total - 1) {
                val lineView = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(lineWidth, lineHeight)
                    setBackgroundResource(R.drawable.progress_line)
                }
                stepLayout.addView(lineView)
            }

            progressContainer.addView(stepLayout)
            stepCircleViews.add(circleFrame)
            stepNumberTexts.add(numberText)
            stepCheckIcons.add(checkIcon)
        }

        updateProgressIndicator()
    }

    private fun updateProgressIndicator() {
        val total = mQuestionBank.size
        if (stepCircleViews.size != total) return

        for (i in 0 until total) {
            val circle = stepCircleViews[i]
            val numberText = stepNumberTexts[i]
            val checkIcon = stepCheckIcons[i]
            val answered = mAnswered[i]

            when {
                answered -> {
                    circle.background = getDrawable(R.drawable.circle_completed)
                    numberText.visibility = View.GONE
                    checkIcon.visibility = View.VISIBLE
                }
                i == mCurrentIndex -> {
                    circle.background = getDrawable(R.drawable.circle_filled)
                    numberText.visibility = View.VISIBLE
                    checkIcon.visibility = View.GONE
                }
                else -> {
                    circle.background = getDrawable(R.drawable.circle_outlined)
                    numberText.visibility = View.VISIBLE
                    checkIcon.visibility = View.GONE
                }
            }
        }
    }
}