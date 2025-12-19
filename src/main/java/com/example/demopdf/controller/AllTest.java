package com.example.demopdf.controller;

import java.util.concurrent.locks.ReentrantLock;

public class AllTest {

    private static int count = 0;

    private static final ReentrantLock lock = new ReentrantLock();

    private void getInsCount(){
        try {
            lock.lock();
            count++;
        }finally {
            lock.unlock();
        }
    }

    private int getAllCount(){
        try {
            lock.lock();
            return count;
        }finally {
            lock.unlock();
        }
    }


    public static void main(String[] args) throws InterruptedException {

//        AllTest allTest = new AllTest();
//
//        Runnable task = () -> {
//            for (int i = 0;i <1000;i++){
//                allTest.getInsCount();
//            }
//        };
//
//        Thread thread1 = new Thread(task);
//        Thread thread2 = new Thread(task);
//
//        thread1.start();
//        thread2.start();
//
//        thread1.join();
//        thread2.join();
//
//        System.out.println(allTest.getAllCount());

    }

}
