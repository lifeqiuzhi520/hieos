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
package com.vangent.hieos.empi.persistence;

import com.vangent.hieos.empi.codes.CodesConfig;
import com.vangent.hieos.empi.config.EMPIConfig;
import com.vangent.hieos.subjectmodel.Subject;
import com.vangent.hieos.subjectmodel.Subject.SubjectType;
import com.vangent.hieos.subjectmodel.SubjectIdentifier;
import com.vangent.hieos.empi.exception.EMPIException;
import com.vangent.hieos.empi.model.SubjectCrossReference;
import com.vangent.hieos.empi.sequence.SequenceGenerator;
import com.vangent.hieos.empi.sequence.SequenceGeneratorException;
import com.vangent.hieos.subjectmodel.InternalId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author thumbe
 */
public class SubjectDAO extends AbstractDAO {

    private static final Logger logger = Logger.getLogger(SubjectDAO.class);

    /**
     * 
     * @param connection
     */
    public SubjectDAO(Connection connection) {
        super(connection);
    }

    /**
     *
     * @param subjectId
     * @return
     * @throws EMPIException
     */
    public Subject load(InternalId subjectId) throws EMPIException {
        Subject subject = new Subject();

        // Load the subject.
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT id,type,birth_time,gender_code,deceased_indicator,deceased_time,multiple_birth_indicator,multiple_birth_order_number,marital_status_code,religious_affiliation_code,race_code,ethnic_group_code,last_updated_time FROM subject WHERE id=?";
            if (logger.isTraceEnabled()) {
                logger.trace("SQL = " + sql);
            }
            stmt = this.getPreparedStatement(sql);
            stmt.setLong(1, subjectId.getId());
            // Execute query.
            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new EMPIException("No subject found for uniqueid = " + subjectId);
            } else {
                subject.setInternalId(subjectId);
                subject.setType(SubjectDAO.getSubjectType(rs.getString(2)));
                subject.setBirthTime(this.getDate(rs, 3));
                subject.setGender(this.getCodedValue(rs.getString(4), CodesConfig.CodedType.GENDER));
                subject.setDeceasedIndicator(this.getBoolean(rs, 5));
                subject.setDeceasedTime(this.getDate(rs, 6));
                subject.setMultipleBirthIndicator(this.getBoolean(rs, 7));
                subject.setMultipleBirthOrderNumber(this.getInteger(rs, 8));
                subject.setMaritalStatus(this.getCodedValue(rs.getString(9), CodesConfig.CodedType.MARITAL_STATUS));
                subject.setReligiousAffiliation(this.getCodedValue(rs.getString(10), CodesConfig.CodedType.RELIGIOUS_AFFILIATION));
                subject.setRace(this.getCodedValue(rs.getString(11), CodesConfig.CodedType.RACE));
                subject.setEthnicGroup(this.getCodedValue(rs.getString(12), CodesConfig.CodedType.ETHNIC_GROUP));
                Date lastUpdatedTime = this.getDate(rs.getTimestamp(13));
                subject.setLastUpdatedTime(lastUpdatedTime);
            }
        } catch (SQLException ex) {
            throw PersistenceHelper.getEMPIException("Exception reading Subject from database", ex);
        } finally {
            this.close(stmt);
            this.close(rs);
        }

        // Now, load composed objects.
        Connection conn = this.getConnection();

        // Names.
        SubjectNameDAO subjectNameDAO = new SubjectNameDAO(conn);
        subjectNameDAO.load(subject);

        // Addresses.
        SubjectAddressDAO subjectAddressDAO = new SubjectAddressDAO(conn);
        subjectAddressDAO.load(subject);

        // Telecom addresses.
        SubjectTelecomAddressDAO subjectTelecomAddressDAO = new SubjectTelecomAddressDAO(conn);
        subjectTelecomAddressDAO.load(subject);

        // Personal relationships.
        SubjectPersonalRelationshipDAO subjectPersonalRelationshipDAO = new SubjectPersonalRelationshipDAO(conn);
        subjectPersonalRelationshipDAO.load(subject);

        // Languages.
        SubjectLanguageDAO subjectLanguageDAO = new SubjectLanguageDAO(conn);
        subjectLanguageDAO.load(subject);

        // Citizenships.
        SubjectCitizenshipDAO subjectCitizenshipDAO = new SubjectCitizenshipDAO(conn);
        subjectCitizenshipDAO.load(subject);

        // Identifiers.
        SubjectIdentifierDAO subjectIdentifierDAO = new SubjectIdentifierDAO(conn);
        List<SubjectIdentifier> subjectIdentifiers = subjectIdentifierDAO.load(subject, SubjectIdentifier.Type.PID);
        subject.setSubjectIdentifiers(subjectIdentifiers);

        // Other identifiers.
        //SubjectOtherIdentifierDAO subjectOtherIdentifierDAO = new SubjectOtherIdentifierDAO(conn);
        List<SubjectIdentifier> subjectOtherIdentifiers = subjectIdentifierDAO.load(subject, SubjectIdentifier.Type.OTHER);
        subject.setSubjectOtherIdentifiers(subjectOtherIdentifiers);

        return subject;
    }

    /**
     *
     * @param subjectIdentifier
     * @return
     * @throws EMPIException
     */
    /*
    public List<Subject> loadBaseSubjectsByIdentifier(SubjectIdentifier subjectIdentifier) throws EMPIException {
        SubjectIdentifierDAO subjectIdentifierDAO = new SubjectIdentifierDAO(this.getConnection());

        // First the subject internal ids for the given subject identifier.
        List<InternalId> subjectIds = subjectIdentifierDAO.getSubjectIds(subjectIdentifier);
        List<Subject> subjects = new ArrayList<Subject>();
        for (InternalId subjectId : subjectIds) {
            Subject subject = this.loadBaseSubject(subjectId);
            subjects.add(subject);
        }
        return subjects;
    }*/

    /**
     * 
     * @param subjectIdentifiers
     * @return
     * @throws EMPIException
     */
    public List<Subject> loadBaseSubjectsByIdentifier(List<SubjectIdentifier> subjectIdentifiers) throws EMPIException {
        SubjectIdentifierDAO subjectIdentifierDAO = new SubjectIdentifierDAO(this.getConnection());

        // First the subject internal ids for the given subject identifier.
        List<InternalId> subjectIds = subjectIdentifierDAO.getSubjectIds(subjectIdentifiers);
        List<Subject> subjects = new ArrayList<Subject>();
        for (InternalId subjectId : subjectIds) {
            Subject subject = this.loadBaseSubject(subjectId);
            subjects.add(subject);
        }
        return subjects;
    }

    /**
     *
     * @param subjectId
     * @return
     * @throws EMPIException
     */
    public Subject loadBaseSubject(InternalId subjectId) throws EMPIException {
        Subject subject = null;

        // Load the subject.
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT id,type FROM subject WHERE id=?";
            if (logger.isTraceEnabled()) {
                logger.trace("SQL = " + sql);
            }
            stmt = this.getPreparedStatement(sql);
            stmt.setLong(1, subjectId.getId());
            // Execute query.
            rs = stmt.executeQuery();
            if (rs.next()) {
                subject = new Subject();
                subject.setInternalId(subjectId);
                subject.setType(SubjectDAO.getSubjectType(rs.getString(2)));
            }
        } catch (SQLException ex) {
            throw PersistenceHelper.getEMPIException("Exception reading Subject from database", ex);
        } finally {
            this.close(stmt);
            this.close(rs);
        }
        return subject;
    }

    /**
     *
     * @param enterpriseSubjectId
     * @return
     * @throws EMPIException
     */
    public InternalId getLastUpdatedSystemSubjectId(InternalId enterpriseSubjectId) throws EMPIException {
        InternalId lastUpdatedSystemSubjectId = null;
        Connection conn = this.getConnection();
        // Get list of cross references.
        SubjectCrossReferenceDAO subjectCrossReferenceDAO = new SubjectCrossReferenceDAO(conn);
        List<SubjectCrossReference> subjectCrossReferences =
                subjectCrossReferenceDAO.loadEnterpriseSubjectCrossReferences(enterpriseSubjectId);
        // Now, get the system subject id with the last updated time stamp.
        Date compareLastUpdatedTime = null;
        for (SubjectCrossReference subjectCrossReference : subjectCrossReferences) {
            Date lastUpdatedTime = this.getLastUpdatedTime(subjectCrossReference.getSystemSubjectId());
            if (compareLastUpdatedTime == null || lastUpdatedTime.after(compareLastUpdatedTime)) {
                compareLastUpdatedTime = lastUpdatedTime;
                lastUpdatedSystemSubjectId = subjectCrossReference.getSystemSubjectId();
            }
        }
        return lastUpdatedSystemSubjectId;
    }

    /**
     * 
     * @param subjectId
     * @return
     * @throws EMPIException
     */
    private Date getLastUpdatedTime(InternalId subjectId) throws EMPIException {
        Date lastUpdatedTime = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT last_updated_time FROM subject WHERE id=?";
            if (logger.isTraceEnabled()) {
                logger.trace("SQL = " + sql);
            }
            stmt = this.getPreparedStatement(sql);
            stmt.setLong(1, subjectId.getId());
            // Execute query.
            rs = stmt.executeQuery();
            if (rs.next()) {
                lastUpdatedTime = this.getDate(rs.getTimestamp(1));
            }
        } catch (SQLException ex) {
            throw PersistenceHelper.getEMPIException("Exception reading subject(s) from database", ex);
        } finally {
            this.close(stmt);
            this.close(rs);
        }
        return lastUpdatedTime;
    }

    /**
     * 
     * @param subjectIdentifiers
     * @return
     * @throws EMPIException
     */
    public boolean doesSubjectExist(List<SubjectIdentifier> subjectIdentifiers) throws EMPIException {
        boolean subjectExists = false;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            SubjectIdentifierDAO subjectIdentifierDAO = new SubjectIdentifierDAO(this.getConnection());
            // Go through each identifier (assumes no duplicates across subjects).
            // See if the identifier has a subject.
            List<InternalId> subjectIds = subjectIdentifierDAO.getSubjectIds(subjectIdentifiers);
            if (!subjectIds.isEmpty()) {
                subjectExists = true;
            }
        } finally {
            this.close(stmt);
            this.close(rs);
        }
        return subjectExists;
    }

    /**
     *
     * @param subjects
     * @throws EMPIException
     */
    public void insert(List<Subject> subjects) throws EMPIException {
        if (subjects.isEmpty()) {
            return; // Early exit!
        }
        PreparedStatement stmt = null;
        Connection conn = this.getConnection();
        try {
            String sql = "INSERT INTO subject(id,type,birth_time,gender_code,deceased_indicator,deceased_time,multiple_birth_indicator,multiple_birth_order_number,marital_status_code,religious_affiliation_code,race_code,ethnic_group_code,last_updated_time) values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
            stmt = this.getPreparedStatement(sql);
            for (Subject subject : subjects) {
                if (logger.isTraceEnabled()) {
                    logger.trace("SQL = " + sql);
                }
                Long subjectId = this.generateSubjectUniqueId();
                InternalId internalId = new InternalId(subjectId);
                subject.setInternalId(internalId);
                String subjectTypeValue = SubjectDAO.getSubjectTypeValue(subject.getType());
                subject.setLastUpdatedTime(new Date());  // Update timestamp.
                stmt.setLong(1, subjectId);
                stmt.setString(2, subjectTypeValue);
                this.setDate(stmt, 3, subject.getBirthTime());
                this.setCodedValue(stmt, 4, subject.getGender(), CodesConfig.CodedType.GENDER);
                this.setBoolean(stmt, 5, subject.getDeceasedIndicator());
                this.setDate(stmt, 6, subject.getDeceasedTime());
                this.setBoolean(stmt, 7, subject.getMultipleBirthIndicator());
                this.setInteger(stmt, 8, subject.getMultipleBirthOrderNumber());
                this.setCodedValue(stmt, 9, subject.getMaritalStatus(), CodesConfig.CodedType.MARITAL_STATUS);
                this.setCodedValue(stmt, 10, subject.getReligiousAffiliation(), CodesConfig.CodedType.RELIGIOUS_AFFILIATION);
                this.setCodedValue(stmt, 11, subject.getRace(), CodesConfig.CodedType.RACE);
                this.setCodedValue(stmt, 12, subject.getEthnicGroup(), CodesConfig.CodedType.ETHNIC_GROUP);
                stmt.setTimestamp(13, this.getTimestamp(subject.getLastUpdatedTime()));

                stmt.addBatch();
            }
            long startTime = System.currentTimeMillis();
            int[] insertCounts = stmt.executeBatch();
            long endTime = System.currentTimeMillis();
            if (logger.isTraceEnabled()) {
                logger.trace("SubjectDAO.insert: done executeBatch elapedTimeMillis=" + (endTime - startTime)
                        + " Number Records Added: " + insertCounts.length);
            }

            // FIXME: Anyway to batch sub components????

            // Now, insert composed objects.
            SubjectNameDAO subjectNameDAO = new SubjectNameDAO(conn);
            SubjectAddressDAO subjectAddressDAO = new SubjectAddressDAO(conn);
            SubjectTelecomAddressDAO subjectTelecomAddressDAO = new SubjectTelecomAddressDAO(conn);
            SubjectPersonalRelationshipDAO subjectPersonalRelationshipDAO = new SubjectPersonalRelationshipDAO(conn);
            SubjectLanguageDAO subjectLanguageDAO = new SubjectLanguageDAO(conn);
            SubjectCitizenshipDAO subjectCitizenshipDAO = new SubjectCitizenshipDAO(conn);
            SubjectIdentifierDAO subjectIdentifierDAO = new SubjectIdentifierDAO(conn);
            //SubjectOtherIdentifierDAO_OLD subjectOtherIdentifierDAO = new SubjectOtherIdentifierDAO_OLD(conn);
            for (Subject subject : subjects) {
                subjectNameDAO.insert(subject.getSubjectNames(), subject);
                subjectAddressDAO.insert(subject.getAddresses(), subject);
                subjectTelecomAddressDAO.insert(subject.getTelecomAddresses(), subject);
                subjectPersonalRelationshipDAO.insert(subject.getSubjectPersonalRelationships(), subject);
                subjectLanguageDAO.insert(subject.getSubjectLanguages(), subject);
                subjectCitizenshipDAO.insert(subject.getSubjectCitizenships(), subject);
                subjectIdentifierDAO.insert(subject);
                //subjectOtherIdentifierDAO.insert(subject.getSubjectOtherIdentifiers(), subject);
            }
        } catch (SequenceGeneratorException ex) {
            throw new EMPIException(ex);
        } catch (SQLException ex) {
            throw PersistenceHelper.getEMPIException("Exception inserting subjects", ex);
        } finally {
            this.close(stmt);
        }
    }

    /**
     * 
     * @return
     * @throws SequenceGeneratorException
     * @throws EMPIException
     */
    private Long generateSubjectUniqueId() throws SequenceGeneratorException, EMPIException {
        EMPIConfig empiConfig = EMPIConfig.getInstance();
        String sql = empiConfig.getSubjectSequenceGeneratorSQL();
        SequenceGenerator sg = new SequenceGenerator(this.getConnection(), sql);
        return sg.getNext();
    }

    /**
     *
     * @param targetEnterpriseSubjectId
     * @param subject Contains demographics to update.
     * @throws EMPIException 
     */
    public void updateEnterpriseSubject(InternalId targetEnterpriseSubjectId, Subject subject) throws EMPIException {
        // First, load the enterprise subject.
        Subject enterpriseSubject = this.load(targetEnterpriseSubjectId);

        // Create updated enterprise subject.
        Subject updatedEnterpriseSubject;
        try {
            updatedEnterpriseSubject = (Subject) enterpriseSubject.clone();
        } catch (CloneNotSupportedException ex) {
            throw new EMPIException("Internal exception - unable to clone subject");
        }
        updatedEnterpriseSubject.setLastUpdatedTime(new Date());

        // Overlay demographics

        // Overlay simple demographics.

        // Birth time
        if (subject.getBirthTime() != null) {
            updatedEnterpriseSubject.setBirthTime(subject.getBirthTime());
            //System.out.println("... updating birth time");
        }

        // Gender.
        if (subject.getGender() != null) {
            updatedEnterpriseSubject.setGender(subject.getGender());
            //System.out.println("... updating gender");
        }

        // Deceased indicator.
        if (subject.getDeceasedIndicator() != null) {
            updatedEnterpriseSubject.setDeceasedIndicator(subject.getDeceasedIndicator());
            //System.out.println("... updating deceased indicator");
        }

        // Deceased time.
        if (subject.getDeceasedTime() != null) {
            updatedEnterpriseSubject.setDeceasedTime(subject.getDeceasedTime());
            //System.out.println("... updating deceased time");
        }

        // Multiple birth indicator.
        if (subject.getMultipleBirthIndicator() != null) {
            updatedEnterpriseSubject.setMultipleBirthIndicator(subject.getMultipleBirthIndicator());
            //System.out.println("... updating multiple birth indicator");
        }

        // Multiple birth order.
        if (subject.getMultipleBirthOrderNumber() != null) {
            updatedEnterpriseSubject.setMultipleBirthOrderNumber(subject.getMultipleBirthOrderNumber());
            //System.out.println("... updating multiple birth order number");
        }

        // Marital status
        if (subject.getMaritalStatus() != null) {
            updatedEnterpriseSubject.setMaritalStatus(subject.getMaritalStatus());
            //System.out.println("... updating marital status");
        }

        // Religious affiliation.
        if (subject.getReligiousAffiliation() != null) {
            updatedEnterpriseSubject.setReligiousAffiliation(subject.getReligiousAffiliation());
            //System.out.println("... updating religious affiliation");
        }

        // Race.
        if (subject.getRace() != null) {
            updatedEnterpriseSubject.setRace(subject.getRace());
            //System.out.println("... updating race");
        }

        // Ethnic group.
        if (subject.getEthnicGroup() != null) {
            updatedEnterpriseSubject.setEthnicGroup(subject.getEthnicGroup());
            //System.out.println("... updating ethnic group");
        }

        // Now, deal with lists.
        // FIXME??: Right now, we do a full replace.  Would do based upon type and/or use, but
        // there can be nulls in these fields.  Need to revisit.

        // Addresses.
        if (!subject.getAddresses().isEmpty()) {
            updatedEnterpriseSubject.getAddresses().clear();
            updatedEnterpriseSubject.getAddresses().addAll(subject.getAddresses());
            //System.out.println("... updating addresses");

        }

        // Telecom addresses.
        if (!subject.getTelecomAddresses().isEmpty()) {
            updatedEnterpriseSubject.getTelecomAddresses().clear();
            updatedEnterpriseSubject.getTelecomAddresses().addAll(subject.getTelecomAddresses());
            //System.out.println("... updating telecom addresses");
        }

        // Names.
        if (!subject.getSubjectNames().isEmpty()) {
            updatedEnterpriseSubject.getSubjectNames().clear();
            updatedEnterpriseSubject.getSubjectNames().addAll(subject.getSubjectNames());
            //System.out.println("... updating names");
        }

        // Personal relationships.
        if (!subject.getSubjectPersonalRelationships().isEmpty()) {
            updatedEnterpriseSubject.getSubjectPersonalRelationships().clear();
            updatedEnterpriseSubject.getSubjectPersonalRelationships().addAll(subject.getSubjectPersonalRelationships());
            //System.out.println("... updating personal relationships");
        }

        // Languages.
        if (!subject.getSubjectLanguages().isEmpty()) {
            updatedEnterpriseSubject.getSubjectLanguages().clear();
            updatedEnterpriseSubject.getSubjectLanguages().addAll(subject.getSubjectLanguages());
            //System.out.println("... updating languages");
        }

        // Citizenships.
        if (!subject.getSubjectCitizenships().isEmpty()) {
            updatedEnterpriseSubject.getSubjectCitizenships().clear();
            updatedEnterpriseSubject.getSubjectCitizenships().addAll(subject.getSubjectCitizenships());
            //System.out.println("... updating citizenships");
        }

        // Delete subject components (names, addresses, etc.)
        this.deleteSubjectComponents(targetEnterpriseSubjectId);

        // Insert composed parts.
        // Now, insert composed objects.

        // Get DAO instances.
        Connection conn = this.getConnection();
        SubjectNameDAO subjectNameDAO = new SubjectNameDAO(conn);
        SubjectAddressDAO subjectAddressDAO = new SubjectAddressDAO(conn);
        SubjectTelecomAddressDAO subjectTelecomAddressDAO = new SubjectTelecomAddressDAO(conn);
        SubjectPersonalRelationshipDAO subjectPersonalRelationshipDAO = new SubjectPersonalRelationshipDAO(conn);
        SubjectLanguageDAO subjectLanguageDAO = new SubjectLanguageDAO(conn);
        SubjectCitizenshipDAO subjectCitizenshipDAO = new SubjectCitizenshipDAO(conn);

        // Insert list content.
        subjectNameDAO.insert(updatedEnterpriseSubject.getSubjectNames(), updatedEnterpriseSubject);
        subjectAddressDAO.insert(updatedEnterpriseSubject.getAddresses(), updatedEnterpriseSubject);
        subjectTelecomAddressDAO.insert(updatedEnterpriseSubject.getTelecomAddresses(), updatedEnterpriseSubject);
        subjectPersonalRelationshipDAO.insert(updatedEnterpriseSubject.getSubjectPersonalRelationships(), updatedEnterpriseSubject);
        subjectLanguageDAO.insert(updatedEnterpriseSubject.getSubjectLanguages(), updatedEnterpriseSubject);
        subjectCitizenshipDAO.insert(updatedEnterpriseSubject.getSubjectCitizenships(), updatedEnterpriseSubject);

        // Now update the simple demographic parts.
        this.updateSubjectSimpleParts(updatedEnterpriseSubject);

        // Note, identifiers are not touched
    }

    /**
     *
     * @param subject
     * @throws EMPIException
     */
    private void updateSubjectSimpleParts(Subject subject) throws EMPIException {
        PreparedStatement stmt = null;
        try {
            String sql = "UPDATE subject SET birth_time=?,gender_code=?,deceased_indicator=?,deceased_time=?,multiple_birth_indicator=?,multiple_birth_order_number=?,marital_status_code=?,religious_affiliation_code=?,race_code=?,ethnic_group_code=?,last_updated_time=? WHERE id=?";
            System.out.println("SQL = " + sql);
            stmt = this.getPreparedStatement(sql);
            this.setDate(stmt, 1, subject.getBirthTime());
            this.setCodedValue(stmt, 2, subject.getGender(), CodesConfig.CodedType.GENDER);
            this.setBoolean(stmt, 3, subject.getDeceasedIndicator());
            this.setDate(stmt, 4, subject.getDeceasedTime());
            this.setBoolean(stmt, 5, subject.getMultipleBirthIndicator());
            this.setInteger(stmt, 6, subject.getMultipleBirthOrderNumber());
            this.setCodedValue(stmt, 7, subject.getMaritalStatus(), CodesConfig.CodedType.MARITAL_STATUS);
            this.setCodedValue(stmt, 8, subject.getReligiousAffiliation(), CodesConfig.CodedType.RELIGIOUS_AFFILIATION);
            this.setCodedValue(stmt, 9, subject.getRace(), CodesConfig.CodedType.RACE);
            this.setCodedValue(stmt, 10, subject.getEthnicGroup(), CodesConfig.CodedType.ETHNIC_GROUP);
            stmt.setTimestamp(11, this.getTimestamp(subject.getLastUpdatedTime()));
            stmt.setLong(12, subject.getInternalId().getId());
            stmt.addBatch();
            long startTime = System.currentTimeMillis();
            stmt.executeUpdate();
            long endTime = System.currentTimeMillis();
            if (logger.isTraceEnabled()) {
                logger.trace("SubjectDAO.updateSubjectSimpleParts: done executeBatch elapedTimeMillis=" + (endTime - startTime));
            }
        } catch (SQLException ex) {
            throw PersistenceHelper.getEMPIException("Exception updated subject", ex);
        } finally {
            this.close(stmt);
        }
    }

    /**
     * 
     * @param survivingEnterpriseSubjectId
     * @param subsumedEnterpriseSubjectId
     * @throws EMPIException
     */
    public void mergeEnterpriseSubjects(InternalId survivingEnterpriseSubjectId, InternalId subsumedEnterpriseSubjectId) throws EMPIException {

        // Only perform merge if the ids are different.
        // Guard is here just in case higher-level logic does not account for this case.
        if (!survivingEnterpriseSubjectId.equals(subsumedEnterpriseSubjectId)) {
            Connection conn = this.getConnection();  // Get connection to use.

            // Delete "SubjectMatchFieldsDAO" record for subsumedEnterpriseSubjectId.
            //SubjectMatchFieldsDAO subjectMatchFieldsDAO = new SubjectMatchFieldsDAO(conn);
            //subjectMatchFieldsDAO.deleteSubjectRecords(subsumedEnterpriseSubjectId);

            // Move cross references from subsumedEnterpriseSubjectId to survivingEnterpriseSubjectId
            SubjectCrossReferenceDAO subjectCrossReferenceDAO = new SubjectCrossReferenceDAO(conn);
            subjectCrossReferenceDAO.mergeEnterpriseSubjects(survivingEnterpriseSubjectId, subsumedEnterpriseSubjectId);

            // Delete the subsumed enterprise subject.
            this.deleteSubject(subsumedEnterpriseSubjectId, Subject.SubjectType.ENTERPRISE);
        }
    }

    /**
     *
     * @param subjectId
     * @param subjectType
     * @throws EMPIException
     */
    public void deleteSubject(InternalId subjectId, Subject.SubjectType subjectType) throws EMPIException {
        PreparedStatement stmt = null;
        try {
            Connection conn = this.getConnection();  // Get connection to use.

            // First delete component parts.
            this.deleteSubjectComponents(subjectId);

            // Now delete identifiers.

            // Get DAO instances responsible for deletions.
            SubjectIdentifierDAO subjectIdentifierDAO = new SubjectIdentifierDAO(conn);
            //SubjectOtherIdentifierDAO subjectOtherIdentifierDAO = new SubjectOtherIdentifierDAO(conn);
            SubjectCrossReferenceDAO subjectCrossReferenceDAO = new SubjectCrossReferenceDAO(conn);

            // Run deletions.
            subjectIdentifierDAO.deleteSubjectRecords(subjectId);
            //subjectOtherIdentifierDAO.deleteSubjectRecords(subjectId);
            subjectCrossReferenceDAO.deleteSubjectCrossReferences(subjectId, subjectType);

            if (subjectType.equals(SubjectType.SYSTEM)) {
                // Delete subject match record.
                SubjectMatchFieldsDAO subjectMatchFieldsDAO = new SubjectMatchFieldsDAO(conn);
                subjectMatchFieldsDAO.deleteSubjectRecords(subjectId);
            }

            // Now, delete the subject record.
            this.deleteRecords(subjectId, "subject", "id", this.getClass().getName());

        } finally {
            this.close(stmt);
        }
    }

    /**
     *
     * @param subjectId
     * @throws EMPIException
     */
    private void deleteSubjectComponents(InternalId subjectId) throws EMPIException {
        PreparedStatement stmt = null;
        try {
            Connection conn = this.getConnection();  // Get connection to use.

            // First delete component parts.

            // Get DAO instances responsible for deletions.
            SubjectNameDAO subjectNameDAO = new SubjectNameDAO(conn);
            SubjectAddressDAO subjectAddressDAO = new SubjectAddressDAO(conn);
            SubjectTelecomAddressDAO subjectTelecomAddressDAO = new SubjectTelecomAddressDAO(conn);
            SubjectPersonalRelationshipDAO subjectPersonalRelationshipDAO = new SubjectPersonalRelationshipDAO(conn);
            SubjectLanguageDAO subjectLanguageDAO = new SubjectLanguageDAO(conn);
            SubjectCitizenshipDAO subjectCitizenshipDAO = new SubjectCitizenshipDAO(conn);

            // Run deletions.
            subjectNameDAO.deleteSubjectRecords(subjectId);
            subjectAddressDAO.deleteSubjectRecords(subjectId);
            subjectTelecomAddressDAO.deleteSubjectRecords(subjectId);
            subjectPersonalRelationshipDAO.deleteSubjectRecords(subjectId);
            subjectLanguageDAO.deleteSubjectRecords(subjectId);
            subjectCitizenshipDAO.deleteSubjectRecords(subjectId);

        } finally {
            this.close(stmt);
        }
    }

    /**
     *
     * @param subjectType
     * @return
     */
    private static String getSubjectTypeValue(Subject.SubjectType subjectType) {
        String value = "";
        switch (subjectType) {
            case ENTERPRISE:
                value = "E";
                break;
            case SYSTEM:
                value = "S";
                break;
            case PERSONAL_RELATIONSHIP:
                value = "P";
                break;
            default:
                value = "V";
                break;
        }
        return value;
    }

    /**
     * NOTE: Could have built a full enumeration, but decided to be overkill.
     *
     * @param type
     * @return
     */
    private static Subject.SubjectType getSubjectType(String type) {
        Subject.SubjectType subjectType = SubjectType.ENTERPRISE;
        if (type.equalsIgnoreCase("E")) {
            subjectType = SubjectType.ENTERPRISE;
        } else if (type.equalsIgnoreCase("S")) {
            subjectType = SubjectType.SYSTEM;
        } else if (type.equalsIgnoreCase("P")) {
            subjectType = SubjectType.PERSONAL_RELATIONSHIP;
        } else {
            subjectType = SubjectType.VOIDED;
        }
        return subjectType;
    }
}
