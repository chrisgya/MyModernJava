package com.training;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class ParallelismAndConcurrency {

    //Concurrency is when multiple tasks can run in OVERLAPPING TIME PERIODS
    //Parallelism is when multiple tasks run at literally the same time

    //Converting from Sequential to Parallel

    //When executing, a stream can be either parallel or sequential. The parallel or sequential methods
    // effectively set or unset a boolean, which is checked when the terminal expression is reached.

    //By default, Java 8 parallel streams use a common fork-join pool to distribute the work.
    //The size of that pool is equal to the number of processors, which you can determine via
    // Runtime.getRuntime().availableProcessors()


    static Long sequentialSum(int N) {
        return Stream.iterate(1L, i -> i + 1)
                .limit(N)
                .peek(i -> System.out.println(Thread.currentThread().getName() + " ->" + i))
                .reduce(0L, Long::sum);
    }

    static Long parallelSum(int N) {
        //this will give you the worse performance. instead, use the LongStream
        System.out.println("Parallel version");
        return Stream.iterate(1L, i -> i + 1)
                .limit(N)
                .parallel()
                .peek(i -> System.out.println(Thread.currentThread().getName() + " ->" + i))
                .reduce(0L, Long::sum);
    }

    static Long sequentialLongStreamSum(int N) {
        return LongStream.rangeClosed(1, N).sum();
    }

    static Long parallelLongStreamSum(int N) {
        System.out.println("Parallel version");
        return LongStream.rangeClosed(1, N).parallel().sum();
    }

    // Submitting a Callable and returning the Future
    static void callableFutureExample() {
        ExecutorService service = Executors.newCachedThreadPool();
        Future<String> future = service.submit(() -> {
            Thread.sleep(1000);
            return "Hello, World!";
        });

        // future.cancel(true);

        System.out.println("Even more processing...");

        getIfNotCancelled(future);
    }

    static void getIfNotCancelled(Future<String> future) {
        try {
            if (!future.isCancelled()) {
                System.out.println(future.get());
            } else {
                System.out.println("Cancelled");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    // Completing a CompletableFuture
    // The runAsync methods are useful if you don’t need to return anything.
    // The supplyAsync methods return an object using the given Supplier

    static Map<Integer, Product> cache = new HashMap<>();

    Product getLocal(int id) {
        return cache.get(id);
    }

    Product getRemote(int id) {
        try {
            Thread.sleep(100);
            if (id == 666) {
                throw new RuntimeException("Evil request");
            }
        } catch (InterruptedException ignored) {
        }
        return new Product(id, "name");
    }

    CompletableFuture<Product> getProduct(int id) {
        try {
            Product product = getLocal(id);
            if (product != null) {
                return CompletableFuture.completedFuture(product);
            } else {
                return CompletableFuture.supplyAsync(() -> {
                    Product p = getRemote(id);
                    cache.put(id, p);
                    return p;
                });
            }
        } catch (Exception e) {
            CompletableFuture<Product> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    //Coordinating CompletableFutures, Part 1

    String sleepThenReturnString() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        return "42";
    }

    //The subsequent thenApply and thenAccept methods use the same thread as the supply Async method.
    //If you use thenApplyAsync, the task will be submitted to the pool, unless you add yet another
    //pool as an additional argument.
    void CoordinatingTasksUsingACompletableFuture() {
        ExecutorService service = Executors.newFixedThreadPool(4);

        //   CompletableFuture.supplyAsync(this::sleepThenReturnString) //using the default joinFolk which use all the available thread on the system
        CompletableFuture.supplyAsync(this::sleepThenReturnString, service) //using custom thread pool
                .thenApply(Integer::parseInt)
                .thenApply(i -> i * 2)
                .thenAccept(System.out::println)
                .join();

        System.out.println("Running...");
    }

    //thenCompose is an instance method that allows you to chain another Future to the original, with the added benefit that the result of the first is available in the second.
    static void thenComposeCompletableFuture() throws ExecutionException, InterruptedException {
        int x = 3;
        int y = 2;

        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> x)
                .thenCompose(n -> CompletableFuture.supplyAsync(() -> n + y));

        System.out.println(completableFuture.get() == 5);
    }

    //If you would rather that the Futures be independent, you can use thenCombine instead
    static void thenCombineCompletableFuture() throws ExecutionException, InterruptedException {
        int x = 3;
        int y = 2;

        var completableFuture = CompletableFuture.supplyAsync(() -> x)
                .thenCombine(CompletableFuture.supplyAsync(() -> y), (n1, n2) -> n1 + n2).join();

        System.out.println(completableFuture == 5);
    }

    //The handle method
    //The two input arguments to the BiFunction are the result of the Future if it completes normally and the thrown
    // exception if not. Your code decides what to return. There are also handleAsyc methods that take either a
    // BiFunction or a BiFunction and an Executor
    private CompletableFuture<Integer> getIntegerCompletableFuture(String num) {
        return CompletableFuture.supplyAsync(() -> Integer.parseInt(num))
                .handle((val, exc) -> val != null ? val : 0);
    }

    void handleWithException() throws Exception {
        String num = "abc";
        CompletableFuture<Integer> value = getIntegerCompletableFuture(num);
        System.out.println(value.get() == 0);
    }

    void handleWithoutException() throws Exception {
        String num = "42";
        CompletableFuture<Integer> value = getIntegerCompletableFuture(num);
        System.out.println(value.get() == 42);
    }


    //Coordinating CompletableFutures, Part 2


    public static void main(String[] args) throws ExecutionException, InterruptedException {

        //In BaseStream (the superclass of the Stream interface), you can use the method isParallel
        // to determine whether the stream is operating sequentially or in parallel.

//        System.out.println(Stream.of(3, 1, 4, 1, 5, 9).isParallel()); //false
//        System.out.println(Stream.iterate(1, n -> n + 1).isParallel()); //false
//        System.out.println(Stream.generate(Math::random).isParallel()); //false
//
//        List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9);
//        System.out.println(numbers.stream().isParallel());  //false

        //If the source was a collection, you can use the parallelStream method to yield a (possibly) parallel stream
        //The reason for the “possibly” qualification is that it is allowable for this method to return a
        // sequential stream, but by default the stream will be parallel. The Javadocs imply that the sequential case
        // will only occur if you create your own spliterator, which is pretty unusual
        //  System.out.println(numbers.parallelStream().isParallel()); //true
        //System.out.println(Stream.of(3, 1, 4, 1, 5, 9).parallel().isParallel()); //true

        //Converting a parallel stream to sequential
        // System.out.println(numbers.parallelStream().sequential().isParallel()); //false

        //  Instant start = Instant.now();
        // System.out.println(sequentialSum(1000000));
        // System.out.println(parallelSum(1000000));
        // System.out.println(sequentialLongStreamSum(1000000000));
        //   System.out.println(parallelLongStreamSum(1000000000));
        //   System.out.println("Duration in ms: " + Duration.between(start, Instant.now()).toMillis());

        // callableFutureExample();

        // thenComposeCompletableFuture();
        thenCombineCompletableFuture();
    }



    /*
    Using completeExceptionally on a CompletableFuture

@Test(expected = ExecutionException.class)
public void testException() throws Exception {
    demo.getProduct(666).get();
}

@Test
public void testExceptionWithCause() throws Exception {
    try {
        demo.getProduct(666).get();
        fail("Houston, we have a problem...");
    } catch (ExecutionException e) {
        assertEquals(ExecutionException.class, e.getClass());
        assertEquals(RuntimeException.class, e.getCause().getClass());
    }
}
     */

    class Product {
        private int id;
        private String name;

        public Product(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

}
