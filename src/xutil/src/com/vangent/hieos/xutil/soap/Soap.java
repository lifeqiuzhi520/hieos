/*
 * This code is subject to the HIEOS License, Version 1.0
 *
 * Copyright(c) 2008-2009 Vangent, Inc.  All rights reserved.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vangent.hieos.xutil.soap;

//import com.vangent.hieos.xutil.exception.XdsException;
//import com.vangent.hieos.xutil.exception.XdsFormatException;
//import com.vangent.hieos.xutil.exception.XdsInternalException;
//import com.vangent.hieos.xutil.metadata.structure.MetadataSupport;
import com.vangent.hieos.xutil.exception.SOAPFaultException;
import com.vangent.hieos.xutil.xml.Util;

import java.util.HashMap;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.OperationContext;

import com.vangent.hieos.xutil.xconfig.XConfig;
import com.vangent.hieos.xutil.xua.client.XServiceUser;
//import java.util.List;
//import org.apache.axis2.engine.Phase;
//import com.vangent.hieos.xutil.xua.handlers.XUAOutPhaseHandler;
import com.vangent.hieos.xutil.xua.utils.XUAConstants;
import com.vangent.hieos.xutil.xua.utils.XUAObject;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.log4j.Logger;

/**
 * Main point of making SOAP requests via AXIS2.
 *
 * @author Bernie Thuman (adapted from original NIST code).
 */
public class Soap {

    private final static Logger logger = Logger.getLogger(Soap.class);
    // Default values (if XConfig is not available).
    private static final long DEFAULT_ASYNC_TIMEOUT_MSEC = 120000;  // 120 secs.
    private static final long DEFAULT_SYNC_TIMEOUT_MSEC = 45000;    // 45 secs.
    private static final String DEFAULT_ASYNC_RESPONSE_PORT = "8080";
    // XConfig propertie names.
    private static final String XCONFIG_PARAM_ASYNC_TIMEOUT_MSEC = "SOAPAsyncTimeOutInMilliseconds";
    private static final String XCONFIG_PARAM_SYNC_TIMEOUT_MSEC = "SOAPtimeOutInMilliseconds";
    private static final String XCONFIG_PARAM_ASYNC_RESPONSE_PORT = "SOAPAsyncResponseHTTPPort";
    // Axis2 property names.
    private static final String AXIS2_PARAM_RUNNING_PORT = "RUNNING_PORT";
    private static final String XUA_OUT_PHASE_NAME = "XUAOutPhase";
    // Private variables:
    private XUAObject xuaObject = null;             // Only used if XUA is enabled (null if not used).
    private ServiceClient serviceClient = null;     // Cached Axis2 ServiceClient.
    private OMElement result = null;                // Holds the SOAP result.
    private boolean async = false;                  // Boolean value (determines "async" mode).
    private MessageContext parentThreadMessageContext = null;

    /**
     * Set boolean value to determine if this request should be an asynchronous
     * SOAP request.
     *
     * @param async Set to true if asynchronous.  Otherwise, set to false.
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     *
     * @param parentThreadMessageContext
     */
    public void setParentThreadMessageContext(MessageContext parentThreadMessageContext) {
        this.parentThreadMessageContext = parentThreadMessageContext;
    }

    /**
     * Sets the XUA object used to properly generate SAML during Axis2 outbound
     * message handling.
     *
     * @param xuaObj The XUAObject.
     */
    public void setXUAObject(XUAObject xuaObj) {
        this.xuaObject = xuaObj;
    }

