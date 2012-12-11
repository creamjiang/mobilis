//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.11 at 09:47:07 AM MEZ 
//


package de.tudresden.inf.rn.mobilis.gwtemulationserver.server.script;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for structureType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="structureType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://mobilis.inf.tu-dresden.de/XMLEmulationScript}commandType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element ref="{http://mobilis.inf.tu-dresden.de/XMLEmulationScript}for"/>
 *           &lt;element name="block" type="{http://www.w3.org/2001/XMLSchema}anyType"/>
 *         &lt;/choice>
 *         &lt;element ref="{http://mobilis.inf.tu-dresden.de/XMLEmulationScript}command" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "structureType", propOrder = {
    "_for",
    "block",
    "command"
})
@XmlSeeAlso({
    ForType.class
})
public class StructureType
    extends CommandType
{

    @XmlElement(name = "for")
    protected ForType _for;
    protected Object block;
    @XmlElementRef(name = "command", namespace = "http://mobilis.inf.tu-dresden.de/XMLEmulationScript", type = JAXBElement.class)
    protected List<JAXBElement<? extends CommandType>> command;

    /**
     * Gets the value of the for property.
     * 
     * @return
     *     possible object is
     *     {@link ForType }
     *     
     */
    public ForType getFor() {
        return _for;
    }

    /**
     * Sets the value of the for property.
     * 
     * @param value
     *     allowed object is
     *     {@link ForType }
     *     
     */
    public void setFor(ForType value) {
        this._for = value;
    }

    /**
     * Gets the value of the block property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getBlock() {
        return block;
    }

    /**
     * Sets the value of the block property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setBlock(Object value) {
        this.block = value;
    }

    /**
     * Gets the value of the command property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the command property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCommand().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link CommandType }{@code >}
     * {@link JAXBElement }{@code <}{@link AbstractInstanceType }{@code >}
     * {@link JAXBElement }{@code <}{@link SetupCommandType }{@code >}
     * {@link JAXBElement }{@code <}{@link InstanceType }{@code >}
     * {@link JAXBElement }{@code <}{@link ControlCommandType }{@code >}
     * {@link JAXBElement }{@code <}{@link AtomicCommandType }{@code >}
     * {@link JAXBElement }{@code <}{@link WaitType }{@code >}
     * {@link JAXBElement }{@code <}{@link StructureType }{@code >}
     * {@link JAXBElement }{@code <}{@link ForType }{@code >}
     * {@link JAXBElement }{@code <}{@link StopType }{@code >}
     * {@link JAXBElement }{@code <}{@link SetupMethodsType }{@code >}
     * {@link JAXBElement }{@code <}{@link AppCommandType }{@code >}
     * {@link JAXBElement }{@code <}{@link StartType }{@code >}
     * {@link JAXBElement }{@code <}{@link InstanceGroupType }{@code >}
     * 
     * 
     */
    public List<JAXBElement<? extends CommandType>> getCommand() {
        if (command == null) {
            command = new ArrayList<JAXBElement<? extends CommandType>>();
        }
        return this.command;
    }

}
