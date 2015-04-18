package com.cspinformatique.kubik.domain.purchase.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cspinformatique.kubik.domain.purchase.model.Rma;

public interface RmaService {
	Page<Rma> findAll(Pageable pageable);
	
	Rma findOne(int id);
	
	Rma save(Rma rma);
}