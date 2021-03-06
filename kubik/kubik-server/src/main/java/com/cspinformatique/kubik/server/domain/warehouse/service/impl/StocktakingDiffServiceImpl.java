package com.cspinformatique.kubik.server.domain.warehouse.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.cspinformatique.kubik.server.domain.warehouse.repository.StocktakingDiffRepository;
import com.cspinformatique.kubik.server.domain.warehouse.service.StocktakingDiffService;
import com.cspinformatique.kubik.server.domain.warehouse.service.StocktakingProductService;
import com.cspinformatique.kubik.server.model.warehouse.Stocktaking.Status;
import com.cspinformatique.kubik.server.model.product.Product;
import com.cspinformatique.kubik.server.model.warehouse.StocktakingDiff;
import com.cspinformatique.kubik.server.model.warehouse.StocktakingProduct;

@Service
public class StocktakingDiffServiceImpl implements StocktakingDiffService {
	@Resource
	StocktakingDiffRepository stocktakingDiffRepository;

	@Resource
	StocktakingProductService stocktakingProductService;

	@Override
	public void delete(StocktakingDiff stocktakingDiff) {
		stocktakingDiffRepository.delete(stocktakingDiff);
	}

	private StocktakingDiff findOne(long id) {
		return stocktakingDiffRepository.findOne(id);
	}

	@Override
	public List<StocktakingDiff> findByProduct(Product product) {
		return stocktakingDiffRepository.findByProduct(product);
	}

	@Override
	public StocktakingDiff updateCountedQuantity(long id, double countedQuantity) {
		StocktakingDiff stocktakingDiff = findOne(id);

		stocktakingDiff.setCountedQuantity(countedQuantity);
		stocktakingDiff
				.setAdjustmentQuantity(
						countedQuantity
								- (stocktakingDiff.getProduct().getProductInventory() != null
										? stocktakingDiff.getProduct().getProductInventory().getQuantityOnHand()
												- stocktakingDiff.getProduct().getProductInventory().getQuantityOnHold()
										: 0));

		// Update the counted quantity for the existing stocktaking product.
		for (StocktakingProduct stocktakingProduct : stocktakingProductService
				.findByProductAndStocktakingStatus(stocktakingDiff.getProduct(), Status.IN_PROGRESS)) {
			stocktakingProduct.setQuantity(countedQuantity);

			stocktakingProductService.save(stocktakingProduct);
		}

		return save(stocktakingDiff);
	}

	@Override
	public StocktakingDiff updateValidated(long id, boolean validated) {
		StocktakingDiff stocktakingDiff = findOne(id);

		stocktakingDiff.setValidated(validated);

		return save(stocktakingDiff);
	}

	private StocktakingDiff save(StocktakingDiff stocktakingDiff) {
		return stocktakingDiffRepository.save(stocktakingDiff);
	}
}
