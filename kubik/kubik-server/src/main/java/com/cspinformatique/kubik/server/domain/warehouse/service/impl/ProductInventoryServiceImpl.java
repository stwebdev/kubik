package com.cspinformatique.kubik.server.domain.warehouse.service.impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.cspinformatique.kubik.server.domain.kos.service.KosNotificationService;
import com.cspinformatique.kubik.server.domain.product.service.ProductService;
import com.cspinformatique.kubik.server.domain.purchase.service.ReceptionDetailService;
import com.cspinformatique.kubik.server.domain.purchase.service.ReceptionService;
import com.cspinformatique.kubik.server.domain.purchase.service.RmaService;
import com.cspinformatique.kubik.server.domain.sales.service.CustomerCreditService;
import com.cspinformatique.kubik.server.domain.sales.service.InvoiceService;
import com.cspinformatique.kubik.server.domain.warehouse.model.InventoryExtract;
import com.cspinformatique.kubik.server.domain.warehouse.model.InventoryExtractLine;
import com.cspinformatique.kubik.server.domain.warehouse.repository.ProductInventoryRepository;
import com.cspinformatique.kubik.server.domain.warehouse.service.InventoryCountService;
import com.cspinformatique.kubik.server.domain.warehouse.service.ProductInventoryService;
import com.cspinformatique.kubik.server.model.kos.KosNotification;
import com.cspinformatique.kubik.server.model.product.Product;
import com.cspinformatique.kubik.server.model.purchase.Reception;
import com.cspinformatique.kubik.server.model.purchase.ReceptionDetail;
import com.cspinformatique.kubik.server.model.warehouse.ProductInventory;