    /**
     * Issues a SOAP call to the target endpoint.
     *
     * @param body The SOAP body to send.
     * @param endpoint The target endpoint of the request.
     * @param mtom Set to true if MTOM should be enabled.  Otherwise, false.
     * @param addressing Set to true if SOAP addressing should be enabled.  Otherwise, false.
     * @param soap12 Set to true if SOAP 1.2 should be enabled.  Otherwise, false.
     * @param action The SOAP action associated with the request.
     * @param expectedReturnAction The expected SOAP return action.
     * @return The SOAP body of the result.
     * @throws SOAPFaultException
     */
    public OMElement soapCall(OMElement body, String endpoint, boolean mtom,
            boolean addressing, boolean soap12, String action, String expectedReturnAction)
            throws SOAPFaultException {

        try {
            // Get the AXIS2 ServiceClient.
            if (this.serviceClient == null) {
                this.serviceClient = new ServiceClient();
            }
            // Setup ServiceClient options.
            Options options = this.serviceClient.getOptions();
            this.setTargetEndpoint(options, endpoint);
            this.setMTOMOption(options, mtom);
            this.setTimeOutFromConfig(options);
            this.setSOAPAction(options, action);
            this.setAddressing(this.serviceClient, addressing);
            this.setSOAPVersion(options, soap12);

            // Configure for "async" mode (if required).
            if (this.async) {
                if (!options.isUseSeparateListener()) {
                    options.setUseSeparateListener(this.async);
                }
                // Now, check to see if the request is simply "http".
                if (endpoint.startsWith("http://")) {
                    this.setAsyncResponsePort();
                    // NOTE: HTTPS is handled via "axis2.xml" configuration.
                }
            }

            // Setup for XUA (if required).
            this.setupXUA(serviceClient, action);

            HttpConnectionManager connMgr =
                    new XUtilSimpleHttpConnectionManager(true);
            HttpClient httpClient = new HttpClient(connMgr);

            // set the above created objects to re use.
            options.setProperty(HTTPConstants.REUSE_HTTP_CLIENT,
                    Constants.VALUE_TRUE);
            options.setProperty(HTTPConstants.CACHED_HTTP_CLIENT,
                    httpClient);

            /*
             * This cleanup option will call response.getEnvelope().build()
             * However, envelope.build() does not build everything
             * and any subsequent access to the atttachement(s) will
             * throw a closed stream exception,
             * an explicit clean up is below
             *
             * options.setCallTransportCleanup(true);
             */

            // Make the SOAP request (and save the result).
            this.result = serviceClient.sendReceive(body);

            // explicitly build the whole response and clean up
            if (this.result != null) {

                MessageContext mc = this.serviceClient.getLastOperationContext().
                        getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
                if (mtom) {
                    mc.getEnvelope().buildWithAttachments();
                } else {
                    mc.getEnvelope().build();
                }

                this.serviceClient.cleanupTransport();
            }

            // Cleanup after "async" (if required).
//            if (this.async) {
//                serviceClient.cleanupTransport();
//            }

            // Make sure response is what is expected.
            this.validateSOAPResponse(this.serviceClient, mtom);

            // Validate proper return action is received.
            if (this.async) {
                verifySOAPReturnAction(expectedReturnAction, "urn:mediateResponse");
            } else if (addressing) {  // Only validate in this case.
                verifySOAPReturnAction(expectedReturnAction, null);
            }
        } catch (AxisFault ex) {
            throw new SOAPFaultException(ex.getMessage());
        }

        // Return the SOAP result.
        return this.result;
    }

    /**
     * Returns the result of the SOAP request.
     *
     * @return The SOAP body response.
     */
    public OMElement getResult() {
        return result;
    }

    /*
     * Returns a deep copy of the SOAP "in" header.
     *
     * @return A deep copy of the SOAP "in" header.
     */
    public OMElement getInHeader() {
        OperationContext oc = serviceClient.getLastOperationContext();
        HashMap<String, MessageContext> ocs = oc.getMessageContexts();
        MessageContext in = ocs.get("In");
        if (in == null) {
            return null;
        }
        if (in.getEnvelope() == null) {
            return null;
        }
        if (in.getEnvelope().getHeader() == null) {
            return null;
        }
        return Util.deep_copy(in.getEnvelope().getHeader());
    }

    /**
     * Returns a deep copy of the SOAP "out" header.
     *
     * @return A deep copy of the SOAP "out" header.
     */
    public OMElement getOutHeader() {
        OperationContext oc = serviceClient.getLastOperationContext();
        HashMap<String, MessageContext> ocs = oc.getMessageContexts();
        MessageContext out = ocs.get("Out");
        if (out == null) {
            return null;
        }
        return Util.deep_copy(out.getEnvelope().getHeader());
    }

