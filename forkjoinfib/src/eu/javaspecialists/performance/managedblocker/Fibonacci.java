package eu.javaspecialists.performance.managedblocker;

import eu.javaspecialists.performance.math.*;

import java.util.*;
import java.util.concurrent.*;

public class Fibonacci {

  static final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

  public static BigInteger f(int n) {

    ConcurrentHashMap<Integer, ForkJoinTask<BigInteger>> cache = new ConcurrentHashMap<>(3 * (1 + (int) Math.log(n + 1)));
    cache.put(0, pool.submit(() -> BigInteger.ZERO));
    cache.put(1, pool.submit(() -> BigInteger.ONE));
    return pool.submit(() -> f(n, cache)).join();
  }

  static BigInteger f(int n, ConcurrentHashMap<Integer, ForkJoinTask<BigInteger>> cache) {

    ForkJoinTask<BigInteger> computation = ForkJoinTask.adapt(() -> {
      int half = (n + 1) / 2;
      ForkJoinTask<BigInteger> f0_task = ForkJoinTask.adapt(() -> f(half - 1, cache)).fork();
      BigInteger f1 = f(half, cache);
      BigInteger f0 = f0_task.join();
      return (n % 2 == 1)
          ? f0.multiply(f0).add(f1.multiply(f1))
          : f0.shiftLeft(1).add(f1).multiply(f1);
    });

    ForkJoinTask<BigInteger> alreadyComputed = cache.putIfAbsent(n, computation);

    return (alreadyComputed != null)
        ? alreadyComputed.join()
        : computation.invoke();
  }
}
