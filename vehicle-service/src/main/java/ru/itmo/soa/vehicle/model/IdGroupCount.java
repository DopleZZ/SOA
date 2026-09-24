package ru.itmo.soa.vehicle.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "IdGroupCount")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdGroupCount", propOrder = {"entries"})
public class IdGroupCount {

    @XmlElementWrapper(name = "entries")
    @XmlElement(name = "entry")
    private List<IdGroupCountEntry> entries = new ArrayList<>();

    public List<IdGroupCountEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<IdGroupCountEntry> entries) {
        this.entries = entries;
    }
}
