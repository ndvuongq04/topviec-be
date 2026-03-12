package com.topviec.topviec_be.service;

public interface TokenService {

    String generateVerifyEmailToken(String email);

    String verifyEmailToken(String token);
}