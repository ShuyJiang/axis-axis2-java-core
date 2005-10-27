/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis2.wsdl.builder.wsdl4j;

import com.ibm.wsdl.extensions.soap.SOAPConstants;
import org.apache.axis2.wsdl.builder.WSDLComponentFactory;
import org.apache.wsdl.*;
import org.apache.wsdl.extensions.DefaultExtensibilityElement;
import org.apache.wsdl.extensions.ExtensionConstants;
import org.apache.wsdl.extensions.ExtensionFactory;
import org.apache.wsdl.impl.WSDLProcessingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

public class WSDLPump {

    private static final String XMLSCHEMA_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema";
    private static final String XMLSCHEMA_NAMESPACE_PREFIX = "xs";
    private static final String XML_SCHEMA_LOCAL_NAME = "schema";
    private static final String XML_SCHEMA_SEQUENCE_LOCAL_NAME = "sequence";
    private static final String XML_SCHEMA_COMPLEX_TYPE_LOCAL_NAME = "complexType";
    private static final String XML_SCHEMA_ELEMENT_LOCAL_NAME = "element";
    private static final String XSD_SCHEMA_QUALIFIED_NAME =XMLSCHEMA_NAMESPACE_PREFIX + ":"+XML_SCHEMA_LOCAL_NAME;
    private static final String XSD_SEQUENCE_QUALIFIED_NAME =XMLSCHEMA_NAMESPACE_PREFIX + ":"+XML_SCHEMA_SEQUENCE_LOCAL_NAME;
    private static final String XSD_COMPLEXTYPE_QUALIFIED_NAME = XMLSCHEMA_NAMESPACE_PREFIX + ":"+XML_SCHEMA_COMPLEX_TYPE_LOCAL_NAME;
    private static final String XSD_ELEMENT_QUALIFIED_NAME = XMLSCHEMA_NAMESPACE_PREFIX + ":"+XML_SCHEMA_ELEMENT_LOCAL_NAME;

    private static final String XSD_NAME = "name";
    private static final String XSD_TARGETNAMESPACE = "targetNamespace";
    private static final String XMLNS_AXIS2WRAPPED = "xmlns:axis2wrapped";
    private static final String AXIS2WRAPPED = "axis2wrapped";
    private static final String XSD_TYPE = "type";
    private static final String BOUND_INTERFACE_NAME = "BoundInterface";


    private static int nsCount=0;

    private WSDLDescription womDefinition;

    private Definition wsdl4jParsedDefinition;

    private WSDLComponentFactory wsdlComponentFactory;

    private Map declaredNameSpaces=null;

    private List resolvedMultipartMessageList = new LinkedList();

    public WSDLPump(WSDLDescription womDefinition,
                    Definition wsdl4jParsedDefinition) {
        this(womDefinition, wsdl4jParsedDefinition, womDefinition);
    }

    public WSDLPump(WSDLDescription womDefinition,
                    Definition wsdl4jParsedDefinition,
                    WSDLComponentFactory wsdlComponentFactory) {
        this.womDefinition = womDefinition;
        this.wsdl4jParsedDefinition = wsdl4jParsedDefinition;
        this.wsdlComponentFactory = wsdlComponentFactory;
    }

    public void pump() {
        if (null != this.wsdl4jParsedDefinition && null != this.womDefinition) {
            this.populateDefinition(this.womDefinition,
                    this.wsdl4jParsedDefinition);
        } else {
            throw new WSDLProcessingException("Properties not set properly");
        }

    }

