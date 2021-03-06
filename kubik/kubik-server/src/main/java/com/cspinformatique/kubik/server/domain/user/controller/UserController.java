package com.cspinformatique.kubik.server.domain.user.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cspinformatique.kubik.server.domain.user.service.UserService;
import com.cspinformatique.kubik.server.model.user.User;

@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	private UserService userService;

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody User findConnectedUser(Principal principal) {
		return this.userService.findByUsername(principal.getName());
	}

	@RequestMapping(value = "/new", method = RequestMethod.POST)
	public @ResponseBody User createUser(@RequestBody User user, @RequestParam String password) {
		user.setPassword(password);

		return this.userService.save(user);
	}
}
