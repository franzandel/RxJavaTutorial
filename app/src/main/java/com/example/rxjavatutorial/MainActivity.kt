package com.example.rxjavatutorial

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapUsage() // Map, onNext, Map, onNext
//        flatMapUsage() // FlatMap, FlatMap, onNext, onNext
//        concatMapUsage() // ConcatMap, onNext, ConcatMap, onNext
//        switchMapUsage() // SwitchMap, SwitchMap, onNext(Last Result only)

        //        startRStream()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

    private fun mapUsage() {
        getUsersObservableMap()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
//            .map(object : io.reactivex.functions.Function<User, User> {
//                override fun apply(it: User): User {
//                    it.email = String.format("%s@rxjava.wtf", it.name)
//                    it.name = it.name?.toUpperCase()
//                    return it
//                }
//            })

            // ABOVE IS EQUIVALENT TO BELOW CODE (MAP)

            .map {
                Log.e(TAG, "Map Block: Email = ${it.email} Thread = ${Thread.currentThread().name}")
                it.email = String.format("%s@rxjava.wtf", it.name)
                it.name = it.name?.toUpperCase()
                it
            }
            .subscribe(object : Observer<User> {
                override fun onSubscribe(d: Disposable) {
                    Log.e(TAG, "onSubscribe")
                    disposable = d
                }

                override fun onNext(user: User) {
                    Log.e(TAG, "onNext: ${user.name}, ${user.gender}, ${user.email}, ${Thread.currentThread().name}")
                }

                override fun onError(e: Throwable) {
                    Log.e(TAG, "onError: " + e.message)
                }

                override fun onComplete() {
                    Log.d(TAG, "onComplete")
                }
            })
    }

    private fun flatMapUsage() {
        getUsersObservableFlatConcatMap()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
//            .flatMap(new Function<User, Observable<User>>() {
//
//                @Override
//                public Observable<User> apply(User user) throws Exception {
//
//                    // getting each user address by making another network call
//                    return getAddressObservable(user);
//                }
//            })

            // ABOVE IS EQUIVALENT TO BELOW CODE (FLATMAP)

            .flatMap {
                Log.e(TAG, "FlatMap Block: Name = ${it.name} Thread = ${Thread.currentThread().name}")
                getAddressObservable(it)
            }
            .subscribe(object : Observer<User> {
                override fun onSubscribe(d: Disposable) {
                    Log.e(TAG, "onSubscribe")
                    disposable = d
                }

                override fun onComplete() {
                    Log.e(TAG, "All users emitted!")
                }

                override fun onNext(user: User) {
                    Log.e(TAG, "onNext: ${user.name}, ${user.gender}, ${user.address?.address}, " +
                            Thread.currentThread().name
                    )
                }

                override fun onError(e: Throwable) {

                }
            })
    }

    private fun concatMapUsage() {
        getUsersObservableFlatConcatMap()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
//            .concatMap(object : Function<User, Observable<User>>() {
//
//                @Throws(Exception::class)
//                fun apply(user: User): Observable<User> {
//
//                    // getting each user address by making another network call
//                    return getAddressObservable(user)
//                }
//            })

            // ABOVE IS EQUIVALENT TO BELOW CODE (CONCATMAP)

            .concatMap {
                Log.e(TAG, "ConcatMap Block: Name = ${it.name} Thread = ${Thread.currentThread().name}")
                getAddressObservable(it)
            }
            .subscribe(object : Observer<User> {
                override fun onSubscribe(d: Disposable) {
                    Log.e(TAG, "onSubscribe")
                    disposable = d
                }

                override fun onNext(user: User) {
                    Log.e(TAG, "onNext: ${user.name}, ${user.gender}, ${user.address?.address}, " +
                            Thread.currentThread().name
                    )

                }

                override fun onError(e: Throwable) {

                }

                override fun onComplete() {
                    Log.e(TAG, "All users emitted!")
                }
            })
    }

    private fun switchMapUsage() {
        val integerObservable = Observable.fromArray(1, 2, 3, 4, 5, 6)

        // it always emits 6 as it un-subscribes the before observer
        integerObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
//            .switchMap(object : Function<Int, ObservableSource<Int>>() {
//                @Throws(Exception::class)
//                fun apply(integer: Int?): ObservableSource<Int> {
//                    return Observable.just(integer!!).delay(1, TimeUnit.SECONDS)
//                }
//            })

            // ABOVE IS EQUIVALENT TO BELOW CODE (CONCATMAP)

            .switchMap {
                Log.e(TAG, "SwitchMap Block: Name = $it Thread = ${Thread.currentThread().name}")
                Observable.just(it).delay(1, TimeUnit.SECONDS)
            }
            .subscribe(object : Observer<Int> {
                override fun onSubscribe(d: Disposable) {
                    Log.d(TAG, "onSubscribe")
                    disposable = d
                }

                override fun onNext(integer: Int) {
                    Log.d(TAG, "onNext: $integer, ${Thread.currentThread().name}")
                }

                override fun onError(e: Throwable) {

                }

                override fun onComplete() {
                    Log.d(TAG, "All users emitted!")
                }
            })
    }

    /**
     * Assume this as a network call
     * returns Users with address filed added
     */
    private fun getAddressObservable(user: User): Observable<User> {

        val addresses = arrayOf(
            "1600 Amphitheatre Parkway, Mountain View, CA 94043",
            "2300 Traverwood Dr. Ann Arbor, MI 48105",
            "500 W 2nd St Suite 2900 Austin, TX 78701",
            "355 Main Street Cambridge, MA 02142"
        )

        return Observable
            .create(ObservableOnSubscribe<User> { emitter ->
                val address = Address()
                address.address = addresses[Random().nextInt(2) + 0]
                if (!emitter.isDisposed) {
                    user.address = address


                    // Generate network latency of random duration
                    val sleepTime = Random().nextInt(1000) + 500

                    Thread.sleep(sleepTime.toLong())
                    emitter.onNext(user)
                    emitter.onComplete()
                }
            }).subscribeOn(Schedulers.io())
    }

    /**
     * Assume this is a network call to fetch users
     * returns Users with name and gender but missing address
     */
    private fun getUsersObservableFlatConcatMap(): Observable<User> {
        val maleUsers = arrayOf("Mark", "John", "Trump", "Obama")

        val users = ArrayList<User>()

        for (name in maleUsers) {
            val user = User()
            user.name = name
            user.gender = "male"

            users.add(user)
        }

        return Observable
            .create(ObservableOnSubscribe<User> { emitter ->
                for (user in users) {
                    if (!emitter.isDisposed) {
                        emitter.onNext(user)
                    }
                }

                if (!emitter.isDisposed) {
                    emitter.onComplete()
                }
            }).subscribeOn(Schedulers.io())
    }

    private fun getUsersObservableMap(): Observable<User> {
        val names = arrayOf("mark", "john", "trump", "obama")

        val users = ArrayList<User>()
        for (name in names) {
            val user = User()
            user.name = name
            user.gender = "male"

            users.add(user)
        }
        return Observable
            .create(ObservableOnSubscribe<User> { emitter ->
                for (user in users) {
                    if (!emitter.isDisposed) {
                        emitter.onNext(user)
                    }
                }

                if (!emitter.isDisposed) {
                    emitter.onComplete()
                }
            }).subscribeOn(Schedulers.io())
    }

    private fun startRStream() {

//Create an Observable//

        val myObservable = getObservable()

//Create an Observer//

        val myObserver = getObserver()

//Subscribe myObserver to myObservable//

        myObservable
            .subscribe(myObserver)
    }

    private fun getObserver(): Observer<String> {
        return object : Observer<String> {
            override fun onSubscribe(d: Disposable) {
            }

//Every time onNext is called, print the value to Android Studio’s Logcat//

            override fun onNext(s: String) {
                Log.d(TAG, "onNext: $s")
            }

//Called if an exception is thrown//

            override fun onError(e: Throwable) {
                Log.e(TAG, "onError: " + e.message)
            }

//When onComplete is called, print the following to Logcat//

            override fun onComplete() {
                Log.d(TAG, "onComplete")
            }
        }
    }

//Give myObservable some data to emit//

    private fun getObservable(): Observable<String> {
        return Observable.just("1", "2", "3", "4", "5")
    }
}
