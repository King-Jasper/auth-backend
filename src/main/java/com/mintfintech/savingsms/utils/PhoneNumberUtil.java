package com.mintfintech.savingsms.utils;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class PhoneNumberUtil {

    private static com.google.i18n.phonenumbers.PhoneNumberUtil phoneNumberUtil = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance();

    public static String toInternationalFormat(String phoneNumber) {
        return toInternationalPhoneNumber(phoneNumber, "NG");
    }

    private static String toInternationalPhoneNumber(String phoneNumber, String regionCode) {
        phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, regionCode);
            return phoneNumberUtil.format(number, com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return phoneNumber;
    }

    public static String toNationalFormat(String phoneNumber) {
        return toNationalFormat(phoneNumber, "NG");
    }

    private static String toNationalFormat(String phoneNumber,  String regionCode) {
        if(!phoneNumber.startsWith("+")) {
            return phoneNumber;
        }
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, regionCode);
            return phoneNumberUtil.format(number, com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return phoneNumber;
    }


   /* public static String maskPhoneNumber(String phoneNumber) {
        return StringUtils.maskString(phoneNumber, 4, 11, '*');
    }*/
}
