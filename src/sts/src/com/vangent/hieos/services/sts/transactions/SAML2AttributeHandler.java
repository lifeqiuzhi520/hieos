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
package com.vangent.hieos.services.sts.transactions;

import com.vangent.hieos.services.sts.exception.STSException;
import com.vangent.hieos.services.sts.model.Claim;
import com.vangent.hieos.services.sts.model.STSRequestData;
import java.util.ArrayList;
import java.util.List;
import org.opensaml.saml2.core.Attribute;

/**
 *
 * @author Bernie Thuman
 */
public class SAML2AttributeHandler {

    /**
     *
     * @param requestData
     * @return
     * @throws STSException
     */
    public List<Attribute> handle(STSRequestData requestData) throws STSException {
        List<Claim> claims = requestData.getClaims();
        return this.getAttributes(claims);
    }

    /**
     *
     * @param claims
     * @return
     */
    private List<Attribute> getAttributes(List<Claim> claims) throws STSException {
        List<Attribute> attributes = new ArrayList<Attribute>();
        for (Claim claim : claims) {
            Attribute attribute = claim.getAttribute();
            attributes.add(attribute);
        }
        return attributes;
    }
}
