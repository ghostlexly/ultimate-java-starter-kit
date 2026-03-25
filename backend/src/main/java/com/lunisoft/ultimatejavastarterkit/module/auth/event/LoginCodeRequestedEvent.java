package com.lunisoft.ultimatejavastarterkit.module.auth.event;

public record LoginCodeRequestedEvent(String email, String code) {}
