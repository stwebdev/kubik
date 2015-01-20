package com.cspinformatique.kubik.warehouse.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import com.cspinformatique.kubik.product.model.Product;
import com.cspinformatique.kubik.warehouse.model.ProductInventory;

public interface ProductInventoryRepository extends
		PagingAndSortingRepository<ProductInventory, Integer> {
	public ProductInventory findByProduct(Product product);
}
