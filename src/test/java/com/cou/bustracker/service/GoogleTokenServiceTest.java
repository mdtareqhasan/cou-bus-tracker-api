package com.cou.bustracker.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleTokenServiceTest {

    @Test
    void acceptsCouDomains() {
        assertTrue(GoogleTokenService.isAllowedInstitutionalEmail("raju@cou.ac.bd"));
        assertTrue(GoogleTokenService.isAllowedInstitutionalEmail("Raju@STUD.COU.AC.BD"));
    }

    @Test
    void rejectsOtherDomains() {
        assertFalse(GoogleTokenService.isAllowedInstitutionalEmail("raju@gmail.com"));
        assertFalse(GoogleTokenService.isAllowedInstitutionalEmail("raju@cou.ac.bd.evil.com"));
        assertFalse(GoogleTokenService.isAllowedInstitutionalEmail("raju@notcou.ac.bd"));
    }

    @Test
    void rejectsMalformedAddresses() {
        assertFalse(GoogleTokenService.isAllowedInstitutionalEmail(null));
        assertFalse(GoogleTokenService.isAllowedInstitutionalEmail("rajucou.ac.bd"));
        assertFalse(GoogleTokenService.isAllowedInstitutionalEmail("raju@"));
    }

    @Test
    void facultyEmailOnlyAcceptsCouDomain() {
        assertTrue(GoogleTokenService.isFacultyEmail("raju@cou.ac.bd"));
        assertTrue(GoogleTokenService.isFacultyEmail("Raju@COU.AC.BD"));
    }

    @Test
    void facultyEmailRejectsStudentAndOtherDomains() {
        assertFalse(GoogleTokenService.isFacultyEmail("raju@stud.cou.ac.bd"));
        assertFalse(GoogleTokenService.isFacultyEmail("raju@gmail.com"));
        assertFalse(GoogleTokenService.isFacultyEmail("raju@cou.ac.bd.evil.com"));
        assertFalse(GoogleTokenService.isFacultyEmail(null));
        assertFalse(GoogleTokenService.isFacultyEmail("rajucou.ac.bd"));
        assertFalse(GoogleTokenService.isFacultyEmail("raju@"));
    }

    @Test
    void extraAllowedEmailsAreAcceptedForBothRoles() {
        assertTrue(GoogleTokenService.isAllowedInstitutionalEmail("kawsar101amin@gmail.com"));
        assertTrue(GoogleTokenService.isAllowedInstitutionalEmail("kawsar.amin@stud.cou.ac.bd"));
        assertTrue(GoogleTokenService.isAllowedInstitutionalEmail("tareqcse@stud.cou.ac.bd"));
        // Case-insensitive: the sign-in gate lower-cases before checking.
        assertTrue(GoogleTokenService.isAllowedInstitutionalEmail("Kawsar.Amin@STUD.COU.AC.BD"));

        assertTrue(GoogleTokenService.isTeacherSignInEmail("kawsar101amin@gmail.com"));
        assertTrue(GoogleTokenService.isTeacherSignInEmail("kawsar.amin@stud.cou.ac.bd"));
        assertTrue(GoogleTokenService.isTeacherSignInEmail("tareqcse@stud.cou.ac.bd"));
        assertTrue(GoogleTokenService.isTeacherSignInEmail("teacher@cou.ac.bd"));
    }

    @Test
    void extraAllowedEmailsDoNotOpenTheFloodgates() {
        // Other addresses still follow the domain rules.
        assertFalse(GoogleTokenService.isAllowedInstitutionalEmail("raju@gmail.com"));
        assertFalse(GoogleTokenService.isAllowedInstitutionalEmail("kawsar.amin@gmail.com"));
        assertFalse(GoogleTokenService.isTeacherSignInEmail("raju@gmail.com"));
        assertFalse(GoogleTokenService.isTeacherSignInEmail("kawsar.amin@gmail.com"));
        assertFalse(GoogleTokenService.isTeacherSignInEmail("someone@stud.cou.ac.bd"));
        assertFalse(GoogleTokenService.isExtraAllowedEmail("kawsar101amin@gmail.com.evil.com"));
        assertFalse(GoogleTokenService.isExtraAllowedEmail(null));
    }
}
