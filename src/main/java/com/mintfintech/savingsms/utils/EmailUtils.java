package com.mintfintech.savingsms.utils;

import java.util.regex.Pattern;

/**
 * Created by jnwanya on
 * Wed, 16 Feb, 2022
 */
public class EmailUtils {

    private static Pattern emailPattern = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

    public static boolean isValid(String email) {
        return emailPattern.matcher(email).matches();
    }

    public static void main(String[] args) {
        String email = "nwanya.justin@gmail.com";
        String sub = email.substring(0, email.indexOf("@"));
        System.out.println(sub);
        System.out.println(isValid(email));

        String name = "Justin Nwanya";
        String[] names = name.split(" ", 2);
        System.out.println(names.length);
        name = "Justin";
        names = name.split(" ", 2);
        System.out.println(names.length);
    }
}