    private void populateDefinition(WSDLDescription wsdlDefinition,
                                    Definition wsdl4JDefinition) {
        //Go through the WSDL4J Definition and pump it to the WOM
        wsdlDefinition.setWSDL1DefinitionName(wsdl4JDefinition.getQName());
        wsdlDefinition
                .setTargetNameSpace(wsdl4JDefinition.getTargetNamespace());
        wsdlDefinition.setNamespaces(wsdl4JDefinition.getNamespaces());
        this.copyExtensibleElements(
                wsdl4JDefinition.getExtensibilityElements(), wsdlDefinition, null);

        //get the namespace map
        this.declaredNameSpaces = wsdl4JDefinition.getNamespaces();

        /////////////////////////////////////////////////////////////////////
        // Order of the following items shouldn't be changed unless you //
        // really know what you are doing. Reason being the components that //
        // are copied(pumped) towards the end depend on the components that //
        // has already being pumped. Following Lists some of the //
        // dependencies. //
        //1) The Binding refers to the Interface //
        //2) The Endpoint refers to the Bindings //
        // .... //
        //																   	//
        //////////////////////////////////////////////////////////////////////

        //////////////////(1)First Copy the Types/////////////////////////////
        //Types may get changed inside the Operation pumping.

        Types wsdl4jTypes = wsdl4JDefinition.getTypes();
        WSDLTypes wsdlTypes = null;
        if (null != wsdl4jTypes) {
            wsdlTypes = this.wsdlComponentFactory.createTypes();
            this.copyExtensibleElements(wsdl4jTypes.getExtensibilityElements(),
                    wsdlTypes, null);
            this.womDefinition.setTypes(wsdlTypes);
        }


        // There can be types that are imported. Check the imports and
        // These schemas are needed for code generation

        Map wsdlImports = wsdl4JDefinition.getImports();

        if (null != wsdlImports && !wsdlImports.isEmpty()){
            Collection importsCollection = wsdlImports.values();
            for (Iterator iterator = importsCollection.iterator(); iterator.hasNext();) {
                Vector values = (Vector)iterator.next();
                for (int i = 0; i < values.size(); i++) {
                    Import wsdlImport = (Import)values.elementAt(i);

                    if (wsdlImport.getDefinition()!=null){
                        Definition importedDef = wsdlImport.getDefinition();

                        if (wsdlTypes==null){
                            wsdlTypes = this.wsdlComponentFactory.createTypes();
                        }
                        //add the imported types
                        this.copyExtensibleElements(importedDef.getTypes().
                                getExtensibilityElements(),
                                wsdlTypes, null);
                        this.womDefinition.setTypes(wsdlTypes);
                    }
                }

            }
        }



        ///////////////////(2)Copy the Interfaces///////////////////////////
        //copy the Interfaces: Get the PortTypes from WSDL4J parse OM and
        // copy it to the
        //WOM's WSDLInterface Components

        Iterator portTypeIterator = wsdl4JDefinition.getPortTypes().values()
                .iterator();
        WSDLInterface wsdlInterface;
        PortType portType;
        while (portTypeIterator.hasNext()) {
            wsdlInterface = this.wsdlComponentFactory.createInterface();
            portType = (PortType) portTypeIterator.next();
            //////////////////////////
            this.populateInterfaces(wsdlInterface, portType,wsdl4JDefinition);
            /////////////////////////
            this.copyExtensibilityAttribute(portType.getExtensionAttributes(),
                    wsdlInterface);
            wsdlDefinition.addInterface(wsdlInterface);
        }

        //////////////////(3)Copy the Bindings///////////////////////
        //pump the Bindings: Get the Bindings map from WSDL4J and create a new
        // map of WSDLBinding elements. At this point we need to do some extra work since there
        //can be header parameters

        Iterator bindingIterator = wsdl4JDefinition.getBindings().values()
                .iterator();
        WSDLBinding wsdlBinding;
        Binding wsdl4jBinding;
        while (bindingIterator.hasNext()) {
            wsdlBinding = this.wsdlComponentFactory.createBinding();
            wsdl4jBinding = (Binding) bindingIterator.next();
            this.populateBindings(wsdlBinding, wsdl4jBinding, wsdl4JDefinition);
            this.copyExtensibleElements(
                    wsdl4jBinding.getExtensibilityElements(),
                    wsdlBinding, null);
            wsdlDefinition.addBinding(wsdlBinding);

        }

        ///////////////////(4)Copy the Services///////////////////////////////

        Iterator serviceIterator = wsdl4JDefinition.getServices().values()
                .iterator();
        WSDLService wsdlService;
        Service wsdl4jService;
        while (serviceIterator.hasNext()) {
            wsdlService = this.wsdlComponentFactory.createService();
            wsdl4jService = (Service) serviceIterator.next();
            this.populateServices(wsdlService, wsdl4jService);
            this.copyExtensibleElements(
                    wsdl4jService.getExtensibilityElements(),
                    wsdlService, null);
            wsdlDefinition.addService(wsdlService);
        }

    }

    //////////////////////////////////////////////////////////////////////////////
    ////////////////////////// Top level Components Copying
    // ////////////////////

    /**
     * Simply Copy information.
     *
     * @param wsdlInterface
     * @param wsdl4jPortType
     */
    // FIXME Evaluate a way of injecting features and priperties with a general
    // formatted input
    private void populateInterfaces(WSDLInterface wsdlInterface,
                                    PortType wsdl4jPortType,Definition wsdl4jDefinition) {

        //Copy the Attribute information items
        //Copied with the Same QName so it will require no Query in Binding
        //Coping.
        wsdlInterface.setName(wsdl4jPortType.getQName());
        Iterator wsdl4JOperationsIterator =
                wsdl4jPortType.getOperations().iterator();
        WSDLOperation wsdloperation;
        Operation wsdl4jOperation;

        while (wsdl4JOperationsIterator.hasNext()) {
            wsdloperation = this.wsdlComponentFactory.createOperation();
            wsdl4jOperation = (Operation) wsdl4JOperationsIterator.next();

            this.populateOperations(wsdloperation,
                    wsdl4jOperation,
                    wsdl4jPortType.getQName().getNamespaceURI());

            this.copyExtensibleElements(
                    wsdl4jOperation.getExtensibilityElements(), wsdloperation, null);

            wsdlInterface.setOperation(wsdloperation);
        }
    }

    /**
     *

     * @return
     */
    private boolean findWrapppable(Message message) {

// ********************************************************************************************
// Note
// We will not use the binding to set the wrappable/unwrappable state. instead we'll look at the
// Messages for the following features
// 1. Messages with multiple parts -> We have no choice but to wrap
// 2. Messages with at least one part having a type attribute -> Again we have no choice but to
// wrap

// ********************************************************************************************
        Map partsMap = message.getParts();
        Iterator parts=partsMap.values().iterator();
        boolean wrappable= partsMap.size()>1;
        Part part;
        while (!wrappable && parts.hasNext()) {
            part = (Part) parts.next();
            wrappable = wrappable || (part.getTypeName()!=null);
        }


        return wrappable;
    }

