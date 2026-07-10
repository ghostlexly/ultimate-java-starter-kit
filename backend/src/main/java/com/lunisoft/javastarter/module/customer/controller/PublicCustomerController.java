package com.lunisoft.javastarter.module.customer.controller;

import com.lunisoft.javastarter.core.security.PublicEndpoint;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PublicEndpoint
@RestController
@RequestMapping("/api/customer")
public class PublicCustomerController {}
