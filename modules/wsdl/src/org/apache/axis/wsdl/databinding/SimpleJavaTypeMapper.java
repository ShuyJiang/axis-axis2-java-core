package org.apache.axis.wsdl.databinding;

import org.apache.axis.om.OMElement;

import javax.xml.namespace.QName;
import java.util.HashMap;

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
 *
 * 
 */
public class SimpleJavaTypeMapper extends TypeMappingAdapter{



    /*
Java	SOAP
String	xsd:string
Boolean	xsd:boolean
boolean	xsd:boolean
Double	xsd:double
double	xsd:double
Long	xsd:long
long	xsd:long
Float	xsd:float
float	xsd:float
Integer	xsd:int
int	xsd:int
Short	xsd:short
short	xsd:short
Byte	xsd:byte
byte	xsd:byte
BigDecimal	xsd:decimal
GregorianCalendar	xsd:timeInstant!
Date	xsd:date
QName	xsd:QName

    */
    public SimpleJavaTypeMapper() {
       this.map.put("string",String.class);
       //add the rest todo the key should be a QName
    }

}