    /**
     * Pre Condition: The Interface Components must be copied by now.
     */
    private void populateBindings(WSDLBinding wsdlBinding,
                                  Binding wsdl4JBinding, Definition wsdl4jDefinition) {
        //Copy attributes
        wsdlBinding.setName(wsdl4JBinding.getQName());
        QName interfaceName = wsdl4JBinding.getPortType().getQName();
        WSDLInterface wsdlInterface =
                this.womDefinition.getInterface(interfaceName);

        //FIXME Do We need this eventually???
        if (null == wsdlInterface)
            throw new WSDLProcessingException("Interface/PortType not found for the Binding :"
                    + wsdlBinding.getName());
        wsdlBinding.setBoundInterface(wsdlInterface);
        Iterator bindingoperationsIterator =
                wsdl4JBinding.getBindingOperations().iterator();
        WSDLBindingOperation wsdlBindingOperation;
        BindingOperation wsdl4jBindingOperation;
        while (bindingoperationsIterator.hasNext()) {
            wsdlBindingOperation =
                    this.wsdlComponentFactory.createWSDLBindingOperation();
            wsdl4jBindingOperation =
                    (BindingOperation) bindingoperationsIterator.next();
            this.populateBindingOperation(wsdlBindingOperation,
                    wsdl4jBindingOperation,
                    wsdl4JBinding.getQName().getNamespaceURI(), wsdl4jDefinition);
            wsdlBindingOperation.setOperation(
                    wsdlInterface.getOperation(
                            wsdl4jBindingOperation.getOperation().getName()));
            this.copyExtensibleElements(
                    wsdl4jBindingOperation.getExtensibilityElements(),
                    wsdlBindingOperation, wsdl4jDefinition);
            wsdlBinding.addBindingOperation(wsdlBindingOperation);
        }

    }

    public void populateServices(WSDLService wsdlService,
                                 Service wsdl4jService) {
        wsdlService.setName(wsdl4jService.getQName());
        Iterator wsdl4jportsIterator =
                wsdl4jService.getPorts().values().iterator();
        wsdlService.setServiceInterface(this.getBoundInterface(wsdlService));
        WSDLEndpoint wsdlEndpoint;
        Port wsdl4jPort;
        while (wsdl4jportsIterator.hasNext()) {
            wsdlEndpoint = this.wsdlComponentFactory.createEndpoint();
            wsdl4jPort = (Port) wsdl4jportsIterator.next();
            this.populatePorts(wsdlEndpoint,
                    wsdl4jPort,
                    wsdl4jService.getQName().getNamespaceURI());
            this.copyExtensibleElements(wsdl4jPort.getExtensibilityElements(),
                    wsdlEndpoint, null);
            wsdlService.setEndpoint(wsdlEndpoint);
        }

    }