    /**
     * Set the target endpoint.
     *
     * @param options Axis2 ServiceClient options.
     * @param endpoint The target http/https endpoint.
     */
    private void setTargetEndpoint(Options options, String endpoint) {
        options.setTo(new EndpointReference(endpoint));
    }

    /**
     * Set the MTOM option (true or false).
     *
     * @param options Axis2 ServiceClient options.
     * @param mtom true if MTOM should be enabled.  Otherwise, false.
     */
    private void setMTOMOption(Options options, boolean mtom) {
        options.setProperty(Constants.Configuration.ENABLE_MTOM,
                ((mtom) ? Constants.VALUE_TRUE : Constants.VALUE_FALSE));
    }

    /**
     * Set the "time out" for the request.  Pulled from XConfig if available; otherwise,
     * sets to default values.
     *
     * @param options Axis2 ServiceClient options.
     */
    private void setTimeOutFromConfig(Options options) {
        // Set defaults first in case configuration is not available.
        long timeOut;
        if (this.async) {
            timeOut = Soap.DEFAULT_ASYNC_TIMEOUT_MSEC;
        } else {
            timeOut = Soap.DEFAULT_SYNC_TIMEOUT_MSEC;
        }
        try {
            XConfig cfg = XConfig.getInstance();
            if (this.async) {
                timeOut = cfg.getHomeCommunityConfigPropertyAsLong(Soap.XCONFIG_PARAM_ASYNC_TIMEOUT_MSEC);
            } else {
                timeOut = cfg.getHomeCommunityConfigPropertyAsLong(Soap.XCONFIG_PARAM_SYNC_TIMEOUT_MSEC);
            }
        } catch (Exception e) {
            logger.warn("Unable to get SOAP timeout from XConfig -- using default");
        }
        options.setTimeOutInMilliSeconds(timeOut);
    }

    /**
     * Set the SOAP action to use in the request.
     *
     * @param options Axis2 ServiceClient options.
     * @param action The SOAP action.
     */
    private void setSOAPAction(Options options, String action) {
        options.setAction(action);
    }

    /**
     * Engage (or desengage) Axis2 SOAP "addressing".
     *
     * @param sc The Axis2 ServiceClient.
     * @param addressing true if SOAP addressing should be used.  Otherwise, false.
     * @throws AxisFault
     */
    private void setAddressing(ServiceClient sc, boolean addressing) throws AxisFault {
        if (addressing) {
            sc.engageModule("addressing");
        } else {
            sc.disengageModule("addressing");    // this does not work in Axis2 yet
        }
    }

    /**
     * Set the SOAP version to use.
     *
     * @param options Axis2 ServiceClient options.
     * @param soap12 true if SOAP1.2 should be used.  false if SOAP1.1.
     */
    private void setSOAPVersion(Options options, boolean soap12) {
        options.setSoapVersionURI(
                ((soap12) ? SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI : SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI));

    }

    /**
     * Configure Axis2 port to use for asynchronous SOAP responses.  Should set in
     * XConfig (a default port will be used if not found) to match the HTTP listener
     * port being used by the application server.
     */
    private void setAsyncResponsePort() {
        // Get default in case configuration is not set.
        String responsePort = Soap.DEFAULT_ASYNC_RESPONSE_PORT;

        // Now, set the proper listening port.
        ConfigurationContext ctx = this.serviceClient.getServiceContext().getConfigurationContext();
        if (ctx.getProperty(Soap.AXIS2_PARAM_RUNNING_PORT) == null) {
            try {
                XConfig cfg = XConfig.getInstance();
                responsePort = cfg.getHomeCommunityConfigProperty(Soap.XCONFIG_PARAM_ASYNC_RESPONSE_PORT);
            } catch (Exception e) {
                logger.warn("Unable to get " + Soap.XCONFIG_PARAM_ASYNC_RESPONSE_PORT + " from XConfig -- using default");
            }
            ctx.setProperty(Soap.AXIS2_PARAM_RUNNING_PORT, responsePort);
        }
    }

