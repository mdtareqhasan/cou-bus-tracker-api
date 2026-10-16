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
}