    private void pushSchemaElement(Schema originalSchema,Stack stack){
        if (originalSchema==null){
            return;
        }
        stack.push(originalSchema.getElement());
        Map map = originalSchema.getImports();
        Collection values;
        if (map!=null && map.size()>0){
            values = map.values();
            for (Iterator iterator = values.iterator(); iterator.hasNext();) {
                //recursively add the schema's
                Vector v = (Vector)iterator.next();
                for (int i = 0; i < v.size(); i++) {
                    pushSchemaElement(((SchemaImport)v.get(i)).getReferencedSchema(),stack);
                }

            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    //////////////////////////// Internal Component Copying ///////////////////
    public void populateOperations(WSDLOperation wsdlOperation,
                                   Operation wsdl4jOperation,
                                   String nameSpaceOfTheOperation
    ) {
        //Copy Name Attribute
        wsdlOperation.setName(new QName(nameSpaceOfTheOperation,
                wsdl4jOperation.getName()));

        // This code make no attempt to make use of the special xs:Token
        // defined in the WSDL 2.0. eg like #any, #none
        // Create the Input Message and add
        Input wsdl4jInputMessage = wsdl4jOperation.getInput();
        QName wrappedInputName = wsdlOperation.getName();
        QName wrappedOutputName = new QName(
                               wrappedInputName.getNamespaceURI(),
                               wrappedInputName.getLocalPart()+ "Response",
                               wrappedInputName.getPrefix());

        if (null != wsdl4jInputMessage) {
            MessageReference wsdlInputMessage = this.wsdlComponentFactory
                    .createMessageReference();
            wsdlInputMessage.setDirection(
                    WSDLConstants.WSDL_MESSAGE_DIRECTION_IN);
            wsdlInputMessage.setMessageLabel(
                    WSDLConstants.MESSAGE_LABEL_IN_VALUE);

            Message message = wsdl4jInputMessage.getMessage();
            if (null != message) {
                //wrapping has to be done on per message basis

                wsdlInputMessage.setElement(
                        this.generateReferenceQname(wrappedInputName
                                ,message,findWrapppable(message)));
                this.copyExtensibleElements(
                        (message).getExtensibilityElements(),
                        wsdlInputMessage, null);
            }
            this.copyExtensibilityAttribute(
                    wsdl4jInputMessage.getExtensionAttributes(),
                    wsdlInputMessage);
            wsdlOperation.setInputMessage(wsdlInputMessage);
        }

        //Create an output message and add
        Output wsdl4jOutputMessage = wsdl4jOperation.getOutput();
        if (null != wsdl4jOutputMessage) {
            MessageReference wsdlOutputMessage =
                    this.wsdlComponentFactory.createMessageReference();
            wsdlOutputMessage.setDirection(
                    WSDLConstants.WSDL_MESSAGE_DIRECTION_OUT);
            wsdlOutputMessage.setMessageLabel(
                    WSDLConstants.MESSAGE_LABEL_OUT_VALUE);

            Message outputMessage = wsdl4jOutputMessage.getMessage();
            if (null != outputMessage) {
                wsdlOutputMessage.setElement(
                        this.generateReferenceQname(wrappedOutputName,outputMessage,findWrapppable(outputMessage)));
                this.copyExtensibleElements(
                        (outputMessage).getExtensibilityElements(),
                        wsdlOutputMessage, null);
            }
            this.copyExtensibilityAttribute(
                    wsdl4jOutputMessage.getExtensionAttributes(),
                    wsdlOutputMessage);
            wsdlOperation.setOutputMessage(wsdlOutputMessage);
        }

        Map faults = wsdl4jOperation.getFaults();
        Iterator faultKeyIterator = faults.keySet().iterator();
        WSDLFaultReference faultReference;

        while (faultKeyIterator.hasNext()) {

            Fault fault = (Fault) faults.get(faultKeyIterator.next());
            faultReference = wsdlComponentFactory.createFaultReference();
            faultReference.setDirection(
                    WSDLConstants.WSDL_MESSAGE_DIRECTION_OUT);
            Message faultMessage = fault.getMessage();
            if (null != faultMessage) {
                faultReference.setRef(
                        this.generateReferenceQname(
                                faultMessage.getQName(),
                                faultMessage,findWrapppable(faultMessage)));
            }
            wsdlOperation.addOutFault(faultReference);
            this.copyExtensibilityAttribute(fault.getExtensionAttributes(),
                    faultReference);
            //TODO Fault Message lable

        }

        //Set the MEP
        wsdlOperation.setMessageExchangePattern(WSDL11MEPFinder
                .getMEP(wsdl4jOperation));

    }

    /**
     * Generates a referenceQName
     * @param wsdl4jMessage
     * @return
     */
    private QName generateReferenceQname(QName outerName,Message wsdl4jMessage,boolean isWrappable) {
        QName referenceQName = null;
        //find the xsd prefix
        String xsdPrefix = findSchemaPrefix();

        //do the wrapping if required
        if (isWrappable) {
            Map parts = wsdl4jMessage.getParts();

            // Multipart Message

            // NOTE (gdaniels) : It appears this code is taking multiple
            // part declarations and "wrapping" them into a single schema
            // type.  This is fine for RPC style stuff, but should not be
            // happening for document style.
            // TODO : sanity check


            // Check whether this message parts have been made to a type
            Iterator multipartListIterator = this.resolvedMultipartMessageList.iterator();
            boolean multipartAlreadyResolved = false;
            while (multipartListIterator.hasNext() &&
                    !multipartAlreadyResolved) {
                QName temp = (QName) multipartListIterator.next();
                multipartAlreadyResolved =
                        wsdl4jMessage.getQName().equals(temp);
            }
            if (multipartAlreadyResolved) {
                //This message with multiple parts has resolved and a new type has been
                //made out of it earlier.
                //FIXME Actual element name should it be xs:, if yes change the qname added to the
                //resolvedmessage list too.
                referenceQName = wsdl4jMessage.getQName();
            } else {
                //Get the list of multiparts of the message and create a new Element
                //out of it and add it to the schema.
                Element schemaElement;
                WSDLTypes types = womDefinition.getTypes();

                //If types is null create a new one to be used for multipart 
                //resolution if any.

                if (null == types) {
                    //create a new types section
                    types = wsdlComponentFactory.createTypes();
                    this.womDefinition.setTypes(types);
                }

                //create a new Schema extensions element
                ExtensionFactory extensionFactory = wsdlComponentFactory.createExtensionFactory();
                org.apache.wsdl.extensions.Schema schemaExtensibilityElement = (org.apache.wsdl.extensions.Schema) extensionFactory.getExtensionElement(
                        ExtensionConstants.SCHEMA);
                types.addExtensibilityElement(schemaExtensibilityElement);


                //add a schema DOM element
                DocumentBuilder documentBuilder = getDOMDocumentBuilder();
                Document newDoc = documentBuilder.newDocument();
                schemaElement = newDoc.createElementNS(XMLSCHEMA_NAMESPACE_URI, xsdPrefix + ":"+XML_SCHEMA_LOCAL_NAME);
                schemaExtensibilityElement.setElement(schemaElement);

                //add this to the imported stack as well
                Stack stack = new Stack();
                stack.push(schemaElement);
                schemaExtensibilityElement.setImportedSchemaStack(stack);

                //add the namespace declarations
                String targetNamespaceUri = wsdl4jMessage.getQName().getNamespaceURI();
                if (targetNamespaceUri.trim().equals("")){
                    targetNamespaceUri = getTemporaryNamespaceUri();
                }
                //the DOMlevel2 documentation states that namespaces need to
                //declared like this
                schemaElement.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                              XMLNS_AXIS2WRAPPED,
                                              targetNamespaceUri);

                schemaElement.setAttribute(XSD_TARGETNAMESPACE,targetNamespaceUri);
                //schemaElement.setAttribute("xmlns:"+XMLSCHEMA_NAMESPACE_PREFIX,XMLSCHEMA_NAMESPACE_URI);

                //add the complex type
                Document doc = schemaElement.getOwnerDocument();
                String name = wsdl4jMessage.getQName().getLocalPart();
                Element newComplexType = doc.createElementNS(WSDLPump.XMLSCHEMA_NAMESPACE_URI, xsdPrefix + ":"+ XML_SCHEMA_COMPLEX_TYPE_LOCAL_NAME);
                newComplexType.setAttribute(WSDLPump.XSD_NAME, name);

                // add the sequence
                // Note -  we model the complex type as a sequence!
                Element cmplxContentSequence = doc.createElementNS(WSDLPump.XMLSCHEMA_NAMESPACE_URI, xsdPrefix + ":"+ XML_SCHEMA_SEQUENCE_LOCAL_NAME);
                Element child;
                Element relaventElementInSchemaReferedByPart = null;
                Iterator iterator = parts.keySet().iterator();
                while (iterator.hasNext()) {
                    Part part = (Part) parts.get(iterator.next());
                    //the part name
                    String elementName = part.getName();
                    //the type name
                    QName schemaTypeName = part.getTypeName();

                    //see whether this type is already present in one of the schemas
                    NodeList allSchemaElements = schemaElement.getChildNodes();
                    for(int idx = 0; idx < allSchemaElements.getLength(); idx++){
                        if(allSchemaElements.item(idx).getNodeType() == Node.ELEMENT_NODE &&
                                allSchemaElements.item(idx).getLocalName().equals(xsdPrefix + ":"+ XML_SCHEMA_ELEMENT_LOCAL_NAME)
                                && elementName.equals(((Element)allSchemaElements.item(idx)).getAttribute(WSDLPump.XSD_NAME))){
                            relaventElementInSchemaReferedByPart = (Element)allSchemaElements.item(idx);
                            break;
                        }
                    }
                    child = doc.createElementNS(XMLSCHEMA_NAMESPACE_URI, xsdPrefix + ":"+ XML_SCHEMA_ELEMENT_LOCAL_NAME);
                    child.setAttribute(WSDLPump.XSD_NAME, elementName);

                    if(null != relaventElementInSchemaReferedByPart){
                        child.setAttribute(WSDLPump.XSD_TYPE,
                                relaventElementInSchemaReferedByPart.getAttribute(WSDLPump.XSD_TYPE));
                    }else{
                        String prefix;
                        if (XMLSCHEMA_NAMESPACE_URI.equals(schemaTypeName.getNamespaceURI())){
                            prefix = xsdPrefix;
                        }else{
                            prefix = schemaTypeName.getPrefix();
                        }
                        child.setAttribute(WSDLPump.XSD_TYPE, prefix +":"+schemaTypeName.getLocalPart());
                    }
                    cmplxContentSequence.appendChild(child);
                }


                newComplexType.appendChild(cmplxContentSequence);
                schemaElement.appendChild(newComplexType);


                Element newElement = doc.createElementNS(XMLSCHEMA_NAMESPACE_URI, xsdPrefix + ":"+ XML_SCHEMA_ELEMENT_LOCAL_NAME);
                newElement.setAttribute(WSDLPump.XSD_NAME,
                        outerName.getLocalPart());
                newElement.setAttribute(WSDLPump.XSD_TYPE,
                        AXIS2WRAPPED +":"+//whats the prefix to put here!!!
                        wsdl4jMessage.getQName().getLocalPart());
                schemaElement.appendChild(newElement);

                //Now since  a new type is created augmenting the parts add the QName
                //of the newly created Element as the messageReference's name.
                //coincidentally it'll be the messages QName!

                referenceQName = outerName;

                //Add this message as a resolved message, so that incase some other
                //operation refer to the same message the if above will take a hit
                //and the cashed QName can be used instead of creating another type
                //for the same message.

                this.resolvedMultipartMessageList.add(referenceQName);

            }
        } else {
            //Only one part so copy the QName of the referenced type.
            Iterator outputIterator =
                    wsdl4jMessage.getParts().values().iterator();
            if (outputIterator.hasNext()) {
                Part outPart = ((Part) outputIterator.next());
                QName typeName;
                if (null != (typeName = outPart.getTypeName())) {
                    referenceQName = typeName;
                } else {
                    referenceQName = outPart.getElementName();
                }
            }
        }

        return referenceQName;
    }

    private DocumentBuilder getDOMDocumentBuilder() {
        DocumentBuilder documentBuilder;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return documentBuilder;
    }

    /**
     *   Find the  XML schema prefix
     */
    private String  findSchemaPrefix() {
        String xsdPrefix=null;
        if (declaredNameSpaces.containsValue(XMLSCHEMA_NAMESPACE_URI)){
            //loop and find the prefix
            Iterator it = declaredNameSpaces.keySet().iterator();
            String key;
            while (it.hasNext()) {
                key =  (String)it.next();
                if (XMLSCHEMA_NAMESPACE_URI.equals(declaredNameSpaces.get(key))){
                    xsdPrefix = key;
                    break;
                }
            }

        }else{
            xsdPrefix = XMLSCHEMA_NAMESPACE_PREFIX; //default prefix
        }

        return  xsdPrefix;
    }

    /**
     *
     * @param wsdlBindingOperation
     * @param wsdl4jBindingOperation
     * @param nameSpaceOfTheBindingOperation
     * @param wsdl4jDefinition
     */
    private void populateBindingOperation(
            WSDLBindingOperation wsdlBindingOperation,
            BindingOperation wsdl4jBindingOperation,
            String nameSpaceOfTheBindingOperation, Definition wsdl4jDefinition) {

        wsdlBindingOperation.setName(
                new QName(nameSpaceOfTheBindingOperation,
                        wsdl4jBindingOperation.getName()));

        BindingInput wsdl4jInputBinding =
                wsdl4jBindingOperation.getBindingInput();

        if (null != wsdl4jInputBinding) {
            WSDLBindingMessageReference wsdlInputBinding =
                    this.wsdlComponentFactory.createWSDLBindingMessageReference();
            wsdlInputBinding.setDirection(
                    WSDLConstants.WSDL_MESSAGE_DIRECTION_IN);
            this.copyExtensibleElements(
                    wsdl4jInputBinding.getExtensibilityElements(),
                    wsdlInputBinding, wsdl4jDefinition);
            wsdlBindingOperation.setInput(wsdlInputBinding);
        }

        BindingOutput wsdl4jOutputBinding = wsdl4jBindingOperation
                .getBindingOutput();
        if (null != wsdl4jOutputBinding) {
            WSDLBindingMessageReference wsdlOutputBinding = this.wsdlComponentFactory
                    .createWSDLBindingMessageReference();
            wsdlOutputBinding.setDirection(
                    WSDLConstants.WSDL_MESSAGE_DIRECTION_OUT);

            this.copyExtensibleElements(
                    wsdl4jOutputBinding.getExtensibilityElements(),
                    wsdlOutputBinding, null);
            wsdlBindingOperation.setOutput(wsdlOutputBinding);
        }


        Map bindingFaults = wsdl4jBindingOperation.getBindingFaults();
        Iterator keyIterator = bindingFaults.keySet().iterator();
        while (keyIterator.hasNext()) {
            BindingFault bindingFault = (BindingFault) bindingFaults.get(
                    keyIterator.next());
            WSDLBindingFault womBindingFault = this.wsdlComponentFactory.createBindingFault();
            this.copyExtensibleElements(
                    bindingFault.getExtensibilityElements(), womBindingFault, null);
            wsdlBindingOperation.addOutFault(womBindingFault);
        }

    }

    public void populatePorts(WSDLEndpoint wsdlEndpoint, Port wsdl4jPort,
                              String targetNamspace) {
        wsdlEndpoint.setName(new QName(targetNamspace, wsdl4jPort.getName()));

        wsdlEndpoint.setBinding(
                this.womDefinition.getBinding(
                        wsdl4jPort
                                .getBinding()
                                .getQName()));

    }

    /**
     * This method will fill up the gap of WSDL 1.1 and WSDL 2.0 w.r.t. the
     * bound interface for the Service Component Defined in the WSDL 2.0. Logic
     * being if there exist only one PortType in the WSDL 1.1 file then that
     * will be set as the bound interface of the Service. If more than one
     * Porttype exist in the WSDl 1.1 file this will create a dummy Interface
     * with the available PortTypes and will return that interface so that it
     * will inherit all those interfaces.
     * <p/>
     * Eventuall this will have to be fixed using user input since
     *
     * @param service
     * @return wsdl interface
     */
    private WSDLInterface getBoundInterface(WSDLService service) {

        // Throw an exception if there are no interfaces defined as at yet.
        if (0 == this.womDefinition.getWsdlInterfaces().size())
            throw new WSDLProcessingException("There are no "
                    +
                    "Interfaces/PortTypes identified in the current partially built"
                    + "WOM");

        //If there is only one Interface available hten return that because
        // normally
        // that interface must be the one to the service should get bound.
        if (1 == this.womDefinition.getWsdlInterfaces().size())
            return (WSDLInterface) this.womDefinition.getWsdlInterfaces()
                    .values().iterator().next();

        //If there are more than one interface available... For the time being
        // create a
        // new interface and set all those existing interfaces as
        // superinterfaces of it
        // and return.
        WSDLInterface newBoundInterface = this.womDefinition.createInterface();
        newBoundInterface.setName(
                new QName(service.getNamespace(),
                        service
                                .getName()
                                .getLocalPart()
                                + BOUND_INTERFACE_NAME));
        Iterator interfaceIterator = this.womDefinition.getWsdlInterfaces()
                .values().iterator();
        while (interfaceIterator.hasNext()) {
            newBoundInterface
                    .addSuperInterface(
                            (WSDLInterface) interfaceIterator.next());
        }
        return newBoundInterface;
    }

    /**
     * Get the Extensible elements form wsdl4jExtensibleElements
     * <code>Vector</code> if any and copy them to <code>Component</code>
     *


     @param wsdl4jExtensibleElements
      * @param component
     * @param wsdl4jDefinition

     */
    private void copyExtensibleElements(List wsdl4jExtensibleElements,
                                        Component component, Definition wsdl4jDefinition) {
        Iterator iterator = wsdl4jExtensibleElements.iterator();
        ExtensionFactory extensionFactory = this.wsdlComponentFactory
                .createExtensionFactory();
        while (iterator.hasNext()) {

            ExtensibilityElement wsdl4jElement = (ExtensibilityElement) iterator
                    .next();

            if (wsdl4jElement instanceof UnknownExtensibilityElement) {
                UnknownExtensibilityElement unknown = (UnknownExtensibilityElement) (wsdl4jElement);

                //look for the SOAP 1.2 stuff here. WSDL4j does not understand SOAP 1.2 things
                if (ExtensionConstants.SOAP_12_OPERATION.equals(unknown.getElementType())){
                    org.apache.wsdl.extensions.SOAPOperation soapOperationExtensibiltyElement = (org.apache.wsdl.extensions.SOAPOperation) extensionFactory
                            .getExtensionElement(wsdl4jElement.getElementType());
                    Element element = unknown.getElement();
                    soapOperationExtensibiltyElement.setSoapAction(element.getAttribute("soapAction"));
                    soapOperationExtensibiltyElement.setStyle(element.getAttribute("style"));
                    // soapActionRequired
                    component.addExtensibilityElement(soapOperationExtensibiltyElement);
                }else if (ExtensionConstants.SOAP_12_BODY.equals(unknown.getElementType())){
                    org.apache.wsdl.extensions.SOAPBody soapBodyExtensibiltyElement = (org.apache.wsdl.extensions.SOAPBody) extensionFactory
                            .getExtensionElement(wsdl4jElement.getElementType());
                    Element element = unknown.getElement();
                    soapBodyExtensibiltyElement.setUse(element.getAttribute("use"));
                    soapBodyExtensibiltyElement.setNamespaceURI(element.getAttribute("namespace"));
                    //encoding style
                    component.addExtensibilityElement(soapBodyExtensibiltyElement);
                }else if (ExtensionConstants.SOAP_12_HEADER.equals(unknown.getElementType())){
                    org.apache.wsdl.extensions.SOAPHeader soapHeaderExtensibilityElement = (org.apache.wsdl.extensions.SOAPHeader) extensionFactory.getExtensionElement(
                            unknown.getElementType());
                    //right now there's no known header binding!. Ignore the copying of values for now
                    component.addExtensibilityElement(soapHeaderExtensibilityElement);
                }else if (ExtensionConstants.SOAP_12_BINDING.equals(unknown.getElementType())){
                    org.apache.wsdl.extensions.SOAPBinding soapBindingExtensibiltyElement = (org.apache.wsdl.extensions.SOAPBinding) extensionFactory
                            .getExtensionElement(wsdl4jElement.getElementType());
                    Element element = unknown.getElement();
                    soapBindingExtensibiltyElement.setTransportURI(element.getAttribute("transport"));
                    soapBindingExtensibiltyElement.setStyle(element.getAttribute("style"));

                    component.addExtensibilityElement(soapBindingExtensibiltyElement);
                } else if (ExtensionConstants.SOAP_12_ADDRESS.equals(unknown.getElementType())){
                    org.apache.wsdl.extensions.SOAPAddress soapAddressExtensibiltyElement = (org.apache.wsdl.extensions.SOAPAddress) extensionFactory
                            .getExtensionElement(wsdl4jElement.getElementType());
                    Element element = unknown.getElement();
                    soapAddressExtensibiltyElement.setLocationURI(element.getAttribute("location"));
                    component.addExtensibilityElement(soapAddressExtensibiltyElement);
                }else{

                    DefaultExtensibilityElement defaultExtensibilityElement = (DefaultExtensibilityElement) extensionFactory
                            .getExtensionElement(wsdl4jElement.getElementType());
                    defaultExtensibilityElement.setElement(unknown.getElement());
                    Boolean required = unknown.getRequired();
                    if (null != required) {
                        defaultExtensibilityElement.setRequired(required.booleanValue());
                    }
                    component.addExtensibilityElement(defaultExtensibilityElement);
                }


            } else if (wsdl4jElement instanceof SOAPAddress) {
                SOAPAddress soapAddress = (SOAPAddress) wsdl4jElement;
                org.apache.wsdl.extensions.SOAPAddress soapAddressExtensibilityElement = (org.apache.wsdl.extensions.SOAPAddress) extensionFactory
                        .getExtensionElement(soapAddress.getElementType());
                soapAddressExtensibilityElement.setLocationURI(soapAddress
                        .getLocationURI());
                Boolean required = soapAddress.getRequired();
                if (null != required) {
                    soapAddressExtensibilityElement.setRequired(required.booleanValue());
                }
                component.addExtensibilityElement(soapAddressExtensibilityElement);
            } else if (wsdl4jElement instanceof Schema) {
                Schema schema = (Schema) wsdl4jElement;
                //schema.getDocumentBaseURI()
                //populate the imported schema stack
                Stack schemaStack = new Stack();
                //recursivly load the schema elements. The best thing is to push these into
                //a stack and then pop from the other side
                pushSchemaElement(schema, schemaStack);
                org.apache.wsdl.extensions.Schema schemaExtensibilityElement = (org.apache.wsdl.extensions.Schema) extensionFactory.getExtensionElement(
                        schema.getElementType());
                schemaExtensibilityElement.setElement(schema.getElement());
                schemaExtensibilityElement.setImportedSchemaStack(schemaStack);
                Boolean required = schema.getRequired();
                if (null != required) {
                    schemaExtensibilityElement.setRequired(required.booleanValue());
                }
                //set the name of this Schema element
                //todo this needs to be fixed
                if(schema.getDocumentBaseURI() != null) {
                    schemaExtensibilityElement.setName(new QName("",schema.getDocumentBaseURI()));
                }
                component.addExtensibilityElement(schemaExtensibilityElement);
            } else if (SOAPConstants.Q_ELEM_SOAP_OPERATION.equals(
                    wsdl4jElement.getElementType())) {
                SOAPOperation soapOperation = (SOAPOperation) wsdl4jElement;
                org.apache.wsdl.extensions.SOAPOperation soapOperationextensibilityElement = (org.apache.wsdl.extensions.SOAPOperation) extensionFactory.getExtensionElement(
                        soapOperation.getElementType());
                soapOperationextensibilityElement.setSoapAction(
                        soapOperation.getSoapActionURI());
                soapOperationextensibilityElement.setStyle(soapOperation.getStyle());
                Boolean required = soapOperation.getRequired();
                if (null != required) {
                    soapOperationextensibilityElement.setRequired(required.booleanValue());
                }
                component.addExtensibilityElement(soapOperationextensibilityElement);
            } else if (SOAPConstants.Q_ELEM_SOAP_BODY.equals(
                    wsdl4jElement.getElementType())) {
                SOAPBody soapBody = (SOAPBody) wsdl4jElement;
                org.apache.wsdl.extensions.SOAPBody soapBodyExtensibilityElement = (org.apache.wsdl.extensions.SOAPBody) extensionFactory.getExtensionElement(
                        soapBody.getElementType());
                soapBodyExtensibilityElement.setNamespaceURI(
                        soapBody.getNamespaceURI());
                soapBodyExtensibilityElement.setUse(soapBody.getUse());
                Boolean required = soapBody.getRequired();
                if (null != required) {
                    soapBodyExtensibilityElement.setRequired(required.booleanValue());
                }

                component.addExtensibilityElement(soapBodyExtensibilityElement);
                //add the header
            } else if (SOAPConstants.Q_ELEM_SOAP_HEADER.equals(
                    wsdl4jElement.getElementType())) {
                SOAPHeader soapHeader = (SOAPHeader) wsdl4jElement;
                org.apache.wsdl.extensions.SOAPHeader soapHeaderExtensibilityElement = (org.apache.wsdl.extensions.SOAPHeader) extensionFactory.getExtensionElement(
                        soapHeader.getElementType());
                soapHeaderExtensibilityElement.setNamespaceURI(
                        soapHeader.getNamespaceURI());
                soapHeaderExtensibilityElement.setUse(soapHeader.getUse());
                Boolean required = soapHeader.getRequired();
                if (null != required) {
                    soapHeaderExtensibilityElement.setRequired(required.booleanValue());
                }
                if (null!=wsdl4jDefinition){
                    //find the relevant schema part from the messages
                    Message msg = wsdl4jDefinition.getMessage(soapHeader.getMessage());
                    Part msgPart = msg.getPart(soapHeader.getPart());
                    soapHeaderExtensibilityElement.setElement(msgPart.getElementName());
                }
                soapHeaderExtensibilityElement.setMessage(soapHeader.getMessage());

                soapHeaderExtensibilityElement.setPart(soapHeader.getPart());
                soapHeader.getMessage();
                component.addExtensibilityElement(soapHeaderExtensibilityElement);
            } else if (SOAPConstants.Q_ELEM_SOAP_BINDING.equals(
                    wsdl4jElement.getElementType())) {
                SOAPBinding soapBinding = (SOAPBinding) wsdl4jElement;
                org.apache.wsdl.extensions.SOAPBinding soapBindingExtensibilityElement = (org.apache.wsdl.extensions.SOAPBinding) extensionFactory.getExtensionElement(
                        soapBinding.getElementType());
                soapBindingExtensibilityElement.setTransportURI(
                        soapBinding.getTransportURI());
                soapBindingExtensibilityElement.setStyle(soapBinding.getStyle());
                Boolean required = soapBinding.getRequired();
                if (null != required) {
                    soapBindingExtensibilityElement.setRequired(required.booleanValue());
                }
                component.addExtensibilityElement(soapBindingExtensibilityElement);
            }
        }
    }

    /**
     * Get the Extensible Attributes from wsdl4jExtensibilityAttribute
     * <code>Map</code> if any and copy them to the <code>Component</code>
     *
     * @param wsdl4jExtensibilityAttributes
     * @param component
     */
    private void copyExtensibilityAttribute(Map wsdl4jExtensibilityAttributes,
                                            Component component) {
        Iterator iterator = wsdl4jExtensibilityAttributes.keySet().iterator();
        while (iterator.hasNext()) {
            QName attributeName = (QName) iterator.next();
            QName value = (QName) wsdl4jExtensibilityAttributes
                    .get(attributeName);
            WSDLExtensibilityAttribute attribute = this.wsdlComponentFactory
                    .createWSDLExtensibilityAttribute();
            attribute.setKey(attributeName);
            attribute.setValue(value);
            component.addExtensibleAttributes(attribute);
        }
    }

    private String getTemporaryNamespaceUri(){
        return "urn:tempNs"+nsCount++ ;
    }
}
