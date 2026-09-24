package ru.itmo.soa.shop.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "VehiclePage")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VehiclePage", propOrder = {"pageNumber", "pageSize", "totalElements", "totalPages", "items"})
public class VehiclePage {

    @XmlElement
    private Integer pageNumber;

    @XmlElement
    private Integer pageSize;

    @XmlElement
    private Long totalElements;

    @XmlElement
    private Integer totalPages;

    @XmlElementWrapper(name = "items")
    @XmlElement(name = "vehicle")
    private List<Vehicle> items = new ArrayList<>();

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public List<Vehicle> getItems() {
        return items;
    }

    public void setItems(List<Vehicle> items) {
        this.items = items;
    }
}
