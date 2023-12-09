package com.example.java_projects;
import java.util.Scanner;
public class Driver {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);//Create a scanner object whose object reference is stored in variable called scan
        System.out.println("Enter your buying price per share:");
        double buyingPrice = scan.nextDouble(); // variable to store the buying price, either decimal
        int day = 1;
        double closingPrice = 0.1;
        while (true) { // Using an endles loop until the condition is met
            System.out.println("Enter the closing price for day" + day + "(any negative value to leave:"); // entering negative value will kick you out of the program.
            closingPrice = scan.nextDouble();
            if (closingPrice < 0.0) break; //checking if closing price is negative.
            double earnings = closingPrice - buyingPrice;
            if (earnings > 0) {
                System.out.println("After day" + day + ", you earned" + earnings + "per share");
            } else if (earnings < 0.0) {
                System.out.println("After day" + day + ", you lost" + (-earnings) + "per share.");
            } else {
                System.out.println("After day" + day + ", you have no earnings");
            }
            day += 1; // always increment the varriable day after the while loop.
        }
        scan.close(); // close the scan object
    }
}