package org.apache.axis2.jaxws.rpclit.enumtype.sei;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.xml.ws.Holder;
import org.test.rpclit.schema.ElementString;


/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.0_01-b15-fcs
 * Generated source version: 2.0
 * 
 */
@WebService(name = "PortType", targetNamespace = "http://rpclit.test.org")
@SOAPBinding(style = Style.RPC)
public interface PortType {


    /**
     * 
     * @param pString
     */
    @WebMethod
    public void echoString(
        @WebParam(name = "pString", mode = Mode.INOUT, partName = "pString")
        Holder<ElementString> pString);

}
