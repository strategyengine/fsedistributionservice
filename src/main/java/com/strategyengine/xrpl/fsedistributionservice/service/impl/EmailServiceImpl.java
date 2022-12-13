package com.strategyengine.xrpl.fsedistributionservice.service.impl;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.strategyengine.xrpl.fsedistributionservice.service.EmailService;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class EmailServiceImpl implements EmailService {

	@Autowired
	private JavaMailSender mailSender;

	@Value("${fsedistributionservice.email.address}")
	private String emailAddress;

	@Override
	public void sendEmail(String to, String subject, String htmlBody) {

		try {
	
			MimeMessage message = mailSender.createMimeMessage();

			message.setFrom(new InternetAddress(emailAddress));

			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

			message.setSubject(subject);

			message.setContent(htmlBody, "text/html");
			mailSender.send(message);

		} catch (Exception e) {
			log.error("Error sending email to " + to, e);
		}

	}
}
