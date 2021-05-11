package com.mintfintech.savingsms.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Created by jnwanya on
 * Sun, 09 May, 2021
 */
//@Service
public class AsyncTest {

    //@Async
    public void testMethod(LocalDateTime time) {
        try {
            String threadName = Thread.currentThread().getName();
            int delay = new Random().nextInt(5) * 1000;
            System.out.println("Thread "+threadName+" delaying "+time+" for - "+(delay / 1000)+" secs");
            Thread.sleep(delay);
            System.out.println("completed - "+threadName);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
