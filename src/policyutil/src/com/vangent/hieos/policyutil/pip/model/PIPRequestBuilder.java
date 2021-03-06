/*
 * This code is subject to the HIEOS License, Version 1.0
 *
 * Copyright(c) 2011 Vangent, Inc.  All rights reserved.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vangent.hieos.policyutil.pip.model;

import com.vangent.hieos.policyutil.util.PolicyConstants;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;

/**
 *
 * @author Bernie Thuman
 */
public class PIPRequestBuilder {

    //<pip:GetConsentDirectivesRequest xsi:schemaLocation="urn:hieos:policy:pip PIP.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:pip="urn:hieos:policy:pip">
    //  <pip:PatientId>String</pip:PatientId>
    //</pip:GetConsentDirectivesRequest>
    /**
     * 
     * @param pipRequest
     * @return
     */
    public PIPRequestElement buildPIPRequestElement(PIPRequest pipRequest) {
        OMFactory omfactory = OMAbstractFactory.getOMFactory();

        // Request
        OMElement requestNode = omfactory.createOMElement(new QName(PolicyConstants.HIEOS_PIP_NS, "GetConsentDirectivesRequest", PolicyConstants.HIEOS_PIP_NS_PREFIX));

        // PatientId
        OMElement patientIdNode = omfactory.createOMElement(new QName(PolicyConstants.HIEOS_PIP_NS, "PatientId", PolicyConstants.HIEOS_PIP_NS_PREFIX));
        patientIdNode.setText(pipRequest.getPatientId().getCXFormatted());
        requestNode.addChild(patientIdNode);
        
        return new PIPRequestElement(requestNode);
    }
}
