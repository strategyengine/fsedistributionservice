package com.strategyengine.xrpl.fsedistributionservice.service;

public interface EmailService {

	void sendEmail(String to, String subject, String text);

}
