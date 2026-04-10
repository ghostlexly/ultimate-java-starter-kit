package com.lunisoft.javastarter.module.auth.event;

public record LoginCodeRequestedEvent(String email, String code) {}
