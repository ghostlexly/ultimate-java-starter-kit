package com.lunisoft.javastarter.shared.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Parses and formats phone numbers to E.164 international format (e.g. +33781209072). Uses the
 * customer's country code to resolve local numbers (e.g. 0781209072 with "FR" → +33781209072).
 */
@Service
public class PhoneNumberService {

  private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

  /**
   * Formats a phone number to E.164 international format.
   *
   * @param rawPhone the raw phone input (e.g. "0781209072" or "+33781209072")
   * @param countryCode the ISO 3166-1 alpha-2 country code (e.g. "FR", "BE", "CH")
   * @return the phone number in E.164 format (e.g. "+33781209072")
   * @throws BusinessRuleException if the phone number is invalid
   */
  public String formatToE164(String rawPhone, String countryCode) {
    try {
      PhoneNumber parsed = phoneNumberUtil.parse(rawPhone, countryCode);

      if (!phoneNumberUtil.isValidNumber(parsed)) {
        throw new BusinessRuleException(
            "Invalid phone number.", "INVALID_PHONE_NUMBER", HttpStatus.BAD_REQUEST);
      }

      return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
    } catch (NumberParseException _) {
      throw new BusinessRuleException(
          "Invalid phone number format.", "INVALID_PHONE_NUMBER", HttpStatus.BAD_REQUEST);
    }
  }
}