    /**
     * Validate that if the inbound request was MTOM, the result is also.
     *
     * @param sc The ServiceClient used to support SOAP request.
     * @param mtomExpected Set to true if MTOM is expected.
     * @throws AxisFault
     */
    private void validateSOAPResponse(ServiceClient sc, boolean mtomExpected) throws AxisFault {
        Object in = sc.getServiceContext().getLastOperationContext().getMessageContexts().get("In");
        if (!(in instanceof MessageContext)) {
            throw new AxisFault("Soap: In MessageContext of type " + in.getClass().getName() + " instead of MessageContext");
        }
        MessageContext messageContext = (MessageContext) in;
        boolean responseMtom = messageContext.isDoingMTOM();
        if (mtomExpected != responseMtom) {
            if (mtomExpected) {
                throw new AxisFault("Request was MTOM format but response was SIMPLE SOAP");
            } else {
                throw new AxisFault("Request was SIMPLE SOAP but response was MTOM");
            }
        }
    }

    /**
     * Verify that the the SOAP response includes the extected return SOAP action.
     *
     * @param expectedReturnAction Expected SOAP return action.
     * @param alternateReturnAction Alternative expected SOAP return action.
     * @throws AxisFault
     */
    private void verifySOAPReturnAction(String expectedReturnAction, String alternateReturnAction) throws AxisFault {
        if (expectedReturnAction == null) {
            return;  // None expected.
        }
        // First see if a SOAP header exists.
        OMElement soapHeader = this.getInHeader();
        if (soapHeader == null) {
            throw new AxisFault(
                    "No SOAPHeader returned: expected header with action = " + expectedReturnAction);
        }

        // Now see if the SOAP action exists.
        OMElement action = this.firstChildWithLocalName(soapHeader, "Action");
        if (action == null) {
            throw new AxisFault(
                    "No action returned in SOAPHeader: expected action = " + expectedReturnAction);
        }

        // Now get the SOAP action value and compare against expected results.
        String soapActionValue = action.getText();
        if (alternateReturnAction == null) {
            if (soapActionValue == null || !soapActionValue.equals(expectedReturnAction)) {
                throw new AxisFault(
                        "Wrong action returned in SOAPHeader: expected action = " + expectedReturnAction
                        + " returned action = " + soapActionValue);
            }
        } else {
            if (soapActionValue == null
                    || ((!soapActionValue.equals(expectedReturnAction)) && (!soapActionValue.equals(alternateReturnAction)))) {
                throw new AxisFault(
                        "Wrong action returned in SOAPHeader: expected action = " + expectedReturnAction
                        + " returned action = " + soapActionValue);
            }
        }
    }

    /**
     * Placed here to remove dependency on XDS classes.
     *
     * @param ele
     * @param localName
     * @return OMElement
     *
     * FIXME: Find better way.
     */
    private OMElement firstChildWithLocalName(OMElement ele, String localName) {
        for (Iterator it = ele.getChildElements(); it.hasNext();) {
            OMElement child = (OMElement) it.next();
            if (child.getLocalName().equals(localName)) {
                return child;
            }
        }
        return null;
    }

    /**
     * 
     * @param serviceClient
     * @param soapAction
     * @throws AxisFault
     */
    private void setupXUA(ServiceClient serviceClient, String soapAction) throws AxisFault {
        OMElement currentSecurityHeader = this.getCurrentSecurityHeader();
        if (currentSecurityHeader != null) {
            // Must be on server-side -- propogate Security header on out-bound requests.
            serviceClient.addHeader(currentSecurityHeader);
        } else if ((xuaObject != null)
                && xuaObject.isXUAEnabled()
                && xuaObject.containsSOAPAction(soapAction)) {
            this.setupSecurityHeader(serviceClient, soapAction);
            /*
            List outFlowPhases = serviceClient.getAxisConfiguration().getOutFlowPhases();
            // Check to see if the out phase handler already exists
            for (Iterator it = outFlowPhases.iterator(); it.hasNext();) {
            Phase phase = (Phase) it.next();
            if (phase.getName().equals(XUA_OUT_PHASE_NAME)) {
            // Already exists.
            return;  // EARLY EXIT!
            }
            }
            logger.info("Adding XUA out phase handler!!!");
            Phase xuaOutPhase = this.getXUAOutPhaseHandler();
            outFlowPhases.add(xuaOutPhase);*/
        }
    }

