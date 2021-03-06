package com.cspinformatique.kubik.server.domain.product.exception;

import com.cspinformatique.kubik.server.exception.ValidationException;

public class CategoryNameAlreadyUsedException extends ValidationException {
	private static final long serialVersionUID = 6671289296545406356L;

	public CategoryNameAlreadyUsedException(String message) {
		super(message);
	}
}
