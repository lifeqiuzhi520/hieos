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


package com.vangent.hieos.services.xds.bridge.activity;

import com.vangent.hieos.services.xds.bridge.client.XDSDocumentRegistryClient;
import com.vangent.hieos.services.xds.bridge.message
    .GetDocumentsSQRequestBuilder;
import com.vangent.hieos.services.xds.bridge.message
    .GetDocumentsSQRequestMessage;
import com.vangent.hieos.services.xds.bridge.message
    .GetDocumentsSQResponseMessage;
import com.vangent.hieos.services.xds.bridge.model.Document;
import com.vangent.hieos.services.xds.bridge.model.ResponseType
    .ResponseTypeStatus;
import com.vangent.hieos.services.xds.bridge.model.SubmitDocumentResponse;
import com.vangent.hieos.services.xds.bridge.support.URIConstants;
import com.vangent.hieos.xutil.exception.SOAPFaultException;
import com.vangent.hieos.xutil.exception.XdsException;
import com.vangent.hieos.xutil.exception.XdsInternalException;
import com.vangent.hieos.xutil.response.RegistryError;
import com.vangent.hieos.xutil.response.RegistryResponseParser;
import com.vangent.hieos.xutil.xml.XPathHelper;

import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.ClassUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;


/**
 * Class description
 *
 *
 * @version        v1.0, 2011-07-13
 * @author         Vangent
 */
public abstract class AbstractGetDocumentsSQActivity
        implements ISubmitDocumentRequestActivity {

    /** Field description */
    private static final Logger logger =
        Logger.getLogger(AbstractGetDocumentsSQActivity.class);

    /** Field description */
    private final XDSDocumentRegistryClient registryClient;

    /**
     * Constructs ...
     *
     *
     * @param registryClient
     */
    public AbstractGetDocumentsSQActivity(
            XDSDocumentRegistryClient registryClient) {

        super();
        this.registryClient = registryClient;
    }

    /**
     * Method description
     *
     *
     *
     * @param context
     * @param docId
     * @return
     */
    protected GetDocumentsSQResponseMessage callGetDocumentsSQ(
            SDRActivityContext context, String docId) {

        GetDocumentsSQResponseMessage result = null;

        try {

            GetDocumentsSQRequestBuilder builder =
                new GetDocumentsSQRequestBuilder();

            GetDocumentsSQRequestMessage msg = builder.buildMessage(docId);

            result = getRegistryClient().getDocuments(msg);

        } catch (SOAPFaultException e) {

            SubmitDocumentResponse sdrResponse =
                context.getSubmitDocumentResponse();

            String msg =
                String.format(
                    "%s:%s caused an error: %s.",
                    XDSDocumentRegistryClient.STORED_QUERY_TRANS,
                    XDSDocumentRegistryClient.STORED_QUERY_REQUEST_ACTION,
                    e.getMessage());

            sdrResponse.addResponse(ResponseTypeStatus.Failure, msg);
        }

        return result;
    }

    /**
     * Method description
     *
     *
     * @return
     */
    @Override
    public String getName() {
        return ClassUtils.getShortClassName(getClass());
    }

    /**
     * Method description
     *
     *
     * @return
     */
    public XDSDocumentRegistryClient getRegistryClient() {
        return registryClient;
    }

    /**
     * Method description
     *
     *
     * @param registryResponse
     * @param context
     *
     * @return
     */
    protected List<String> parseObjectRefs(
            GetDocumentsSQResponseMessage registryResponse,
            SDRActivityContext context)
            throws XdsException {

        List<String> result = new ArrayList<String>();

        SubmitDocumentResponse sdrResponse =
            context.getSubmitDocumentResponse();
        Document document = context.getDocument();
        OMElement rootNode = registryResponse.getElement();

        RegistryResponseParser parser = new RegistryResponseParser(rootNode);
        
        // The registry may return success even though there was
        // a token or policy issue/problem so we can not rely on "Success"
        List<RegistryError> registryErrors = parser.parseErrors();
        
        if (parser.is_error() || registryErrors.isEmpty() == false) {

            // If there are any errors or warnings then
            // propagate back to caller
            StringBuilder errmsg = new StringBuilder();
            errmsg.append("The XDS.b Registry returned the following errors/warnings: \n");
            for (RegistryError error : registryErrors) {
             
                String msg = String.format("%s: %s%n",
                        error.getCode(), error.getContext());
                
                errmsg.append(msg);
            }

            throw new XdsException(errmsg.toString());

        } else {

            try {

                // search for any nodes
                String expr = "./ns:RegistryObjectList/ns:ObjectRef";
                List<OMElement> docs = XPathHelper.selectNodes(rootNode, expr,
                                           URIConstants.RIM_URI);

                if (docs != null) {

                    QName idQName = new QName("id");

                    for (OMElement doc : docs) {

                        result.add(doc.getAttributeValue(idQName));
                    }
                }
                
            } catch (XdsInternalException e) {

                // log it
                logger.error(e, e);

                // capture in response
                StringBuilder sb = new StringBuilder();

                sb.append(
                    "Unable to parse repository response, exception follows. ");
                sb.append(e.getMessage());

                sdrResponse.addResponse(document, ResponseTypeStatus.Failure,
                                        sb.toString());
            }
        }

        return result;
    }
}
