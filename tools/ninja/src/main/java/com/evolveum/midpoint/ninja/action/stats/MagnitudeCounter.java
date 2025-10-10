package com.evolveum.midpoint.ninja.action.stats;

public class MagnitudeCounter {

    private int count;

    MagnitudeCounter() {
        this.count = 0;
    }

    public void increment() {
        this.count++;
    }

    public int toOrderOfMagnitude() {
        if (count <= 0) {
            throw new IllegalStateException("Count of object has to be positive number.");
        }
        if (count < 10) {
            return 99;
        }

        int remaining = count;
        int numberOfDigits = 0;
        while(remaining > 0) {
            remaining /= 10;
            numberOfDigits++;
        }

        return (int) Math.pow(10 , numberOfDigits) - 1;
    }
}
