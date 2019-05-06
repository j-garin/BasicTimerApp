package com.jgarin.basictimerapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

	private val viewModel by lazy {
		ViewModelProviders.of(this, TimerViewModel.Factory(PreferenceManager.getDefaultSharedPreferences(this)))
				.get(TimerViewModel::class.java)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		input.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable) {
				viewModel.inputChanged(s.toString())
			}

			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
			}

			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
			}
		})
		btnStart.setOnClickListener { viewModel.startTimer() }

		viewModel.stateLiveData.observeNotNull(this, ::renderState)
	}

	private fun renderState(state: TimerViewModel.ViewState) {
		state.inputError?.content?.let { input.error = it.message }
		btnStart.isEnabled = state.isStartBtnEnabled

		if (state.isTimerExpired) {
			tvRemainingTime.text = getString(R.string.timer_expired)
		} else {
			tvRemainingTime.text = getString(R.string.xx_remaining, state.remainingTime)
		}

	}
}