@Service
public class ProductInventoryServiceImpl implements ProductInventoryService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProductInventoryServiceImpl.class);

	@Resource
	private ProductInventoryRepository productInventoyRepository;

	@Resource
	private CustomerCreditService customerCreditService;

	@Resource
	private InventoryCountService inventoryCountService;

	@Resource
	private InvoiceService invoiceService;

	@Resource
	private KosNotificationService kosNotificationService;

	@Resource
	private ProductService productService;

	@Resource
	private ReceptionService receptionService;

	@Resource
	private ReceptionDetailService receptionDetailService;

	@Resource
	private RmaService rmaService;

	@Override
	public double calculateProductOnHandUntil(int productId, Date until) {
		double quantityReceived = receptionService.findProductQuantityReceivedUntil(productId, until);
		double quantityCustomerReturned = customerCreditService.findProductQuantityReturnedByCustomerUntil(productId,
				until);
		double quantityReturnedToSupplier = rmaService.findProductQuantityReturnedToSupplierUntil(productId, until);
		double quantitySold = invoiceService.findProductQuantitySoldUntil(productId, until);
		double quantityCounted = inventoryCountService.findProductQuantityCountedUntil(productId, until);
		double quantityOnHold = invoiceService.findProductQuantityOnHoldUntil(productId, until);

		return quantityReceived + quantityCustomerReturned - quantitySold - quantityReturnedToSupplier - quantityOnHold
				+ quantityCounted;
	}

	@Override
	public void deleteByProduct(Product product) {
		productInventoyRepository.delete(findByProduct(product));
	}

	@Override
	public List<ProductInventory> findAll() {
		return productInventoyRepository.findAll();
	}

	@Override
	public Page<ProductInventory> findAll(Pageable pageable) {
		return productInventoyRepository.findAll(pageable);
	}

	@Override
	public ProductInventory findByProduct(Product product) {
		Optional<ProductInventory> optional = productInventoyRepository.findByProduct(product);

		if (optional.isPresent())
			return optional.get();
		else
			return new ProductInventory(null, product, 0d, 0d);
	}

	@Override
	public List<Integer> findProductIdWithInventory() {
		return productInventoyRepository.findProductIdWithInventory();
	}

	@Override
	public void updateInventory(Product product) {
		updateInventory(findByProduct(product));
	}

	@Override
	public void updateInventory(ProductInventory productInventory) {
		int productId = productInventory.getProduct().getId();

		double oldQuantityOnHand = productInventory.getQuantityOnHand();
		double oldQuantityOnHold = productInventory.getQuantityOnHold();
		double quantityReceived = receptionService.findProductQuantityReceived(productId);
		double quantityCustomerReturned = customerCreditService.findProductQuantityReturnedByCustomer(productId);
		double quantityReturnedToSupplier = rmaService.findProductQuantityReturnedToSupplier(productId);
		double quantitySold = invoiceService.findProductQuantitySold(productId);
		double quantityCounted = inventoryCountService.findProductQuantityCounted(productId);
		double quantityOnHold = invoiceService.findProductQuantityOnHold(productId);

		double quantityOnHand = quantityReceived + quantityCustomerReturned - quantitySold - quantityReturnedToSupplier
				- quantityOnHold + quantityCounted;

		productInventory.setQuantityOnHand(quantityOnHand);
		productInventory.setQuantityOnHold(quantityOnHold);

		LOGGER.info("Updating inventory for product " + productId + ". On Hand : " + oldQuantityOnHand + " to "
				+ quantityOnHand + ". On Hold : " + oldQuantityOnHold + " to " + quantityOnHold + ".");

		productInventory = save(productInventory);

		kosNotificationService.createNewNotification(productInventory.getProduct().getId(),
				KosNotification.Type.PRODUCT_INVENTORY, KosNotification.Action.UPDATE);
	}

	@Override
	public void updateInventories() {
		Page<ProductInventory> page = null;
		Pageable pageable = new PageRequest(0, 100, Direction.ASC, "id");
		do {
			page = findAll(pageable);

			page.getContent().stream().forEach(productInventory -> updateInventory(productInventory));

			pageable = pageable.next();
		} while (page.hasNext());
	}

	@Override
	public InventoryExtract generateInventoryExtraction(String separator, DecimalFormat decimalFormat) {
		return generateInventoryExtraction(separator, decimalFormat, null);
	}

	@Override
	public InventoryExtract generateInventoryExtraction(String separator, DecimalFormat decimalFormat, Date until) {
		List<InventoryExtractLine> extract = new ArrayList<>();

		for (Integer productId : findProductIdWithInventory()) {
			Product product = productService.findOne(productId);

			double quantityOnHand = until == null ? findByProduct(product).getQuantityOnHand()
					: calculateProductOnHandUntil(product.getId(), until);

			Page<ReceptionDetail> receptionDetailPage = receptionDetailService.findByProductAndReceptionStatus(product,
					Reception.Status.CLOSED, new PageRequest(0, 1, Direction.DESC, "reception.dateReceived"));
			BigDecimal purchasePrice = new BigDecimal(0d);
			if (!receptionDetailPage.hasContent()) {
				LOGGER.warn("Product " + productId
						+ " has never been received in the system. Purchase will not be calculated.");
			} else {
				purchasePrice = new BigDecimal(receptionDetailPage.getContent().get(0).getUnitPriceTaxOut());
			}

			BigDecimal priceTaxLess = null;
			if (product.getPriceTaxOut1() != null) {
				priceTaxLess = new BigDecimal(product.getPriceTaxOut1());
			} else {
				BigDecimal taxAmount = new BigDecimal(product.getPriceTaxIn())
						.multiply(new BigDecimal(product.getTvaRate1()).divide(new BigDecimal(100)));
				priceTaxLess = new BigDecimal(product.getPriceTaxIn()).subtract(taxAmount);
			}

			extract.add(new InventoryExtractLine(product.getEan13(), product.getExtendedLabel(), quantityOnHand,
					purchasePrice, purchasePrice.multiply(new BigDecimal(quantityOnHand)), priceTaxLess,
					priceTaxLess.multiply(new BigDecimal(quantityOnHand))));
		}

		return new InventoryExtract(new Date(), separator, decimalFormat, extract);
	}

	@Override
	public ProductInventory save(ProductInventory productInventory) {
		return productInventoyRepository.save(productInventory);
	}

}
