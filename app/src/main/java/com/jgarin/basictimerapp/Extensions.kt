package com.jgarin.basictimerapp

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

fun <I, O> LiveData<I>.scan(initial: O, scanner: (I, O) -> O): LiveData<O> {
    var data = initial
    val result = MediatorLiveData<O>().apply { postValue(data) }
    result.addSource(this) {
        data = scanner(it, data)
        result.postValue(data)
    }
    return result
}

fun <I, O> LiveData<I>.forkJoin(vararg forks: LiveData<I>.() -> LiveData<O>): LiveData<O> {
    val result = MediatorLiveData<O>()
    for (fork in forks) {
        result.addSource(this.fork()) {
            result.postValue(it)
        }
    }
    return result
}

fun <T> LiveData<T>.filter(predicate: (T) -> Boolean): LiveData<T> {
    val result = MediatorLiveData<T>()
    result.addSource(this) {
        if (predicate(it)) result.postValue(it)
    }
    return result
}

fun <I, O> LiveData<I>.cast(type: Class<O>): LiveData<O> {
    val result = MediatorLiveData<O>()
    result.addSource(this) {
        result.postValue(it as O)
    }
    return result
}

inline fun <I, reified O> LiveData<I>.ofType(type: Class<O>): LiveData<O> {
    return filter { it is O }.cast(type)
}

fun <T> LiveData<T>.distinctUntilChanged(): LiveData<T> {
    val result = MediatorLiveData<T>()
    var isFirstItem = true
    var lastItem: T? = null
    result.addSource(this) { item ->
        if (isFirstItem || lastItem != item) {
            lastItem = item
            isFirstItem = false
            result.postValue(item)
        }
    }
    return result
}

fun <I, O> LiveData<I>.map(mapper: (I) -> O): LiveData<O> {
    return Transformations.map(this, mapper)
}

fun <T> LiveData<T>.observeNotNull(owner: LifecycleOwner, observer: (T) -> Unit) {
    observe(owner, Observer<T> { it?.let(observer) })
}