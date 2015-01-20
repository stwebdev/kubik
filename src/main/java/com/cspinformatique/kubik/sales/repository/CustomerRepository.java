package com.cspinformatique.kubik.sales.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.cspinformatique.kubik.sales.model.Customer;

public interface CustomerRepository extends
		PagingAndSortingRepository<Customer, Integer> {

	@Query("SELECT customer FROM Customer customer WHERE CONCAT(firstName, ' ', lastName) like :query OR email like :query")
	public Page<Customer> search(@Param("query") String query, Pageable pageable);
}
