
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    static AtomicInteger counter = new AtomicInteger();
    static int stock = 100;
    static final Object monitor = new Object();

    public static boolean decrementStock(int items) {
        synchronized (monitor) {
            if (stock - items >= 0) {
                stock -= items;
                return true;
            }
            return false;
        }
    }

    public static void main(String[] args) {

        CountDownLatch latch = new CountDownLatch(20);
        List<Runnable> orderList = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            int item = random.nextInt(1, 11);
            Order order = new Order(i, item);
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Order " + order.id + " is proceed by " + Thread.currentThread().getName());
                    if (decrementStock(order.items)) {
                        counter.getAndAdd(order.items);

                    } else {
                        System.out.println("Order " + order.id + " has been canceled: Out of stock. We are sorry)");
                    }
                    latch.countDown();
                }
            };
            orderList.add(runnable);
        }

        for (Runnable runnable : orderList) {
            executorService.execute(runnable);
        }


        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Future<String> stringFuture = executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "All orders processed" + "\nTotal items : " + counter + "\nStock left: " + stock;
            }
        });
        executorService.shutdown();

        try {
            String report = stringFuture.get();
            System.out.println(report);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }


    }
}
