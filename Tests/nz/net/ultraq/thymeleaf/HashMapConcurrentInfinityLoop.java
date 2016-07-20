/*
 * Copyright 2016 zhanhb.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.net.ultraq.thymeleaf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * java.util.HashMap is optimized in JDK 8, this issue won't happen.
 *
 * @see
 * https://github.com/ultraq/thymeleaf-layout-dialect/commit/8d04625cfc9b4e50e36490ddefdeb0f4d6c6dd19#commitcomment-18282381
 * @see
 * http://stackoverflow.com/questions/13695832/explain-the-timing-causing-hashmap-put-to-execute-an-infinite-loop
 * @author zhanhb
 */
public class HashMapConcurrentInfinityLoop {

    public static void main(String[] args) throws IOException, InterruptedException {
        ArrayList<String> list = new ArrayList<>();
        for (String a : new String[]{"layout", "legacy", "theme"}) {
            for (String b : new String[]{"main", "admin", "dialect", "admin", "blue", "yellow", "red", "pink", "grey", "green", "test"}) {
                list.add("a very long string to make the issue occur faster" + a + "/" + b);
            }
        }
        String[] tags = list.toArray(new String[list.size()]);
        Random rr = new Random();
        final ThreadGroup group = new ThreadGroup("sample");
        ExecutorService es = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(group, r);
            }
        });
        for (int k = 0;; k++) {
            final HashSet<String> set = new HashSet<>();

            final CountDownLatch countDownLatch = new CountDownLatch(40);
            for (int i = (int) countDownLatch.getCount(); i > 0; i--) {
                final String c = tags[rr.nextInt(tags.length)];
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (!set.contains(c)) {
                            set.add(c);
                        }
                        countDownLatch.countDown();
                    }
                });
            }
            if (!countDownLatch.await(20, TimeUnit.SECONDS)) {
                System.out.println("Run the test " + k + " times and the issue reappeared");
                break;
            }
        }
        es.shutdownNow();
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] value = entry.getValue();
            if (thread.getThreadGroup() == group) {
                Exception ex = new Exception();
                ex.setStackTrace(value);
                group.uncaughtException(thread, ex);
            }
        }
        if (!es.awaitTermination(10, TimeUnit.SECONDS)) {
            System.exit(1);
        }
    }

}
