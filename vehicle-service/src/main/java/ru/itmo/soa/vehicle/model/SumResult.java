package ru.itmo.soa.vehicle.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "SumResult")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SumResult", propOrder = {"sum"})
public class SumResult {

    @XmlElement
    private Double sum;

    public SumResult() {
    }

    public SumResult(Double sum) {
        this.sum = sum;
    }

    public Double getSum() {
        return sum;
    }

    public void setSum(Double sum) {
        this.sum = sum;
    }
}
