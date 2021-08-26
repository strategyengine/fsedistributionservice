package com.strategyengine.xrpl.fsedistributionservice.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SwaggerController {

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String swagger(HttpServletResponse response) throws IOException {
		
		response.sendRedirect("/swagger-ui.html");
		return null;
	}

}