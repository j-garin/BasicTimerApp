package com.jgarin.basictimerapp

import android.content.SharedPreferences
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TimerViewModel(private val prefs: SharedPreferences) : ViewModel() {

	private val timeFormat = SimpleDateFormat("mm:ss", Locale.US)


	private var timestamp = prefs.getLong(TIMESTAMP_KEY, TIMESTAMP_NONE)
		set(value) {
			if (field != value) {
				field = value
				prefs.edit().putLong(TIMESTAMP_KEY, value).apply()
			}
		}

	private val actions = MutableLiveData<Action>()
	private val state = actions
			.forkJoin<Action, Result>(
					{ ofType(Action.InputChanged::class.java).map { Result.InputChanged(it.input) } },
					{
						ofType(Action.StartTimer::class.java).map {
							timer?.cancel()
							timestamp = System.currentTimeMillis() + it.millis
							timer = object : CountDownTimer(it.millis, COUNTDOWN_INTERVAL) {
								override fun onFinish() {
									actions.postValue(Action.TimerFinished)
								}

								override fun onTick(millisUntilFinished: Long) {
									actions.postValue(Action.OnTick(millisUntilFinished))
								}
							}
							timer!!.start()
							Result.StartTimer
						}
					},
					{ ofType(Action.TimerFinished::class.java).map { Result.TimerFinished } },
					{ ofType(Action.OnTick::class.java).map { Result.OnTick(it.remainingTime) } }
			)
			.scan(State.default) { result, prev ->
				when (result) {
					is Result.InputChanged -> try {
						prev.copy(input = result.input.toLong(), inputError = null)
					} catch (e: Exception) {
						prev.copy(input = 0L, inputError = if (result.input.isEmpty()) null
						else SingleLiveEvent(IllegalArgumentException("Input must be a number")))
					}
					Result.StartTimer      -> prev.copy(isTimerExpired = false)
					Result.TimerFinished   -> prev.copy(isTimerExpired = true, remainingTime = 0L)
					is Result.OnTick       -> prev.copy(
							remainingTime = result.remainingTime
					)
				}
			}

	val stateLiveData: LiveData<ViewState> = state
			.map { state ->
				ViewState(
						isStartBtnEnabled = state.input != 0L,
						inputError = state.inputError,
						isTimerExpired = state.isTimerExpired,
						remainingTime = timeFormat.format(Date(state.remainingTime))
				)
			}
			.distinctUntilChanged()

	private var timer: CountDownTimer? = null

	init {
		val currentTime = System.currentTimeMillis()
		when {
			timestamp == TIMESTAMP_NONE -> {
			} // no timer saved
			timestamp <= currentTime    -> actions.postValue(Action.TimerFinished) //expired
			else                        -> actions.postValue(Action.StartTimer(timestamp - currentTime))
		}
	}


	private sealed class Action {
		class InputChanged(val input: String) : Action()
		class StartTimer(val millis: Long) : Action()
		object TimerFinished : Action()
		class OnTick(val remainingTime: Long) : Action()
	}

	private sealed class Result {
		class InputChanged(val input: String) : Result()
		object StartTimer : Result()
		object TimerFinished : Result()
		class OnTick(val remainingTime: Long) : Result()
	}

	private data class State(
			val input: Long,
			val isStartBtnEnabled: Boolean,
			val inputError: SingleLiveEvent<Throwable>?,
			val isTimerExpired: Boolean,
			val remainingTime: Long
	) {
		companion object {
			val default = State(
					input = 0L,
					isStartBtnEnabled = false,
					inputError = null,
					isTimerExpired = false,
					remainingTime = 0L
			)
		}
	}

	data class ViewState(
			val isStartBtnEnabled: Boolean,
			val inputError: SingleLiveEvent<Throwable>?,
			val isTimerExpired: Boolean,
			val remainingTime: String
	)

	fun inputChanged(input: String) = actions.postValue(Action.InputChanged(input))

	fun startTimer() = actions.postValue(Action.StartTimer(TimeUnit.MINUTES.toMillis(state.value!!.input)))

	class Factory(private val prefs: SharedPreferences) : ViewModelProvider.Factory {
		override fun <T : ViewModel?> create(modelClass: Class<T>): T {
			return if (modelClass.isAssignableFrom(TimerViewModel::class.java)) TimerViewModel(prefs) as T
			else throw IllegalArgumentException("Unable to create instance of ${modelClass.name}")
		}
	}

	companion object {
		private const val TIMESTAMP_KEY = "timestamp"
		private const val TIMESTAMP_NONE = -1L
		private const val COUNTDOWN_INTERVAL = 1000L
	}

}