    /**
     * 
     * @param serviceClient
     * @param soapAction
     * @throws AxisFault
     */
    public void setupSecurityHeader(ServiceClient serviceClient, String soapAction) throws AxisFault {

        // Get SAML assertion from STS issuer.
        XServiceUser xServiceUser = new XServiceUser();
        try {
            // Get the SAML assertion from the STS provider (for the given user):
            SOAPEnvelope responseEnvelope = xServiceUser.getToken(this.xuaObject);
            if (logger.isDebugEnabled()) {
                //logger.debug("XUA: XUAOutPhaseHandler::invoke - STS Response: " + responseEnvelope.toString());
                logger.info("XUA: XUAOutPhaseHandler::invoke - STS Response: " + responseEnvelope.toString());
            }
            OMElement samlTokenEle = xServiceUser.getTokenFromSTSResponse(responseEnvelope);
            if (logger.isDebugEnabled()) {
                //logger.debug("XUA: XUAOutPhaseHandler::invoke - SAML Token: " + samlTokenEle.toString());
                logger.info("XUA: XUAOutPhaseHandler::invoke - SAML Token: " + samlTokenEle.toString());
            }

            // Create WS-Security wrapper element.
            OMFactory omFactory = OMAbstractFactory.getOMFactory();
            OMNamespace wsseNS = omFactory.createOMNamespace(XUAConstants.WS_SECURITY_NS_URL, XUAConstants.WS_SECURITY_NS_PREFIX);
            OMElement wsseSecurityHeader = omFactory.createOMElement(XUAConstants.WS_SECURITY_ELEMENT_NAME, wsseNS);

            // Attach assertion to security header.
            logger.info("Attaching WS-Security header with SAML token to SOAP header");
            wsseSecurityHeader.addChild(samlTokenEle);
            serviceClient.addHeader(wsseSecurityHeader);

        } catch (Exception ex) {
            logger.info("Unable to invoke STS to get SAML token" + ex.getLocalizedMessage());
            throw new AxisFault("Unable to invoke STS to get SAML token" + ex.getLocalizedMessage());
        }
    }

    /**
     * Sets the XUA "Out Phase Handler".
     * 
     * @return Axis2 Phase (XUAOutPhaseHandler).
     */
    /*
    private Phase getXUAOutPhaseHandler() {
    Phase phase = null;
    try {
    phase = new Phase(XUA_OUT_PHASE_NAME);
    XUAOutPhaseHandler xuaOutPhaseHandler = new XUAOutPhaseHandler();
    xuaOutPhaseHandler.setXUAObject(this.xuaObject);
    phase.addHandler(xuaOutPhaseHandler);

    } catch (Throwable t) {
    logger.error("Exception while initializing the XUA out phase handler", t);
    t.printStackTrace(System.out);
    }
    return phase;
    }*/
    /**
     *
     * @return
     */
    private OMElement getCurrentSecurityHeader() {
        OMElement securityHeader = null;
        MessageContext currentMessageContext = this.getCurrentMessageContext();
        if (currentMessageContext != null) {
            // Get the SOAP envelope from the current message context
            SOAPEnvelope env = currentMessageContext.getEnvelope();

            // Check to see if the envelope contains a Security header ... if so, propagate
            SOAPHeader header = env.getHeader();
            if (header != null) {
                OMElement securityHeaderFromRequest = header.getFirstChildWithName(
                        new QName(XUAConstants.WS_SECURITY_NS_URL, "Security"));
                securityHeader = Util.deep_copy(securityHeaderFromRequest);
            }
        }
        return securityHeader;
    }

    /**
     *
     * @return
     */
    private MessageContext getCurrentMessageContext() {
        MessageContext currentMessageContext = MessageContext.getCurrentMessageContext();
        if (currentMessageContext == null) {
            // May be sending outbound SOAP requests in multi-threaded mode (i.e. XCA, XCPD).
            currentMessageContext = this.parentThreadMessageContext;
        }
        return currentMessageContext;
    }
}
