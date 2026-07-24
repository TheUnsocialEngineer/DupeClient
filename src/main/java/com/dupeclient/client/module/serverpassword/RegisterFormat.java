package com.dupeclient.client.module.serverpassword;

/** Argument order for /register on auth plugins (AuthMe, etc.). */
public enum RegisterFormat {
    /** /register password */
    PASSWORD_ONLY,
    /** /register password password */
    PASSWORD_REPEAT,
    /** /register email password */
    EMAIL_THEN_PASSWORD,
    /** /register password email */
    PASSWORD_THEN_EMAIL
}
