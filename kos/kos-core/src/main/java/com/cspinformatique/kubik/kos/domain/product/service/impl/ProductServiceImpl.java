package com.cspinformatique.kubik.kos.domain.product.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import com.cspinformatique.kubik.kos.domain.product.repository.ProductRepository;
import com.cspinformatique.kubik.kos.domain.product.service.CategoryService;
import com.cspinformatique.kubik.kos.domain.product.service.ProductService;
import com.cspinformatique.kubik.kos.model.product.Category;
import com.cspinformatique.kubik.kos.model.product.Product;
import com.cspinformatique.kubik.kos.rest.KubikTemplate;
import com.cspinformatique.kubik.server.model.kos.KosNotification;

@Service
public class ProductServiceImpl implements ProductService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceImpl.class);

	@Resource
	private KubikTemplate kubikTemplate;

	@Resource
	private CategoryService categoryService;

	@Resource
	private ProductRepository productRepository;

	@Override
	public List<Product> findByCategory(Category category) {
		return productRepository.findByCategory(category);
	}

	@Override
	public Product findOne(String ean13) {
		return productRepository.findOne(ean13);
	}

	private List<Category> calculateCategoriesScope(List<String> categoriesNames) {
		List<Category> categories = categoriesNames != null ? categoriesNames.stream().map(categoryName -> {
			Category category = categoryService.findByName(categoryName);
			if (category == null) {
				LOGGER.warn("Category " + categoryName + " does not exists. Will be filtered from product search.");
			}
			return category;
		}).filter(category -> category != null).collect(Collectors.toList()) : null;

		HashMap<Integer, Category> categoriesMap = new HashMap<>();

		categories.forEach(category -> {
			categoriesMap.put(category.getId(), category);
			addChildCategoriesToMap(categoriesMap, category);
		});

		return new ArrayList<Category>(categoriesMap.values());
	}

	private void addChildCategoriesToMap(Map<Integer, Category> categoriesMap, Category category) {
		category.getChildCategories().forEach(childCategory -> {
			categoriesMap.put(childCategory.getId(), childCategory);

			addChildCategoriesToMap(categoriesMap, childCategory);
		});
	}

	@Override
	public void processProductNotification(KosNotification kosNotification) {
		save(requestProductFromKubik(kosNotification.getKubikId()));
	}

	@Override
	public void processProductInventoryNotification(KosNotification kosNotification) {
		processProductNotification(kosNotification);
	}

	private Product requestProductFromKubik(int productId) {
		com.cspinformatique.kubik.server.model.product.Product kubikProduct;

		kubikProduct = kubikTemplate.exchange("/product/" + productId, HttpMethod.GET,
				com.cspinformatique.kubik.server.model.product.Product.class).getBody();

		// Checks if the product id already exists.
		Product product = productRepository.findOne(kubikProduct.getEan13());

		if (product == null) {
			product = new Product();

			if (kubikProduct.getCategory() != null) {
				// Loads the category
				int categoryId = kubikProduct.getCategory().getId();
				Category category = categoryService.findByKubikId(categoryId);

				if (category == null) {
					throw new RuntimeException("Category with Kubik id " + categoryId
							+ " doesn not exists. Product will not be synchronized.");
				}
				product.setCategory(category);
			}
		}

		product.setBrand(kubikProduct.getBrand());
		product.setEan13(kubikProduct.getEan13());
		product.setName(kubikProduct.getName());
		product.setPrice(kubikProduct.getPriceTaxIn());
		product.setWeight(kubikProduct.getWeight());

		if (kubikProduct.getProductInventory() != null)
			product.setAvailable(kubikProduct.getProductInventory().getQuantityOnHand() > 0);

		return product;

	}

	@Override
	public Product save(Product product) {
		return productRepository.save(product);
	}

	@Override
	public Page<Product> search(String title, String brand, List<String> categoriesIds, Date publishFrom,
			Date publishUntil, String manufacturer, Double priceFrom, Double priceTo, Boolean hideUnavailable,
			String query, Pageable pageable) {
		if (title != null)
			if (title.equals(""))
				title = null;
			else
				title = "%" + title + "%";
		if (brand != null)
			if (brand.equals(""))
				brand = null;
			else
				brand = "%" + brand + "%";
		if (manufacturer != null)
			if (manufacturer.equals(""))
				manufacturer = null;
			else
				manufacturer = "%" + manufacturer + "%";
		if (hideUnavailable == null)
			hideUnavailable = true;
		if (query != null)
			if (query.equals(""))
				query = null;
			else
				query = "%" + query + "%";

		if (categoriesIds == null)
			return productRepository.search(title, brand, publishFrom, publishUntil, manufacturer, priceFrom, priceTo,
					hideUnavailable, query, pageable);
		else
			return productRepository.search(title, brand, calculateCategoriesScope(categoriesIds), publishFrom,
					publishUntil, manufacturer, priceFrom, priceTo, hideUnavailable, query, pageable);
	}
}