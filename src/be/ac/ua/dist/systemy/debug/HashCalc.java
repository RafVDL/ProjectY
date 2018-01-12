package be.ac.ua.dist.systemy.debug;

import java.util.Scanner;

public class HashCalc {
    public static void main(String[] args) {
        String string;
        do {
            Scanner sc = new Scanner(System.in);
            System.out.print("Enter string to calculate hash: ");
            string = sc.nextLine();
            System.out.println("String " + string + " hashes to: " + calculateHash(string) + "\n");
        } while (!string.equals("exit"));
    }

    private static int calculateHash(String string) {
        return Math.abs(string.hashCode() % 32768);
    }
}
