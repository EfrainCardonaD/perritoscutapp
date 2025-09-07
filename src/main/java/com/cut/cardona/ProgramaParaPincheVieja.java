package com.cut.cardona;

import java.text.DateFormat;
import java.util.Scanner;

public class ProgramaParaPincheVieja {
    public static void main(String[] args) {

        for (int i = 0; i < 100; i++) {
            if (i % 3 == 0 && i % 5 == 0) {
                System.out.println(i + "fizzBuzz");
            }else if (i % 3 == 0) {
                System.out.println(i + "fizz");
            }else if (i % 5 == 0) {
                System.out.println(i + "buzz");
            }

        }

    }
}
