package com.cspinformatique.kubik.model.product;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class Supplier {
	private Integer id;
	private String ean13;
	private String name;
	private String purchaseOrderEan13;
	private float discount;
	
	public Supplier(){
		
	}

	public Supplier(Integer id, String ean13, String name, String purchaseOrderEan13, float discount) {
		this.id = id;
		this.ean13 = ean13;
		this.name = name;
		this.purchaseOrderEan13 = purchaseOrderEan13;
		this.discount = discount;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getEan13() {
		return ean13;
	}

	public void setEan13(String ean13) {
		this.ean13 = ean13;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPurchaseOrderEan13() {
		return purchaseOrderEan13;
	}

	public void setPurchaseOrderEan13(String purchaseOrderEan13) {
		this.purchaseOrderEan13 = purchaseOrderEan13;
	}

	public float getDiscount() {
		return discount;
	}

	public void setDiscount(float discount) {
		this.discount = discount;
	}
}