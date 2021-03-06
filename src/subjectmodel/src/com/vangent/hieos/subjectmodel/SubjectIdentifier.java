/*
 * This code is subject to the HIEOS License, Version 1.0
 *
 * Copyright(c) 2010 Vangent, Inc.  All rights reserved.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vangent.hieos.subjectmodel;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Bernie Thuman
 */
public class SubjectIdentifier extends SubjectAbstractEntity {

    public enum Type {

        PID, OTHER
    };
    private String identifier;
    private SubjectIdentifierDomain identifierDomain;
    private Type identifierType = Type.PID;  // Default.

    /**
     * 
     */
    public SubjectIdentifier() {
    }

    /**
     * 
     * @param pidCXFormatted
     */
    public SubjectIdentifier(String pidCXFormatted) {
        this.buildFromPIDCXFormatted(pidCXFormatted);
    }

    /**
     *
     * @return
     */
    public Type getIdentifierType() {
        return identifierType;
    }

    /**
     * 
     * @param identifierType
     */
    public void setIdentifierType(Type identifierType) {
        this.identifierType = identifierType;
    }

    /**
     *
     * @param pidCXFormatted
     */
    private void buildFromPIDCXFormatted(String pidCXFormatted) {
        // 5cfe5f4f31604fa^^^&1.3.6.1.4.1.21367.2005.3.7&ISO
        String parts[] = pidCXFormatted.split("\\^");
        if (parts.length > 0) {
            this.identifier = parts[0];
        }
        this.identifierDomain = new SubjectIdentifierDomain(pidCXFormatted);

        /*   String aa[] = parts[3].split("\\&");
        if (aa.length == 3) {
        this.identifierDomain = new SubjectIdentifierDomain();

        // Get namespace id
        //identifierDomain.setNamespaceId(aa[0]);

        // Get universal id (a.k.a. assigning authority).
        identifierDomain.setUniversalId(aa[1]);

        // Get universal id type (e.g. ISO).
        identifierDomain.setUniversalIdType(aa[2]);
        }*/
    }

    /**
     *
     * @return
     */
    public String getIdentifier() {
        return identifier;


    }

    /**
     *
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;


    }

    /**
     *
     * @return
     */
    public SubjectIdentifierDomain getIdentifierDomain() {
        return identifierDomain;


    }

    /**
     *
     * @param identifierDomain
     */
    public void setIdentifierDomain(SubjectIdentifierDomain identifierDomain) {
        this.identifierDomain = identifierDomain;


    }

    /**
     *
     * @return
     */
    public String getCXFormatted() {
        return identifier + "^^^&" + identifierDomain.getUniversalId() + "&" + identifierDomain.getUniversalIdType();


    }

    /**
     * 
     * @param subjectIdentifier
     * @return
     */
    public boolean equals(SubjectIdentifier subjectIdentifier) {
        return subjectIdentifier.getIdentifier().equals(this.identifier)
                && subjectIdentifier.getIdentifierDomain().equals(this.identifierDomain);


    }

    /**
     *
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        SubjectIdentifier copy = (SubjectIdentifier) super.clone();


        if (identifierDomain != null) {
            copy.identifierDomain = (SubjectIdentifierDomain) identifierDomain.clone();


        }
        return copy;


    }

    /**
     *
     * @param listToClone
     * @return
     * @throws CloneNotSupportedException
     */
    public static List<SubjectIdentifier> clone(List<SubjectIdentifier> listToClone) throws CloneNotSupportedException {
        List<SubjectIdentifier> copy = null;


        if (listToClone != null) {
            copy = new ArrayList<SubjectIdentifier>();


            for (SubjectIdentifier elementToClone : listToClone) {
                SubjectIdentifier clonedElement = (SubjectIdentifier) elementToClone.clone();
                copy.add(clonedElement);


            }
        }
        return copy;

    }
}
