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
package com.vangent.hieos.services.xca.gateway.serviceimpl;

import com.vangent.hieos.xutil.exception.SOAPFaultException;

/**
 * XCARespondingGatewayAsync is a simple class that overrides a few methods in
 * its superclass, to provide certain asynchronous behaviors.
 *
 * @author Anand Sastry
 */
public class XCARespondingGatewayAsync extends XCARespondingGateway {

    /**
     * This method ensures that an asynchronous request has been sent. It evaluates the message
     * context to dtermine if "ReplyTo" is non-null and is not anonymous. It also ensures that
     * "MessageID" is non-null. It throws an exception if that is not the case.
     * @throws XdsWSException
     */
    @Override
    protected void validateWS() throws SOAPFaultException {
        validateAsyncWS();
    }

    /**
     * This method returns the Query Transaction name for the Async Gateway.
     * @return a String value representing a PnR transaction name.
     */
    @Override
    protected String getQueryTransactionName() {
        return super.getQueryTransactionName() + " ASync";
    }

    /**
     * This method returns the Retrieve Transaction name for the Async Gateway.
     * @return a String value representing a PnR transaction name.
     */
    @Override
    protected String getRetTransactionName() {
        return super.getRetTransactionName() + " ASync";
    }
}
